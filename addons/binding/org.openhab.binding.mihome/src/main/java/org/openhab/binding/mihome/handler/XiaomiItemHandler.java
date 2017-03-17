/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.handler;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.mihome.internal.ColorUtil;
import org.openhab.binding.mihome.internal.XiaomiItemUpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link XiaomiItemHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Patrick Boos - Initial contribution
 * @author Kuba Wolanin - Added voltage and low battery report
 */
public class XiaomiItemHandler extends BaseThingHandler implements XiaomiItemUpdateListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = new HashSet<>(
            Arrays.asList(THING_TYPE_GATEWAY, THING_TYPE_SENSOR_HT, THING_TYPE_SENSOR_MOTION, THING_TYPE_SENSOR_SWITCH,
                    THING_TYPE_SENSOR_MAGNET, THING_TYPE_SENSOR_PLUG, THING_TYPE_SENSOR_CUBE));

    private static final long ONLINE_TIMEOUT = 24 * 60 * 60 * 1000;

    private JsonParser parser = new JsonParser();

    private XiaomiBridgeHandler bridgeHandler;
    private String itemId;

    private Logger logger = LoggerFactory.getLogger(XiaomiItemHandler.class);

    public XiaomiItemHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        initializeThing();

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateItemStatus();
            }
        }, 1, 60, TimeUnit.SECONDS);
    }

    private void initializeThing() {
        final String configItemId = (String) getConfig().get(ITEM_ID);
        if (configItemId != null) {
            itemId = configItemId;
        }
        updateItemStatus();
    }

    @Override
    public void dispose() {
        logger.debug("Handler disposes. Unregistering listener.");
        if (itemId != null) {
            XiaomiBridgeHandler bridgeHandler = getXiaomiBridgeHandler();
            if (bridgeHandler != null) {
                bridgeHandler.unregisterItemListener(this);
                this.bridgeHandler = null;
            }
            itemId = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO somehow it seems that this is called as well with LAST KNOWN STATE when openhab gets started. Can this
        // be turned off somehow?
        switch (channelUID.getId()) {
            case CHANNEL_POWER_ON:
                String status = command.toString().toLowerCase();
                getXiaomiBridgeHandler().writeToDevice(itemId, new String[] { "status" }, new Object[] { status });
                break;
            case CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                    writeBridgeLightColor(getGatewayLightColor(), ((PercentType) command).floatValue() / 100);
                } else if (command instanceof OnOffType) {
                    writeBridgeLightColor(getGatewayLightColor(), command == OnOffType.ON ? 1 : 0);
                } else {
                    logger.error("Can't handle command " + command);
                }
                break;
            case CHANNEL_COLOR:
                if (command instanceof HSBType) {
                    writeBridgeLightColor(((HSBType) command).getRGB() & 0xffffff, getGatewayLightBrightness());
                }
                break;
            case CHANNEL_COLOR_TEMPERATURE:
                if (command instanceof PercentType) {
                    PercentType colorTemperature = (PercentType) command;
                    int kelvin = 48 * colorTemperature.intValue() + 1700;
                    int color = ColorUtil.getRGBFromK(kelvin);
                    writeBridgeLightColor(color, getGatewayLightBrightness());
                    updateState(CHANNEL_COLOR,
                            HSBType.fromRGB((color / 256 / 256) & 0xff, (color / 256) & 0xff, color & 0xff));
                } else {
                    logger.error("Can't handle command {}", command);
                }
                break;

            default:
                logger.error("Can't handle command {}", command);
                break;
        }
    }

    private int getGatewayLightColor() {
        Item item = getItemInChannel(CHANNEL_COLOR);
        if (item == null) {
            return 0xffffff;
        }

        State state = item.getState();
        if (state != null && state instanceof HSBType) {
            return ((HSBType) state).getRGB() & 0xffffff;
        }

        return 0xffffff;
    }

    private float getGatewayLightBrightness() {
        Item item = getItemInChannel(CHANNEL_BRIGHTNESS);
        if (item == null) {
            return 1f;
        }

        State state = item.getState();
        if (state == null) {
            return 1f;
        } else if (state instanceof PercentType) {
            PercentType brightness = (PercentType) state;
            return brightness.floatValue() / 100;
        } else if (state instanceof OnOffType) {
            return state == OnOffType.ON ? 1f : 0f;
        }

        return 1f;
    }

    @Override
    public void onItemUpdate(String sid, String command, JsonObject message) {
        if (itemId != null && itemId.equals(sid)) {
            updateItemStatus();
            logger.debug("Item got update: {}", message.toString());

            JsonObject data = parser.parse(message.get("data").getAsString()).getAsJsonObject();
            String model = message.get("model").getAsString();
            switch (model) {
                case "sensor_ht":
                    if (data.get("humidity") != null) {
                        float humidity = data.get("humidity").getAsFloat() / 100;
                        updateState(CHANNEL_HUMIDITY, new DecimalType(humidity));
                    }
                    if (data.get("temperature") != null) {
                        float temperature = data.get("temperature").getAsFloat() / 100;
                        updateState(CHANNEL_TEMPERATURE, new DecimalType(temperature));
                    }
                    if (data.get("voltage") != null) {
                        Integer voltage = data.get("voltage").getAsInt();
                        updateState(CHANNEL_VOLTAGE, new DecimalType(voltage));
                    }
                    break;
                case "motion":
                    boolean hasMotion = data.has("status") && data.get("status").getAsString().equals("motion");
                    updateState(CHANNEL_MOTION, hasMotion ? OnOffType.ON : OnOffType.OFF);
                    if (hasMotion) {
                        updateState(CHANNEL_LAST_MOTION, new DateTimeType());
                    }
                    break;
                case "switch":
                    if (data.has("status")) {
                        triggerChannel("button", data.get("status").getAsString().toUpperCase());
                    }
                    break;
                case "magnet":
                    if (data.has("status")) {
                        boolean isOpen = !data.get("status").getAsString().equals("close");
                        updateState(CHANNEL_IS_OPEN, isOpen ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
                    }
                    break;
                case "plug":
                    if (data.has("status")) {
                        boolean isOn = data.get("status").getAsString().equals("on");
                        updateState(CHANNEL_POWER_ON, isOn ? OnOffType.ON : OnOffType.OFF);
                    }
                    if (data.has("load_voltage")) {
                        updateState(CHANNEL_LOAD_VOLTAGE, new DecimalType(data.get("load_voltage").getAsBigDecimal()));
                    }
                    if (data.has("load_power")) {
                        updateState(CHANNEL_LOAD_POWER, new DecimalType(data.get("load_power").getAsBigDecimal()));
                    }
                    if (data.has("power_consumed")) {
                        updateState(CHANNEL_POWER_CONSUMED,
                                new DecimalType(data.get("power_consumed").getAsBigDecimal()));
                    }
                    break;
                case "gateway":
                    if (data.has("rgb")) {
                        long rgb = data.get("rgb").getAsLong();
                        updateState(CHANNEL_BRIGHTNESS,
                                new PercentType((int) (((rgb / 256 / 256 / 256) & 0xff) / 2.55)));
                        updateState(CHANNEL_COLOR, HSBType.fromRGB((int) (rgb / 256 / 256) & 0xff,
                                (int) (rgb / 256) & 0xff, (int) rgb & 0xff));
                    }
                    break;
                case "cube":
                    logger.debug("Cube data: {}", data);
                    if (data.has("status")) {
                        triggerChannel("action", data.get("status").getAsString().toUpperCase());
                    } else if (data.has("rotate")) {
                        boolean isRotateLeft = data.get("status").getAsString().startsWith("-");
                        triggerChannel("action", isRotateLeft ? "ROTATE_LEFT" : "ROTATE_RIGHT");
                    }
                    break;
            }

            if (data.get("voltage") != null) {
                Integer voltage = data.get("voltage").getAsInt();
                updateState(CHANNEL_VOLTAGE, new DecimalType(voltage));

                if (voltage < 2800) {
                    triggerChannel("batteryLevel", "LOW");
                }
            }
        }
    }

    private Item getItemInChannel(String channel) {
        Iterator<Item> iterator = linkRegistry.getLinkedItems(thing.getChannel(channel).getUID()).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    private void writeBridgeLightColor(int color, float brightness) {
        long brightnessInt = ((long) (brightness * 255)) * 256 * 256 * 256;
        writeBridgeLightColor((color & 0xffffff) | (brightnessInt & 0xff000000));
    }

    private void writeBridgeLightColor(long color) {
        getXiaomiBridgeHandler().writeToBridge(new String[] { "rgb" }, new Object[] { color });
    }

    /**
     * Play ringtone on Xiaomi Gateway
     * 0 - 8, 10 - 13, 20 - 29 -- ringtones that come with the system)
     * > 10001 -- user-defined ringtones
     *
     * @param ringtoneId
     */
    private void writeBridgeRingtone(int ringtoneId) {
        getXiaomiBridgeHandler().writeToBridge(new String[] { "mid" }, new Object[] { ringtoneId });
    }

    /**
     * Stop playing ringtone on Xiaomi Gateway
     * by setting "mid" parameter to 10000
     */
    private void stopRingtone() {
        getXiaomiBridgeHandler().writeToBridge(new String[] { "mid" }, new Object[] { 10000 });
    }

    private synchronized XiaomiBridgeHandler getXiaomiBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof XiaomiBridgeHandler) {
                this.bridgeHandler = (XiaomiBridgeHandler) handler;
                this.bridgeHandler.registerItemListener(this);
            } else {
                return null;
            }
        }
        return this.bridgeHandler;
    }

    private void updateItemStatus() {
        if (itemId != null) {
            // note: this call implicitly registers our handler as a listener on the bridge
            if (getXiaomiBridgeHandler() != null) {
                ThingStatus bridgeStatus = (getBridge() == null) ? null : getBridge().getStatus();
                if (bridgeStatus == ThingStatus.ONLINE) {
                    ThingStatus itemStatus = getThing().getStatus();
                    ThingStatus newStatus = getXiaomiBridgeHandler().hasItemActivity(itemId, ONLINE_TIMEOUT)
                            ? ThingStatus.ONLINE : ThingStatus.OFFLINE;

                    if (!newStatus.equals(itemStatus)) {
                        updateStatus(newStatus);

                        // TODO initialize properties?
                        // initializeProperties();
                    }
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                }
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }
}
