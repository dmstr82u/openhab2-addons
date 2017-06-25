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

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.chamberlainmyq.config.ChamberlainMyQDeviceConfig;
import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQGatewayHandler.RequestCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link ChamberlainMyQHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 * 
 * @author Scott Hanson - Initial contribution
 */
public abstract class ChamberlainMyQHandler extends BaseThingHandler {
    /**
     * Base configuration of this device.
     */
    protected ChamberlainMyQDeviceConfig deviceConfig;

    private Logger logger = LoggerFactory.getLogger(ChamberlainMyQHandler.class);
    /**
     * Creates a new instance of this class for the {@link Thing}.
     *
     * @param thing the thing that should be handled, not null.
     */
    public ChamberlainMyQHandler(Thing thing) {
        super(thing);
        /*String config = (String) getThing().getConfiguration().get(WINK_DEVICE_CONFIG);
        logger.trace("Initializing a thing with the following config: {}", config);*/
        String id = (String) getThing().getConfiguration().get(MYQ_ID);
        logger.trace("Thing ID: {}", id);
        this.deviceConfig = new ChamberlainMyQDeviceConfig(id);
        /*parseConfig(config);*/
        logger.info("Initializing a MyQ device: \n{}", deviceConfig.asString());
    }

    /**
     * Parse the configuration of this thing.
     *
     * @param jsonConfigString the string containing the configuration of this thing as returned by the hub (in JSON).
     */
    protected void parseConfig(String jsonConfigString) {
        logger.trace("Parsing a thing's config: {}", jsonConfigString);
        JsonParser parser = new JsonParser();
        deviceConfig.readConfigFromJson(parser.parse(jsonConfigString).getAsJsonObject());
    }

    /**
     * Used to retrieve the {@link ChamberlainMyQGatewayHandler} controlling this thing.
     *
     * @return this thing gateway handler, null if it hasn't been set yet.
     */
    protected ChamberlainMyQGatewayHandler getGatewayHandler() {
        Bridge gateway = getBridge();
        return gateway == null ? null : (ChamberlainMyQGatewayHandler) gateway.getHandler();
    }

    /**
     * Function called when a communication error between the gateway and the thing has been detected.
     */
    protected void HandleCommunicationError(String errorMessage) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, errorMessage);
    }

        /**
     * Abstract method that should return the device path that get appended to the
     * Wink server URL when making a request.
     *
     * It usually has the form "{device_type}/{device_id}".
     *
     * @return this thing REST request path.
     */
    protected abstract String getDeviceRequestPath();

    private abstract class ChamberlainMyQDeviceRequestCallback implements RequestCallback {
        /**
         * Handler of the thing that should for which the configuration should be set.
         */
        protected ChamberlainMyQHandler handler;

        /**
         * Creates a new instance of this class.
         *
         * @param handler The handler for which the configuration should be read.
         */
        public ChamberlainMyQDeviceRequestCallback(ChamberlainMyQHandler handler) {
            Preconditions.checkArgument(handler != null, "The argument |handler| must not be null.");
            this.handler = handler;
        }

        @Override
        public void OnError(String error) {
            handler.HandleCommunicationError(error);
        }
    }

    //////////// Read State functions ////////////

    /**
     * Specialization of a {link RequestCallback} to read a device configuration.
     *
     * @author Scott Hanson
     */
    private class ReadDeviceStateCallback extends ChamberlainMyQDeviceRequestCallback {
        /**
         * Creates a new instance of this class.
         *
         * @param handler The handler for which the state should be read.
         */
        public ReadDeviceStateCallback(ChamberlainMyQHandler handler) {
            super(handler);
        }

        @Override
        public void parseRequestResult(JsonObject jsonResult) {
            logger.trace("Parsing a ReadDeviceState request result: {}", jsonResult);
            // The response from the server is a JSON object containing the device information and state.
            handler.updateDeviceStateCallback(jsonResult.get("data").getAsJsonObject());
        }
    }

    /**
     * Query the {@link WinkHub2Handler} for this device's state.
     */
    protected void ReadDeviceState() {
        logger.trace("Querying the device state for: \n{}", deviceConfig.asString());
        try {
            getGatewayHandler().sendRequestToServer(getDeviceRequestPath(), new ReadDeviceStateCallback(this));
        } catch (IOException e) {
            logger.error("Error while querying the hub for {}", getDeviceRequestPath(), e);
        }
    }

    /**
     * Callback called once the device state has been updated.
     *
     * @param jsonDataBlob the reply from the hub.
     */
    abstract protected void updateDeviceStateCallback(JsonObject jsonDataBlob);

    /////////////////////////////////////////////////

    //////////// Send commands functions ////////////

    private class SendCommandCallback extends ChamberlainMyQDeviceRequestCallback {
        public SendCommandCallback(ChamberlainMyQHandler handler) {
            super(handler);
        }

        @Override
        public void parseRequestResult(JsonObject jsonResult) {
            logger.trace("Parsing a SendCommandCallback request result: {}", jsonResult);
            handler.sendCommandCallback(jsonResult);
        }
    }

    public void sendCommand(String payLoad) {
        logger.trace("Sending a command with the following payload: {}\nto device: \n", payLoad, deviceConfig.asString());
        try {
            getGatewayHandler().sendRequestToServer(getDeviceRequestPath() + "/desired_state",
                    new SendCommandCallback(this), payLoad);
        } catch (IOException e) {
            logger.error("Error while querying the hub for {}", getDeviceRequestPath(), e);
        }
    }

    abstract public void sendCommandCallback(JsonObject jsonResult);
}
