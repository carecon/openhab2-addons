/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.handler;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.mihome.internal.EncryptionHelper;
import org.openhab.binding.mihome.internal.XiaomiItemUpdateListener;
import org.openhab.binding.mihome.internal.discovery.XiaomiItemDiscoveryService;
import org.openhab.binding.mihome.internal.socket.XiaomiBridgeSocket;
import org.openhab.binding.mihome.internal.socket.XiaomiSocket;
import org.openhab.binding.mihome.internal.socket.XiaomiSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link XiaomiBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels for the bridge.
 *
 * @author Patrick Boos - Initial contribution
 * @author Dimalo - added device update from heartbeat
 */
public class XiaomiBridgeHandler extends ConfigStatusBridgeHandler implements XiaomiSocketListener {

    private static final int DISCOVERY_LOCK_TIME = 10000;
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);
    private final static JsonParser parser = new JsonParser();

    private Logger logger = LoggerFactory.getLogger(XiaomiBridgeHandler.class);

    private List<XiaomiItemUpdateListener> itemListeners = new ArrayList<>();
    private List<XiaomiItemUpdateListener> itemDiscoveryListeners = new ArrayList<>();

    private String token; // token of gateway
    private long lastDiscoveryTime;
    private Map<String, Long> lastOnlineMap = new HashMap<>();

    private Configuration config;
    private InetAddress host;
    private int port;
    private XiaomiBridgeSocket socket;

    public XiaomiBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        // Currently we have no errors. Since we always use discover, it should always be okay.
        return Collections.emptyList();
    }

    @Override
    public void initialize() {
        try {
            config = getThing().getConfiguration();
            host = InetAddress.getByName(config.get(HOST).toString());
            port = getConfigInteger(config, PORT);
        } catch (UnknownHostException e) {
            logger.error("Bridge IP/PORT config is not set or not valid");
        }

        // Use existing socket for this port (if one exists)
        for (XiaomiSocket s : XiaomiSocket.getOpenSockets()) {
            if (s.getPort() == port) {
                socket = (XiaomiBridgeSocket) s;
                break;
            }
            socket = new XiaomiBridgeSocket(port);
        }
        socket.registerListener(this);

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                discoverItems();
            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        logger.error("dispose");
        socket.unregisterListener(this);
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Gateway doesn't handle command: {}", command);
    }

    @Override
    public void onDataReceived(JsonObject message) {
        logger.debug("Received message {}", message.toString());
        String sid = message.has("sid") ? message.get("sid").getAsString() : null;
        String command = message.get("cmd").getAsString();

        updateDeviceStatus(sid);
        updateStatus(ThingStatus.ONLINE);

        if (command.equals("heartbeat") && message.has("token")) {
            this.token = message.get("token").getAsString();
        } else if (command.equals("get_id_list_ack")) {
            JsonArray devices = parser.parse(message.get("data").getAsString()).getAsJsonArray();
            for (JsonElement deviceId : devices) {
                String device = deviceId.getAsString();
                sendCommandToBridge("read", device);
            }
            // as well get gateway status
            sendCommandToBridge("read", getGatewaySid());
            return;
        } else if (command.equals("read_ack")) {
            logger.debug("Device {} honored read request", sid);
        } else if (command.equals("write_ack")) {
            logger.debug("Device {} honored write request", sid);
        }
        notifyListeners(command, message);
    }

    synchronized private void notifyListeners(String command, JsonObject message) {
        boolean knownDevice = false;
        String sid = message.get("sid").getAsString();

        // Not a message to pass to any itemListener
        if (sid == null) {
            return;
        }
        for (XiaomiItemUpdateListener itemListener : itemListeners) {
            if (itemListener.getItemId().equals(sid)) {
                try {
                    itemListener.onItemUpdate(sid, command, message);
                    knownDevice = true;
                } catch (Exception e) {
                    logger.error("An exception occurred while calling the BridgeHeartbeatListener", e);
                }
            }
        }
        if (!knownDevice) {
            for (XiaomiItemUpdateListener itemListener : itemDiscoveryListeners) {
                itemListener.onItemUpdate(sid, command, message);
            }
        }
    }

    public synchronized boolean registerItemListener(XiaomiItemUpdateListener listener) {
        boolean result = false;
        if (listener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null XiaomiItemUpdateListener.");
        }
        if (listener instanceof XiaomiItemDiscoveryService) {
            result = !(itemDiscoveryListeners.contains(listener)) ? itemDiscoveryListeners.add(listener) : false;
            logger.debug("Having {} Discovery listeners", itemDiscoveryListeners.size());
        } else {
            logger.debug("Adding item listener for device {}", listener.getItemId());
            result = !(itemListeners.contains(listener)) ? itemListeners.add(listener) : false;
            logger.debug("Having {} Item listeners", itemListeners.size());
        }
        return result;
    }

    public synchronized boolean unregisterItemListener(XiaomiItemUpdateListener listener) {
        boolean result = itemListeners.remove(listener);
        if (result) {
            checkForDevices();
        }
        return result;
    }

    private void checkForDevices() {
        if (isInitialized()) {
            logger.debug("Discovering all items");
            discoverItems(); // this will as well send all items again to all listeners
        }
    }

    private void sendMessageToBridge(String message) {
        logger.debug("Send to bridge: {}", message);
        socket.sendMessage(message, host, port);
    }

    private void sendCommandToBridge(String cmd) {
        sendCommandToBridge(cmd, null, null, null);
    }

    private void sendCommandToBridge(String cmd, String[] keys, Object[] values) {
        sendCommandToBridge(cmd, null, keys, values);
    }

    private void sendCommandToBridge(String cmd, String sid) {
        sendCommandToBridge(cmd, sid, null, null);
    }

    private void sendCommandToBridge(String cmd, String sid, String[] keys, Object[] values) {
        StringBuilder message = new StringBuilder("{");
        message.append("\"cmd\": \"").append(cmd).append("\"");
        if (sid != null) {
            message.append("\"sid\": \"").append(sid).append("\"");
        }
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                message.append(",").append("\"").append(keys[i]).append("\"").append(": ");

                // write value
                message.append(toJsonValue(values[i]));
            }
        }
        message.append("}");

        sendMessageToBridge(message.toString());
    }

    void writeToDevice(String itemId, String[] keys, Object[] values) {
        sendCommandToBridge("write", new String[] { "sid", "data" },
                new Object[] { itemId, createDataJsonString(keys, values) });
    }

    void writeToBridge(String[] keys, Object[] values) {
        sendCommandToBridge("write", new String[] { "model", "sid", "short_id", "data" },
                new Object[] { "gateway", getGatewaySid(), "0", createDataJsonString(keys, values) });
    }

    private String createDataJsonString(String[] keys, Object[] values) {
        return "{" + createDataString(keys, values) + ", \\\"key\\\": \\\"" + getEncryptedKey() + "\"}";
    }

    private String getGatewaySid() {
        return (String) getConfig().get(SERIAL_NUMBER);
    }

    private String getEncryptedKey() {
        String key = (String) getConfig().get("key");

        if (key == null) {
            logger.error("No key set in the gateway settings. Edit it in the configuration.");
            return "";
        }

        return EncryptionHelper.encrypt(token, key);
    }

    private String createDataString(String[] keys, Object[] values) {
        StringBuilder builder = new StringBuilder();

        if (keys.length != values.length) {
            return "";
        }

        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                builder.append(",");
            }

            // write key
            builder.append("\\\"").append(keys[i]).append("\\\"").append(": ");

            // write value
            builder.append(escapeQuotes(toJsonValue(values[i])));
        }
        return builder.toString();
    }

    private String toJsonValue(Object o) {
        if (o instanceof String) {
            return "\"" + o + "\"";
        } else {
            return o.toString();
        }
    }

    private String escapeQuotes(String string) {
        return string.replaceAll("\"", "\\\\\"");
    }

    private int getConfigInteger(Configuration config, String key) {
        Object value = config.get(key);
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else {
            return (Integer) value;
        }
    }

    private void discoverItems() {
        if (System.currentTimeMillis() - lastDiscoveryTime > DISCOVERY_LOCK_TIME) {
            logger.debug("Triggered discovery");
            sendCommandToBridge("get_id_list");
            lastDiscoveryTime = System.currentTimeMillis();
        } else {
            logger.debug("Triggered unneccessary discovery");
        }
    }

    boolean hasItemActivity(String itemId, long withinLastMillis) {
        Long lastOnlineTimeMillis = lastOnlineMap.get(itemId);
        return lastOnlineTimeMillis != null && System.currentTimeMillis() - lastOnlineTimeMillis < withinLastMillis;
    }

    private void updateDeviceStatus(String sid) {
        if (sid != null) {
            lastOnlineMap.put(sid, System.currentTimeMillis());
            logger.debug("Updated \"last time seen\" for device {}", sid);
        }
    }
}