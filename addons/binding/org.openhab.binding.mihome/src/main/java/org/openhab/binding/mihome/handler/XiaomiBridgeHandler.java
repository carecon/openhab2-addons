/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.handler;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;
import static org.openhab.binding.mihome.internal.ModelMapper.getThingTypeForModel;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.mihome.internal.EncryptionHelper;
import org.openhab.binding.mihome.internal.XiaomiItemUpdateListener;
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
 */
public class XiaomiBridgeHandler extends ConfigStatusBridgeHandler implements XiaomiSocketListener {

    private final static int ONLINE_TIMEOUT = 30000;

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);
    private final static JsonParser parser = new JsonParser();

    private Logger logger = LoggerFactory.getLogger(XiaomiBridgeHandler.class);

    private List<XiaomiItemUpdateListener> itemListeners = new CopyOnWriteArrayList<>();

    private Map<String, JsonObject> xiaomiItems = new HashMap<>();
    private String token; // token of gateway
    private long lastDiscoveryTime;
    private Map<String, Long> lastOnlineMap = new HashMap<>();

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
        // Long running initialization should be done asynchronously in background.
        XiaomiSocket.registerListener(this);
        discoverItems();

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateGatewayStatus();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.error("dispose");
        XiaomiSocket.unregisterListener(this);
        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Gateway doesn't handle command: {}", command);
    }

    @Override
    public void onDataReceived(String command, JsonObject message) {
        String sid = message.has("sid") ? message.get("sid").getAsString() : null;

        String token = message.has("token") ? message.get("token").getAsString() : null;
        if (token != null) {
            this.token = token;
        }

        if (command.equals("get_id_list_ack")) {
            JsonArray devices = parser.parse(message.get("data").getAsString()).getAsJsonArray();
            for (JsonElement deviceId : devices) {
                String device = deviceId.getAsString();
                sendCommandToBridge("read", device);
            }
            // as well get gateway status
            sendCommandToBridge("read", getGatewaySid());
        } else if (command.equals("read_ack")) {
            String model = message.get("model").getAsString();
            ThingUID thingUID = getThingUID(model, sid);
            if (thingUID != null) {
                xiaomiItems.put(sid, message);
            }
        }

        // device last seen update
        if (sid != null) {
            lastOnlineMap.put(sid, System.currentTimeMillis());

            // update state for gateway
            if (isGatewayOnline()) {
                updateStatus(ThingStatus.ONLINE);
            }
        }

        notifyListeners(command, message);
    }

    private ThingUID getThingUID(String model, String sid) {
        ThingTypeUID thingType = getThingTypeForModel(model);
        if (thingType == null) {
            logger.error("Unknown discovered model: {}", model);
            return null;
        }

        return new ThingUID(thingType, sid);
    }

    private void notifyListeners(String command, JsonObject message) {
        for (XiaomiItemUpdateListener itemListener : itemListeners) {
            try {
                String sid = message.get("sid").getAsString();
                itemListener.onItemUpdate(sid, command, message);
            } catch (Exception e) {
                logger.error("An exception occurred while calling the BridgeHeartbeatListener", e);
            }
        }
    }

    public synchronized boolean registerItemListener(XiaomiItemUpdateListener listener) {
        if (listener == null) {
            throw new NullPointerException("It's not allowed to pass a null XiaomiItemUpdateListener.");
        }
        boolean result = itemListeners.add(listener);
        if (result) {
            onUpdate();

            // inform the listener initially about all items (it will just look at own item)
            for (Map.Entry<String, JsonObject> entry : new HashSet<>(xiaomiItems.entrySet())) {
                listener.onItemUpdate(entry.getKey(), "read_ack", entry.getValue());
            }
        }
        return result;
    }

    public synchronized boolean unregisterItemListener(XiaomiItemUpdateListener listener) {
        boolean result = itemListeners.remove(listener);
        if (result) {
            onUpdate();
        }
        return result;
    }

    private void onUpdate() {
        if (isInitialized()) {
            discoverItems(); // this will as well send all items again to all listeners
        }
    }

    private void sendMessageToBridge(String message) {
        try {
            Configuration config = getThing().getConfiguration();
            String host = (String) config.get(HOST);
            int port = getConfigInteger(config, PORT);
            XiaomiSocket.sendMessage(message, InetAddress.getByName(host), port);
            logger.debug("Sent to bridge: {}", message);
        } catch (UnknownHostException e) {
            logger.error("Could not send message to bridge", e);
        }
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
        if (System.currentTimeMillis() - lastDiscoveryTime > 10000) {
            forceDiscovery();
        }
    }

    private void forceDiscovery() {
        sendCommandToBridge("get_id_list");
        lastDiscoveryTime = System.currentTimeMillis();
    }

    boolean hasItemActivity(String itemId, long withinLastMillis) {
        Long lastOnlineTimeMillis = lastOnlineMap.get(itemId);
        return lastOnlineTimeMillis != null && System.currentTimeMillis() - lastOnlineTimeMillis < withinLastMillis;
    }

    private void updateGatewayStatus() {
        updateStatus(isGatewayOnline() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
    }

    private boolean isGatewayOnline() {
        return hasItemActivity(getGatewaySid(), ONLINE_TIMEOUT);
    }
}
