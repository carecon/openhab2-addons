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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

/**
 * @author Dimalo
 */
public class XiaomiAqaraActorSwitch2Handler extends XiaomiActorBaseHandler {

    public XiaomiAqaraActorSwitch2Handler(Thing thing) {
        super(thing);
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        String status = command.toString().toLowerCase();
        if (channelUID.getId().equals(CHANNEL_AQARA_CH0)) {
            getXiaomiBridgeHandler().writeToDevice(itemId, new String[] { "channel_0" }, new Object[] { status });
        } else if (channelUID.getId().equals(CHANNEL_AQARA_CH1)) {
            getXiaomiBridgeHandler().writeToDevice(itemId, new String[] { "channel_1" }, new Object[] { status });
        } else {
            logger.error("Can't handle command {} on channel {}", command, channelUID);
        }
    }

    @Override
    void parseReport(JsonObject data) {
        if (data.has("channel_0")) {
            boolean isOn = data.get("channel_0").getAsString().toLowerCase().equals("on");
            updateState(CHANNEL_AQARA_CH0, isOn ? OnOffType.ON : OnOffType.OFF);
        } else if (data.has("channel_1")) {
            boolean isOn = data.get("channel_1").getAsString().toLowerCase().equals("on");
            updateState(CHANNEL_AQARA_CH1, isOn ? OnOffType.ON : OnOffType.OFF);
        }
    }

    @Override
    void parseHeartbeat(JsonObject data) {
        parseReport(data);
    }

    @Override
    void parseReadAck(JsonObject data) {
        parseReport(data);
    }

    @Override
    void parseWriteAck(JsonObject data) {
        /*
         * As of 2017/04/22 and Firmware 1.4.1.145, write_ack does not get us any valuable informations.
         * When writing a command to the switch, it reports the actual state in "write_ack" and then
         * reports the changed value in "report" - as the state gets tracked by parsing reports and heartbeats,
         * write_ack can be ignored
         * otherwise put in:
         * parseReport(data);
         */
        return;
    }
}
