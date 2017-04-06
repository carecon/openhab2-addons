package org.openhab.binding.mihome.handler;

import org.eclipse.smarthome.core.thing.Thing;

import com.google.gson.JsonObject;

public class XiaomiSensorSwitchHandler extends XiaomiSensorBaseHandler {

    public XiaomiSensorSwitchHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        if (data.has("status")) {
            triggerChannel("button", data.get("status").getAsString().toUpperCase());
        }
    }
}
