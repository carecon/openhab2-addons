package org.openhab.binding.mihome.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;

public abstract class XiaomiSensorBaseHandler extends XiaomiDeviceBaseHandler {

    public XiaomiSensorBaseHandler(Thing thing) {
        super(thing);
    }

    @Override
    void execute(ChannelUID channelUID, Command command) {
        return;
    }
}
