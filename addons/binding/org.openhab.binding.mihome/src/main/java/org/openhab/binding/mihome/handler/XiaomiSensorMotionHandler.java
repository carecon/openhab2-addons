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
public class XiaomiSensorMotionHandler extends XiaomiSensorBaseHandler {

    private static final int DEFAULT_OFF_TIMER = 120;
    private Integer noMotionAfter = DEFAULT_OFF_TIMER;
    private boolean taskIsActive;

    private class TimerAction extends TimerTask {
        @Override
        public synchronized void run() {
            updateState(CHANNEL_MOTION, OnOffType.OFF);
            taskIsActive = false;
        }
    };

    private Timer trigger = new Timer();

    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public XiaomiSensorMotionHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        boolean hasMotion = data.has("status") && data.get("status").getAsString().equals("motion");
        synchronized (this) {
            if (hasMotion) {
                updateState(CHANNEL_MOTION, OnOffType.ON);
                cancelRunningTimer();
                updateState(CHANNEL_LAST_MOTION, new DateTimeType());
                logger.debug("Set off timer to {} sec", noMotionAfter);
                trigger.schedule(new TimerAction(), noMotionAfter * 1000);
                taskIsActive = true;
            }
        }
    }

    /**
    *
    */
    private void cancelRunningTimer() {
        if (taskIsActive) {
            logger.debug("Cancel running timer");
            trigger.cancel();
            trigger = new Timer();
            taskIsActive = false;
        }
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_MOTION_OFF_TIMER)) {
            if (command != null && command instanceof DecimalType) {
                try {
                    int cmd = DecimalType.valueOf(command.toString()).intValue();
                    noMotionAfter = cmd < 5 ? 5 : cmd;
                    if (noMotionAfter == 5) {
                        updateState(CHANNEL_MOTION_OFF_TIMER, new DecimalType(noMotionAfter));
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Cannot parse the command {} to an Integer", command.toString());
                    noMotionAfter = DEFAULT_OFF_TIMER;
                }
            }
        } else {
            return;
        }
    }
}
