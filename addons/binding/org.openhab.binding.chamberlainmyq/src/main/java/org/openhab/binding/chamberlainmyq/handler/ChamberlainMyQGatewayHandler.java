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

import org.openhab.binding.chamberlainmyq.internal.ChamberlainMyQResponseCode;
import org.openhab.binding.chamberlainmyq.internal.InvalidDataException;
import org.openhab.binding.chamberlainmyq.internal.InvalidLoginException;
import org.openhab.binding.chamberlainmyq.internal.HttpUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.chamberlainmyq.config.ChamberlainMyQGatewayConfig;
import org.openhab.binding.chamberlainmyq.internal.discovery.ChamberlainMyQDeviceDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

//import org.eclipse.smarthome.io.net.http.HttpUtil;

/**
 * TODO: The {@link ChamberlainMyQGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Scott Hanson - Initial contribution
 */
public class ChamberlainMyQGatewayHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(ChamberlainMyQGatewayHandler.class);
    
    private String securityToken;
    //private Properties header;
    
    public ChamberlainMyQGatewayHandler(Bridge bridge) {
        super(bridge);
        
        /*header = new Properties();
        header.put("Accept", "application/json");
        header.put("User-Agent", USERAGENT);
        logger.debug("User-Agent: {}", USERAGENT);
        header.put("BrandId", BRANDID);
        logger.debug("BrandId: {}", BRANDID);
        header.put("ApiVersion", APIVERSION);
        logger.debug("ApiVersion: {}", APIVERSION);
        header.put("Culture", CULTURE);
        logger.debug("Culture: {}", CULTURE);
        header.put("MyQApplicationId", APP_ID);
        logger.debug("MyQApplicationId: {}", APP_ID);*/
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.error("The gateway doesn't support any command!");
    }

    @Override
    public void initialize() {
        this.config = getThing().getConfiguration().as(ChamberlainMyQGatewayConfig.class);
        if (validConfiguration()) {
            ChamberlainMyQDeviceDiscoveryService discovery = new ChamberlainMyQDeviceDiscoveryService(this);

            this.bundleContext.registerService(DiscoveryService.class, discovery, null);

            this.scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    // TODO
                    // connect();
                }
            }, 0, TimeUnit.SECONDS);
        }
        updateStatus(ThingStatus.ONLINE);
    }

    private boolean validConfiguration() {
        if (this.config == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Gateway configuration missing");
            return false;
        } else if (StringUtils.isEmpty(this.config.username)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "username not specified");
            return false;
        } else if (StringUtils.isEmpty(this.config.password)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "password not specified");
            return false;
        }
        else if(!login()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "failed to login to Chamberlain MyQ Service");
            return false;
        }
        return true;
    }

    public ChamberlainMyQGatewayConfig getGatewayConfig() {
        return this.config;
    }

    private ChamberlainMyQGatewayConfig config;

    // REST API variables
    /**
     * Returns the currently cached security token, this will make a call to
     * login if the token does not exist.
     *
     * @return The cached security token
     * @throws IOException
     * @throws InvalidLoginException
     */
    private String getSecurityToken() throws IOException, InvalidLoginException {
        if (securityToken == null) {
            login();
        }
        return securityToken;
    }
    
    private boolean login() {
        logger.debug("attempting to login");
        
        String url = String.format("%s/api/v4/User/Validate", WEBSITE);

        String message = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                this.config.username,  this.config.password);
        //Result result = http.post(url, message);
        //logger.trace(result.getBody());
        JsonObject data = request("POST", url, message,"application/json", true);
        
        if (!data.get("SecurityToken").isJsonNull()) {
            securityToken = data.get("SecurityToken").getAsString();
            logger.trace("myq securityToken: {}", securityToken);
            return true;
        }
        return false;
    }
    
    /**
     * Retrieves MyQ device data from myq website, throws if connection
     * fails or user login fails
     *
     */
    public JsonObject getMyqData() throws InvalidLoginException, IOException {
        logger.debug("Retrieving door data");
        String url = String.format("%s/api/v4/userdevicedetails/get?appId=%s&SecurityToken=%s", WEBSITE, enc(APP_ID),
                enc(getSecurityToken()));

        JsonObject data = request("GET", url, null, null, true);

        return data;
    }
    

    public interface RequestCallback {
        public void parseRequestResult(JsonObject resultJson);

        public void OnError(String error);
    }

    protected class Request implements Runnable {
        private String target;
        private RequestCallback callback;
        private String payLoad;

        public Request( RequestCallback callback) {
            //this.target = myqTarget.path(targetPath);
            this.callback = callback;
            //this.payLoad = payLoad;
        }
        
        public Request(String target, RequestCallback callback, String payLoad) {
            this.target = target;
            this.callback = callback;
            this.payLoad = payLoad;
        }

        protected String checkForFailure(JsonObject jsonResult) {
            if (jsonResult.get("data").isJsonNull()) {
                return jsonResult.get("errors").getAsString();
            }
            return null;
        }

        @Override
        public void run() {
            try {
                JsonObject resultJson = getMyqData();
                //String result = invokeAndParse(this.payLoad);
                //logger.debug("Gateway replied with: {}", result);
                //JsonParser parser = new JsonParser();
                //JsonObject resultJson = parser.parse(result).getAsJsonObject();

                //String errors = checkForFailure(resultJson);
                //if (errors != null) {

                //}
                callback.parseRequestResult(resultJson);
            } catch (Exception e) {
                logger.error("An exception occurred while executing a request to the Gateway: '{}'", e.getMessage());
            }
        }
    }

    public void sendRequestToServer( RequestCallback callback) throws IOException {
        sendRequestToServer(callback);
    }

    public void sendRequestToServer( String path, RequestCallback callback, String payLoad)
            throws IOException {
        Request request = new Request( callback);
        request.run();
    }
    
    // UTF-8 URL encode
    private String enc(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            // throw new EndOfTheWorldException()
            throw new UnsupportedOperationException("UTF-8 not supported");
        }
    }
    
    /**
     * Make a request to the server, optionally retry the call if there is a
     * login issue. Will throw a InvalidLoginExcpetion if the account is
     * invalid, locked or soon to be locked.
     *
     * @param method
     *            The Http Method Type (GET,PUT)
     * @param url
     *            The request URL
     * @param payload
     *            Payload string for put operations
     * @param payloadType
     *            Payload content type for put operations
     * @param retry
     *            Retry the attempt if our session key is not valid
     * @return The JsonNode representing the response data
     * @throws IOException
     * @throws InvalidLoginException
     */
    private synchronized JsonObject request(String method, String url, String payload, String payloadType, boolean retry) {
           
        Properties header;
        header = new Properties();
        header.put("Accept", "application/json");
        header.put("Connection", "keep-alive");
        //header.put("Content-Type", "application/json");
        //header.put("User-Agent", USERAGENT);
        //logger.debug("User-Agent: {}", USERAGENT);
        header.put("BrandId", BRANDID);
        logger.debug("BrandId: {}", BRANDID);
        header.put("ApiVersion", APIVERSION);
        logger.debug("ApiVersion: {}", APIVERSION);
        header.put("Culture", CULTURE);
        logger.debug("Culture: {}", CULTURE);
        header.put("MyQApplicationId", APP_ID);
        logger.debug("MyQApplicationId: {}", APP_ID);
        logger.debug("Requesting method {}", method);
        logger.debug("Requesting URL {}", url);
        logger.debug("Requesting payload {}", payload);
        logger.debug("Requesting payloadType {}", payloadType);

        String dataString;
        try {
              dataString = HttpUtil.executeUrl(method, url, header, payload == null ? null : IOUtils.toInputStream(payload),
                payloadType, (this.config.timeout*1000));

               logger.debug("Received MyQ JSON: {}", dataString);

               if (dataString == null) {
                 logger.error("Null response from MyQ server");
                  throw new IOException("Null response from MyQ server");
               }
        } catch (Exception e) {
            logger.error("Requesting URL Failed",e.getMessage());
            return new JsonObject();
        }
        try {
            JsonParser parser = new JsonParser();
            JsonObject rootNode = parser.parse(dataString).getAsJsonObject();
            
            int returnCode = Integer.parseInt(rootNode.get("ReturnCode").getAsString());
            logger.debug("myq ReturnCode: {}", returnCode);

            ChamberlainMyQResponseCode rc = ChamberlainMyQResponseCode.fromCode(returnCode);

            switch (rc) {
                case OK: {
                    return rootNode;
                }
                case ACCOUNT_INVALID:
                case ACCOUNT_NOT_FOUND:
                case ACCOUNT_LOCKED:
                case ACCOUNT_LOCKED_PENDING:
                    // these are bad, we do not want to continue to log in and
                    // lock an account
                    //throw new InvalidLoginException(rc.getDesc());
                    logger.error("Your MyQ Acount is Locked: ", rc.getDesc());
                    return new JsonObject();
                case LOGIN_ERROR:
                    // Our session key has expired, request a new one
                    if (retry) {
                        login();
                        return request(method, url, payload, payloadType, false);
                    }
                    // fall through to default
                default:
                    logger.error("Request Failed: ", rc.getDesc());
                    return new JsonObject();
            }

        } catch (Exception e) {
            logger.error("Could not parse response",e.getMessage());
            return new JsonObject();
        }
    }
}
