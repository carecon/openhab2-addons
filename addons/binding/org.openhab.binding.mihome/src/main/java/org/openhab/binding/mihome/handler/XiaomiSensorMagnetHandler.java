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

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
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
public class XiaomiSensorMagnetHandler extends XiaomiSensorBaseHandler {

    private static final int DEFAULT_TIMER = 300;
    private Integer triggerAfter = DEFAULT_TIMER;
    private boolean taskIsActive;

    private class TimerAction extends TimerTask {

        @Override
        public synchronized void run() {
            triggerChannel(CHANNEL_OPEN_ALARM, "ALARM");
            taskIsActive = false;
        }
    };

    private Timer trigger = new Timer();

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public XiaomiSensorMagnetHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        if (data.has("status")) {
            boolean isOpen = data.get("status").getAsString().equals("open");
            updateState(CHANNEL_IS_OPEN, isOpen ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
            synchronized (this) {
                if (isOpen) {
                    cancelRunningTimer();
                    updateState(CHANNEL_LAST_OPENED, new DateTimeType());
                    logger.debug("Set alarm timer to {} sec", triggerAfter);
                    trigger.schedule(new TimerAction(), triggerAfter * 1000);
                    taskIsActive = true;
                } else {
                    cancelRunningTimer();
                }
            }
        }
    }

    /**
     *
     */
    private void cancelRunningTimer() {
        if (taskIsActive) {
            logger.debug("Cancel running alarm timer");
            trigger.cancel();
            trigger = new Timer();
            taskIsActive = false;
        }
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_OPEN_ALARM_TIMER)) {
            if (command != null && command instanceof DecimalType) {
                try {
                    int cmd = DecimalType.valueOf(command.toString()).intValue();
                    triggerAfter = cmd < 30 ? 30 : cmd;
                    if (triggerAfter == 30) {
                        updateState(CHANNEL_OPEN_ALARM_TIMER, new DecimalType(triggerAfter));
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Cannot parse the command {} to an Integer", command.toString());
                    triggerAfter = DEFAULT_TIMER;
                }
            }
        } else {
            return;
        }
    }
}
