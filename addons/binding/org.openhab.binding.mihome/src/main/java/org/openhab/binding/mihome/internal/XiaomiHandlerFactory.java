/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal;

import com.google.common.collect.Sets;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.mihome.handler.XiaomiBridgeHandler;
import org.openhab.binding.mihome.handler.XiaomiItemHandler;
import org.openhab.binding.mihome.internal.discovery.XiaomiItemDiscoveryService;
import org.osgi.framework.ServiceRegistration;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.ITEM_ID;
import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.SERIAL_NUMBER;

/**
 * The {@link XiaomiHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Patrick Boos - Initial contribution
 */
public class XiaomiHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.union(XiaomiBridgeHandler.SUPPORTED_THING_TYPES, XiaomiItemHandler.SUPPORTED_THING_TYPES);

    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID, ThingUID bridgeUID) {
        if (XiaomiBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID hueBridgeUID = getBridgeThingUID(thingTypeUID, thingUID, configuration);
            return super.createThing(thingTypeUID, configuration, hueBridgeUID, null);
        }
        if (XiaomiItemHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            ThingUID itemUID = getItemUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, itemUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the hue binding.");
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
            String serialNumber = (String) configuration.get(SERIAL_NUMBER);
            thingUID = new ThingUID(thingTypeUID, serialNumber);
        }
        return thingUID;
    }

    private ThingUID getItemUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
                                ThingUID bridgeUID) {
        if (thingUID == null) {
            String itemId = (String) configuration.get(ITEM_ID);
            thingUID = new ThingUID(thingTypeUID, itemId, bridgeUID.getId());
        }
        return thingUID;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (XiaomiBridgeHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            XiaomiBridgeHandler handler = new XiaomiBridgeHandler((Bridge) thing);
            registerItemDiscoveryService(handler);
            return handler;
        } else if (XiaomiItemHandler.SUPPORTED_THING_TYPES.contains(thingTypeUID)) {
            return new XiaomiItemHandler(thing);
        }

        return null;
    }

    @Override
    protected synchronized void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof XiaomiBridgeHandler) {
            ServiceRegistration<?> serviceReg = this.discoveryServiceRegs.get(thingHandler.getThing().getUID());
            if (serviceReg != null) {
                // remove discovery service, if bridge handler is removed
                XiaomiItemDiscoveryService service = (XiaomiItemDiscoveryService) bundleContext.getService(serviceReg.getReference());
                service.onHandlerRemoved();
                service.deactivate();
                serviceReg.unregister();
                discoveryServiceRegs.remove(thingHandler.getThing().getUID());
            }
        }
    }

    private synchronized void registerItemDiscoveryService(XiaomiBridgeHandler bridgeHandler) {
        XiaomiItemDiscoveryService discoveryService = new XiaomiItemDiscoveryService(bridgeHandler);
//        discoveryService.activate();
        discoveryService.activate();
        this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext.registerService(
                DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }
}

