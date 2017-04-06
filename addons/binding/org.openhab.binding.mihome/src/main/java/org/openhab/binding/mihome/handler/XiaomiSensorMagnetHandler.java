package org.openhab.binding.mihome.handler;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;

import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.Thing;

import com.google.gson.JsonObject;

public class XiaomiSensorMagnetHandler extends XiaomiSensorBaseHandler {

    public XiaomiSensorMagnetHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        if (data.has("status")) {
            boolean isOpen = !data.get("status").getAsString().equals("close");
            updateState(CHANNEL_IS_OPEN, isOpen ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
        }
        if (data.has("no_close")) {
            Integer alarmAfter = Integer.parseInt(getConfig().get(ALARM).toString());
            if (data.get("no_close").getAsInt() == alarmAfter) {
                triggerChannel(CHANNEL_OPEN_ALARM, "ALARM");
            }
        }
    }
}
