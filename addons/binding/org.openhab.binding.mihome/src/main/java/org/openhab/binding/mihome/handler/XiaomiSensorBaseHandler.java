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
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * @author Dimalo
 */
public abstract class XiaomiSensorBaseHandler extends XiaomiDeviceBaseHandler {

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public XiaomiSensorBaseHandler(Thing thing) {
        super(thing);
    }

    /**
     * @param sid
     * @param command
     * @param data
     */
    @Override
    void parseCommand(String command, JsonObject data) {
        if (command.equals("report")) {
            parseReport(data);
        } else if (command.equals("heartbeat") || command.equals("read_ack")) {
            parseHeartbeat(data);
        } else {
            logger.debug("Device {} got unknown command {}", itemId, command);
        }
    }

    /**
     * @param data
     */
    @Override
    void parseHeartbeat(JsonObject data) {
        if (data.get("voltage") != null) {
            Integer voltage = data.get("voltage").getAsInt();
            updateState(CHANNEL_VOLTAGE, new DecimalType(voltage));
            if (voltage < 2800) {
                triggerChannel(CHANNEL_BATTERY_LOW, "LOW");
            }
        }
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        return;
    }
}
