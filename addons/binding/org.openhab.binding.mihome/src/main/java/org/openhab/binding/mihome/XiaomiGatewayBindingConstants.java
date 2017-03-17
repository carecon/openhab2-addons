/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link XiaomiGatewayBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Patrick Boos - Initial contribution
 */
public class XiaomiGatewayBindingConstants {

    public static final String BINDING_ID = "mihome";

    public final static ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");
    public final static ThingTypeUID THING_TYPE_GATEWAY = new ThingTypeUID(BINDING_ID, "gateway");
    public final static ThingTypeUID THING_TYPE_SENSOR_HT = new ThingTypeUID(BINDING_ID, "sensor_ht");
    public final static ThingTypeUID THING_TYPE_SENSOR_MOTION = new ThingTypeUID(BINDING_ID, "sensor_motion");
    public final static ThingTypeUID THING_TYPE_SENSOR_SWITCH = new ThingTypeUID(BINDING_ID, "sensor_switch");
    public final static ThingTypeUID THING_TYPE_SENSOR_MAGNET = new ThingTypeUID(BINDING_ID, "sensor_magnet");
    public final static ThingTypeUID THING_TYPE_SENSOR_PLUG = new ThingTypeUID(BINDING_ID, "sensor_plug");
    public final static ThingTypeUID THING_TYPE_SENSOR_CUBE = new ThingTypeUID(BINDING_ID, "sensor_cube");

    // List of all Channel ids
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_VOLTAGE = "voltage";
    public static final String CHANNEL_HUMIDITY = "humidity";
    public static final String CHANNEL_MOTION = "motion";
    public static final String CHANNEL_LAST_MOTION = "lastMotion";
    public static final String CHANNEL_IS_OPEN = "isOpen";
    public static final String CHANNEL_POWER_ON = "powerOn";
    public static final String CHANNEL_LOAD_VOLTAGE = "loadVoltage";
    public static final String CHANNEL_LOAD_POWER = "loadPower";
    public static final String CHANNEL_POWER_CONSUMED = "powerConsumed";
    // gateway light
    public static final String CHANNEL_BRIGHTNESS = "brightness";
    public static final String CHANNEL_COLOR = "color";
    public static final String CHANNEL_COLOR_TEMPERATURE = "colorTemperature";

    // Bridge config properties
    public static final String SERIAL_NUMBER = "serialNumber";
    public static final String HOST = "ipAddress";
    public static final String PORT = "port";
    public static final String TOKEN = "token";
    public static final String POLLING_INTERVAL = "pollingInterval";

    // Item config properties
    public static final String ITEM_ID = "itemId";

}
