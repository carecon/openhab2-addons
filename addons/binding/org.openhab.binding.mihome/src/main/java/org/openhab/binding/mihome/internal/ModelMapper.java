/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * Maps the model (provided from Xiaomi) to thing.
 *
 * @author Patrick Boos - Initial contribution
 * @author Kuba Wolanin - Renamed labels
 * @author Dimalo
 */
public class ModelMapper {

    public static ThingTypeUID getThingTypeForModel(String model) {
        switch (model) {
            case "gateway":
                return THING_TYPE_GATEWAY;
            case "sensor_ht":
                return THING_TYPE_SENSOR_HT;
            case "motion":
                return THING_TYPE_SENSOR_MOTION;
            case "switch":
                return THING_TYPE_SENSOR_SWITCH;
            case "magnet":
                return THING_TYPE_SENSOR_MAGNET;
            case "cube":
                return THING_TYPE_SENSOR_CUBE;
            case "86sw1":
                return THING_TYPE_SENSOR_AQARA1;
            case "86sw2":
                return THING_TYPE_SENSOR_AQARA2;
            case "ctrl_neutral1":
                return THING_TYPE_ACTOR_AQARA1;
            case "ctrl_neutral2":
                return THING_TYPE_ACTOR_AQARA2;
            case "plug":
                return THING_TYPE_ACTOR_PLUG;
        }
        return null;
    }

    public static String getLabelForModel(String model) {
        switch (model) {
            case "gateway":
                return "Xiaomi Mi Smart Home Gateway";
            case "sensor_ht":
                return "Xiaomi Mi Temperature & Humidity Sensor";
            case "motion":
                return "Xiaomi Mi Motion Sensor";
            case "magnet":
                return "Xiaomi Door/Window Sensor";
            case "switch":
                return "Xiaomi Mi Wireless Switch";
            case "plug":
                return "Xiaomi Mi Smart Socket Plug";
            case "cube":
                return "Xiaomi Mi Smart Cube";
            case "86sw1":
                return "Xiaomi Aqara Smart Switch 1 Button";
            case "86sw2":
                return "Xiaomi Aqara Smart Switch 2 Button";
            case "ctrl_neutral1":
                return "Xiaomi Aqara Wall Switch 1 Button";
            case "ctrl_neutral2":
                return "Xiaomi Aqara Wall Switch 2 Button";
        }
        return null;
    }
}
