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

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

/**
 * @author Patrick Boos - Initial contribution
 * @author Dimalo
 */
public class XiaomiSensorMagnetHandler extends XiaomiSensorBaseHandlerWithTimer {

    private static final int DEFAULT_TIMER = 300;
    private static final int MIN_TIMER = 30;

    public XiaomiSensorMagnetHandler(Thing thing) {
        super(thing, DEFAULT_TIMER, MIN_TIMER, CHANNEL_OPEN_ALARM_TIMER);
    }

    @Override
    void parseReport(JsonObject data) {
        if (data.has("status")) {
            boolean isOpen = data.get("status").getAsString().equals("open");
            updateState(CHANNEL_IS_OPEN, isOpen ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
            synchronized (this) {
                if (isOpen) {
                    updateState(CHANNEL_LAST_OPENED, new DateTimeType());
                    startTimer();
                } else {
                    cancelRunningTimer();
                }
            }
        }
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_OPEN_ALARM_TIMER)) {
            if (command != null && command instanceof DecimalType) {
                setTimerFromDecimalType((DecimalType) command);
            } else {
                logger.error("Cannot execute command {} on channel {}", channelUID, command);
            }
        } else {
            logger.error("Channel {} does not exist", channelUID);
        }
    }

    @Override
    void onTimer() {
        triggerChannel(CHANNEL_OPEN_ALARM, "ALARM");
    }

}
