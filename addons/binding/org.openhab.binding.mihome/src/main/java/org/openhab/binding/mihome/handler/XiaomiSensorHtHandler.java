package org.openhab.binding.mihome.handler;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Thing;

import com.google.gson.JsonObject;

public class XiaomiSensorHtHandler extends XiaomiSensorBaseHandler {

    public XiaomiSensorHtHandler(Thing thing) {
        super(thing);
    }

    @Override
    void parseReport(JsonObject data) {
        if (data.get("humidity") != null) {
            float humidity = data.get("humidity").getAsFloat() / 100;
            updateState(CHANNEL_HUMIDITY, new DecimalType(humidity));
        }
        if (data.get("temperature") != null) {
            float temperature = data.get("temperature").getAsFloat() / 100;
            updateState(CHANNEL_TEMPERATURE, new DecimalType(temperature));
        }
        if (data.get("voltage") != null) {
            Integer voltage = data.get("voltage").getAsInt();
            updateState(CHANNEL_VOLTAGE, new DecimalType(voltage));
        }
    }
}
