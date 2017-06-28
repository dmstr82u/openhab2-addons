/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.chamberlainmyq.internal.discovery;

import static org.openhab.binding.chamberlainmyq.ChamberlainMyQBindingConstants.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQGatewayHandler;
import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQGatewayHandler.RequestCallback;
import org.openhab.binding.chamberlainmyq.internal.ChamberlainMyQHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ChamberlainMyQDeviceDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(ChamberlainMyQDeviceDiscoveryService.class);
    private ChamberlainMyQGatewayHandler hubHandler;

    public ChamberlainMyQDeviceDiscoveryService(ChamberlainMyQGatewayHandler hubHandler) throws IllegalArgumentException {
        super(ChamberlainMyQHandlerFactory.DISCOVERABLE_DEVICE_TYPES_UIDS, 10);

        this.hubHandler = hubHandler;
    }

    private ScheduledFuture<?> scanTask;

    @Override
    protected void startScan() {
        if (this.scanTask == null || this.scanTask.isDone()) {
            this.scanTask = scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        readDeviceDatabase();
                    } catch (Exception e) {
                        logger.error("Error scanning for devices", e);

                        if (scanListener != null) {
                            scanListener.onErrorOccurred(e);
                        }
                    }
                }
            }, 0, TimeUnit.SECONDS);
        }
    }

    protected void addMyQDevice(ThingTypeUID thinkType, JsonObject deviceDescription) {
        logger.debug("New Device {}", deviceDescription.toString());
        String uuid = deviceDescription.get("MyQDeviceId").toString();
        String deviceName = deviceDescription.get("MyQDeviceTypeId").toString();
        ThingUID hubUID = this.hubHandler.getThing().getUID();
        ThingUID uid = new ThingUID(thinkType, hubUID, uuid);

        Map<String, Object> properties = new HashMap<>();
        properties.put(MYQ_ID, uuid);
        //properties.put(WINK_DEVICE_CONFIG, deviceDescription.toString());

        DiscoveryResult result = DiscoveryResultBuilder.create(uid).withLabel(deviceName).withProperties(properties)
                .withBridge(hubUID).build();

        thingDiscovered(result);

        logger.debug("Discovered {}", uid);
    }

    protected void enumerateDevices(JsonObject gatewayResponse) {
        /*
        if (rootNode.has("Devices")) {
            JsonNode node = rootNode.get("Devices");
            if (node.isArray()) {
                logger.debug("Chamberlain MyQ Devices:");
                int arraysize = node.size();
                for (int i = 0; i < arraysize; i++) {
                    int deviceId = node.get(i).get("MyQDeviceId").asInt();
                    String deviceType = node.get(i).get("MyQDeviceTypeName").asText();
                    int deviceTypeId = node.get(i).get("MyQDeviceTypeId").asInt();

                    // GarageDoorOpener have MyQDeviceTypeId of 2,5,7,17
                    if (deviceTypeId == 2 || deviceTypeId == 5 || deviceTypeId == 7 || deviceTypeId == 17) {
                        devices.add(new GarageDoorDevice(deviceId, deviceType, deviceTypeId, node.get(i)));
                    } else if (deviceTypeId == 3) { // Light have MyQDeviceTypeId of 3
                        devices.add(new LampDevice(deviceId, deviceType, deviceTypeId, node.get(i)));
                    }
                }
            }
        }
        */
        JsonElement data_blob = gatewayResponse.get("Devices");
        if (data_blob == null) {
            logger.error("Empty data blob");
            return;
        }
        Iterator<JsonElement> data_blob_iter = data_blob.getAsJsonArray().iterator();
        while (data_blob_iter.hasNext()) {
            JsonElement element = data_blob_iter.next();
            if (!element.isJsonObject()) {
                continue;
            }
            if (element.getAsJsonObject().get("MyQDeviceTypeId") != null) {
                String deviceTypeId = element.getAsJsonObject().get("MyQDeviceTypeId").toString();
                if (deviceTypeId == "2" || deviceTypeId == "5" || deviceTypeId == "7" || deviceTypeId == "17") {
                    addMyQDevice(THING_TYPE_DOOR_OPENER, element.getAsJsonObject());
                } else if (deviceTypeId == "3") {
                    addMyQDevice(THING_TYPE_LIGHT, element.getAsJsonObject());
                }
            }
        }
    }

    private class listDevicesCallback implements RequestCallback {
        private ChamberlainMyQDeviceDiscoveryService discoveryService;

        public listDevicesCallback(ChamberlainMyQDeviceDiscoveryService discoveryService) {
            this.discoveryService = discoveryService;
        }

        @Override
        public void parseRequestResult(JsonObject jsonResult) {
            discoveryService.enumerateDevices(jsonResult);
        }

        @Override
        public void OnError(String error) {
            discoveryService.logger.error("Error during the device discovery: {}", error);
        }
    }

    private void readDeviceDatabase() throws IOException {
        try {
            hubHandler.sendRequestToServer(new listDevicesCallback(this));
        } catch (IOException e) {
            logger.error("Error while querying the hub for the devices.", e);
        }
    }

}
