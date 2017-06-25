/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.chamberlainmyq.handler;

import static org.openhab.binding.chamberlainmyq.ChamberlainMyQBindingConstants.*;

import java.text.DecimalFormat;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * TODO: The {@link ChamberlainMyQLightHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Hanson - Initial contribution
 */
public class ChamberlainMyQLightHandler extends ChamberlainMyQHandler {
    public ChamberlainMyQLightHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        if (!this.deviceConfig.validateConfig()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid config.");
            return;
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    protected String getDeviceRequestPath() {
        return "light_bulbs/" + this.deviceConfig.getDeviceId();
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        if (channelUID.getId().equals(CHANNEL_LIGHT_STATE)) {
            ReadDeviceState();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(CHANNEL_LIGHT_STATE)) {
            if (command.equals(OnOffType.ON)) {
                setLightState(true);
            } else if (command.equals(OnOffType.OFF)) {
                setLightState(false);
            } else if (command instanceof RefreshType) {
                //logger.debug("Refreshing state");
                ReadDeviceState();
            }
        }
    }

    private void setLightState(boolean state) {
        if (state) {
            sendCommand("{\"desired_state\":{\"powered\": true}}");
        } else {
            sendCommand("{\"desired_state\": {\"powered\": false}}");
        }
    }

    private void updateState(JsonObject jsonDataBlob) {
        boolean poweredLastReading = false;
        JsonElement lastReadingBlob = jsonDataBlob.get("last_reading");
        if (lastReadingBlob != null) {
            JsonElement poweredBlob = lastReadingBlob.getAsJsonObject().get("powered");
            if (poweredBlob != null && poweredBlob.getAsBoolean() == true) {
                poweredLastReading = true;
            }
        }
        boolean poweredDesiredState = false;
        JsonElement desiredStateBlob = jsonDataBlob.get("desired_state");
        if (desiredStateBlob != null) {
            JsonElement poweredBlob = desiredStateBlob.getAsJsonObject().get("powered");
            if (poweredBlob != null && poweredBlob.getAsBoolean() == true) {
                poweredDesiredState = true;
            }
        }
        // Don't update the state during a transition.
        if (poweredDesiredState == poweredLastReading) {
            updateState(CHANNEL_LIGHT_STATE, (poweredLastReading ? OnOffType.ON : OnOffType.OFF));
        }
    }

    @Override
    public void sendCommandCallback(JsonObject jsonResult) {
        // TODO: Is there something to do here? Maybe verify that the request succeed (e.g. that the device is online
        // etc...)
    }
    @Override
    public void updateDeviceStateCallback(JsonObject jsonDataBlob) {
        
    }
}
