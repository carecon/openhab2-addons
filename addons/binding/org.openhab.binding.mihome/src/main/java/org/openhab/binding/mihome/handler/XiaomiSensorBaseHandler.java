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

import com.google.gson.JsonObject;

/**
 * @author Dimalo
 */
public abstract class XiaomiSensorBaseHandler extends XiaomiDeviceBaseHandler {

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
            if (data.get("voltage") != null) {
                Integer voltage = data.get("voltage").getAsInt();
                updateState(CHANNEL_VOLTAGE, new DecimalType(voltage));
                if (voltage < 2800) {
                    triggerChannel(CHANNEL_BATTERY_LOW, "LOW");
                }
            }
        } else {
            logger.debug("Device {} got unknown command {}", itemId, command);
        }
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        return;
    }
}
