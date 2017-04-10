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

    private DateTimeType lastMotion;

    private class TimerAction extends TimerTask {
        @Override
        public void run() {
            updateState(CHANNEL_MOTION, OnOffType.OFF);
        }
    };

    private Timer trigger = new Timer();

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    public XiaomiSensorMotionHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        boolean hasMotion = data.has("status") && data.get("status").getAsString().equals("motion");
        DateTimeType now = new DateTimeType();
        if (hasMotion) {
            updateState(CHANNEL_MOTION, OnOffType.ON);
            int noMotionAfterMillis = noMotionAfter * 1000;
            long nowMillis = now.getCalendar().getTimeInMillis();
            long lastMotionMillis = (lastMotion != null) ? lastMotion.getCalendar().getTimeInMillis()
                    : nowMillis - noMotionAfterMillis - 1;
            if (nowMillis < lastMotionMillis + noMotionAfterMillis) {
                logger.debug("Cancel running off timer and reschedule");
                trigger.cancel();
                trigger = new Timer();
            }
            updateState(CHANNEL_LAST_MOTION, now);
            lastMotion = now;
            logger.debug("Set off timer to {} sec", noMotionAfter);
            trigger.schedule(new TimerAction(), noMotionAfterMillis);
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
