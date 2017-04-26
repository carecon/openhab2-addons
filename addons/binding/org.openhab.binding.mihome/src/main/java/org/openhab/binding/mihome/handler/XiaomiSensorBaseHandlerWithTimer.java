/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.handler;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.State;

/**
 * The {@link XiaomiSensorBaseHandlerWithTimer} is an abstract class for sensor devices
 * which use a timer to update a certain channel. The user can configure the timer via an item
 * value.
 *
 * @author Dimalo
 *
 */
public abstract class XiaomiSensorBaseHandlerWithTimer extends XiaomiSensorBaseHandler {

    private int defaultTimer;
    private int minTimer;
    private Integer timerSetpoint;
    private final String SETPOINT_CHANNEL;
    boolean timerIsRunning;
    private Timer trigger = new Timer();

    public XiaomiSensorBaseHandlerWithTimer(Thing thing, int defaultTimer, int minTimer, String setpointChannel) {
        super(thing);
        this.defaultTimer = defaultTimer;
        this.minTimer = minTimer;
        this.timerSetpoint = defaultTimer;
        this.SETPOINT_CHANNEL = setpointChannel;
    }

    class TimerAction extends TimerTask {
        @Override
        public synchronized void run() {
            onTimer();
            timerIsRunning = false;
        }

        /**
         *
         */

    };

    /**
     *
     */
    synchronized void startTimer() {
        setTimerFromItemInSetpointChannel();
        cancelRunningTimer();
        logger.debug("Set timer to {} sec", timerSetpoint);
        trigger.schedule(new TimerAction(), timerSetpoint * 1000);
        timerIsRunning = true;
    }

    /**
    *
    */
    synchronized void cancelRunningTimer() {
        if (timerIsRunning) {
            trigger.cancel();
            logger.debug("Cancelled running timer");
            trigger = new Timer();
            timerIsRunning = false;
        }
    }

    abstract void onTimer();

    /**
     * @param value
     */
    void setTimerFromDecimalType(DecimalType value) {
        try {
            int newValue = value.intValue();
            timerSetpoint = newValue < minTimer ? minTimer : newValue;
            if (timerSetpoint == minTimer) {
                updateState(SETPOINT_CHANNEL, new DecimalType(timerSetpoint));
            }
        } catch (NumberFormatException e) {
            logger.debug("Cannot parse the value {} to an Integer", value.toString());
            timerSetpoint = defaultTimer;
        }
    }

    /**
     *
     */
    void setTimerFromItemInSetpointChannel() {
        ChannelUID uid = this.thing.getChannel(SETPOINT_CHANNEL).getUID();
        Set<Item> items = linkRegistry.getLinkedItems(uid);
        if (items.size() == 1) {
            State state = ((Item) items.toArray()[0]).getState();
            if (state instanceof DecimalType) {
                logger.debug("Trying to set timer setpoint to {}", state);
                setTimerFromDecimalType((DecimalType) state);
            }
        } else {
            logger.error("Cannot find item for timer value, using already set value {}", timerSetpoint);
        }
    }

}
