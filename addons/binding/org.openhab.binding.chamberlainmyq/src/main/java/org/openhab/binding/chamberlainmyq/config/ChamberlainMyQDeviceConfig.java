/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.chamberlainmyq.config;

import com.google.gson.JsonObject;

/**
 * The {@link ChamberlainMyQDeviceConfig} class represents the common configuration
 * parameters of a MyQ Device
 *
 * @author Scott Hanson - Initial contribution
 *
 */
public class ChamberlainMyQDeviceConfig {
    private String deviceId;
    private String deviceType;
    private String name;
    private String modelName;

    public ChamberlainMyQDeviceConfig(String deviceID) {
        this.deviceId = deviceID;
    }

    public void readConfigFromJson(JsonObject jsonConfig) {
       /* this.name = jsonConfig.get("name").toString().replaceAll("\"", "");
        this.modelName = jsonConfig.get("model_name").toString().replaceAll("\"", "");
        this.deviceManufacturer = jsonConfig.get("device_manufacturer").toString().replaceAll("\"", "");
        JsonObject subscriptionBlob = jsonConfig.get("subscription").getAsJsonObject();
        if (subscriptionBlob != null) {
            JsonObject pubnubBlob = subscriptionBlob.get("pubnub").getAsJsonObject();
            if (pubnubBlob != null) {
                this.pubnubSubscribeKey = pubnubBlob.get("subscribe_key").toString().replaceAll("\"", "");
                this.pubnubChannel = pubnubBlob.get("channel").toString().replaceAll("\"", "");
            }
        }*/
    }

    public String asString() {
      // TODO: Add more data?
      return ("Name:        " + name + "\n"+
              "Device Type: " + deviceType + "\n"+
              "Model name:  " + modelName);
    }

    public boolean validateConfig() {
        if (this.deviceId == null || this.name == null || this.deviceType == null) {
            return false;
        }
        return true;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }

    public String getModelName() {
        return modelName;
    }

    public String getDeviceType() {
        return deviceType;
    }
}
