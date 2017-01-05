/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal.discovery;

import static org.openhab.binding.mihome.XiaomiGatewayBindingConstants.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.mihome.internal.socket.XiaomiSocket;
import org.openhab.binding.mihome.internal.socket.XiaomiSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

/**
 * Discovery service for the Xiaomi bridge.
 *
 * @author Patrick Boos - Initial contribution
 * @author Kuba Wolanin - logger fixes
 */
public class XiaomiBridgeDiscoveryService extends AbstractDiscoveryService implements XiaomiSocketListener {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_BRIDGE);
    private static final int DISCOVERY_TIMEOUT = 10;

    private Logger logger = LoggerFactory.getLogger(XiaomiBridgeDiscoveryService.class);

    public XiaomiBridgeDiscoveryService() {
        super(SUPPORTED_THING_TYPES, DISCOVERY_TIMEOUT, false);
    }

    @Override
    protected void startScan() {
        logger.debug("Start scan");
        XiaomiSocket.registerListener(this);

        discoverGateways();
        waitUntilEnded();

        XiaomiSocket.unregisterListener(this);
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    private void waitUntilEnded() {
        final Semaphore discoveryEndedLock = new Semaphore(0);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                discoveryEndedLock.release();
            }
        }, DISCOVERY_TIMEOUT, TimeUnit.SECONDS);
        try {
            discoveryEndedLock.acquire();
        } catch (InterruptedException e) {
            logger.error("Discovery problem", e);
        }
    }

    @Override
    public void onDataReceived(String command, JsonObject data) {
        if (command.equals("iam")) {
            getGatewayInfo(data);
        }
    }

    private void discoverGateways() {
        XiaomiSocket.sendMessage("{\"cmd\": \"whois\"}");
    }

    private void getGatewayInfo(JsonObject jobject) {
        Map<String, Object> properties = new HashMap<>(4);
        String serialNumber = jobject.get("sid").getAsString();
        String ipAddress = jobject.get("ip").getAsString();
        int port = jobject.get("port").getAsInt();

        properties.put(SERIAL_NUMBER, serialNumber);
        properties.put(HOST, ipAddress);
        properties.put(PORT, port);

        logger.debug("Discovered Xiaomi Gateway - sid: {} ip: {} port: {}", serialNumber, ipAddress, port);

        ThingUID thingUID = new ThingUID(THING_TYPE_BRIDGE, serialNumber);
        thingDiscovered(
                DiscoveryResultBuilder.create(thingUID).withThingType(THING_TYPE_BRIDGE).withProperties(properties)
                        .withLabel("Xiaomi Gateway").withRepresentationProperty(SERIAL_NUMBER).build());

        // DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
        // .withProperties(properties).withBridge(bridgeUID).withLabel(light.getName()).build();
        // thingDiscovered(discoveryResult);
        // return new ThingUID(thingTypeUID, bridgeUID, light.getId());
    }
}
