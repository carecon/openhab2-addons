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

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Patrick Boos - Initial contribution
 * @author Dimalo
 */
public class XiaomiActorPlugHandler extends XiaomiActorBaseHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public XiaomiActorPlugHandler(Thing thing) {
        super(thing);
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_POWER_ON)) {
            String status = command.toString().toLowerCase();
            getXiaomiBridgeHandler().writeToDevice(itemId, new String[] { "status" }, new Object[] { status });
        } else {
            logger.error("Can't handle command {} on channel {}", command, channelUID);
        }
    }

    // TODO: wait for user feedbacks and logs to parse report, read_ack, write_ack correctly

    @Override
    void parseHeartbeat(JsonObject data) {
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
            updateState(CHANNEL_POWER_CONSUMED, new DecimalType(data.get("power_consumed").getAsBigDecimal()));
        }
    }
}
