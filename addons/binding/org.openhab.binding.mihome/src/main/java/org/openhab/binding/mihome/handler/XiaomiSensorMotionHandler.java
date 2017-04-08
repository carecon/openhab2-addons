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
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;

import com.google.gson.JsonObject;

public class XiaomiSensorMotionHandler extends XiaomiSensorBaseHandler {

    public XiaomiSensorMotionHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        boolean hasMotion = data.has("status") && data.get("status").getAsString().equals("motion");
        if (hasMotion) {
            updateState(CHANNEL_MOTION, OnOffType.ON);
            updateState(CHANNEL_LAST_MOTION, new DateTimeType());
        }
        if (data.has("no_motion")) {
            Integer noMotionAfter = Integer.parseInt(getConfig().get(NO_MOTION).toString());
            if (data.get("no_motion").getAsInt() == noMotionAfter) {
                updateState(CHANNEL_MOTION, OnOffType.OFF);
            }
        }
    }
}
