package org.openhab.binding.mihome.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

import com.google.gson.JsonObject;

public class XiaomiActorAqaraHandler extends XiaomiDeviceBaseHandler {

    public XiaomiActorAqaraHandler(Thing thing) {
        super(thing);
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
    }

    @Override
    protected void parseReport(JsonObject data) {
    }
}
