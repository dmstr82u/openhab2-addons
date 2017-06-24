/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.chamberlainmyq.internal;

import static org.openhab.binding.chamberlainmyq.ChamberlainMyQBindingConstants.*;

import java.util.Collections;
import java.util.Set;

import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQHandler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.chamberlainmyq.handler.LightBulbHandler;
import org.openhab.binding.chamberlainmyq.handler.RemoteHandler;
import org.openhab.binding.chamberlainmyq.handler.WinkHub2Handler;
import org.openhab.binding.chamberlainmyq.handler.BinarySwitchHandler;

/**
 * The {@link ChamberlainMyQHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Scott Hanson - Initial contribution
 */
public class ChamberlainMyQHandlerFactory extends BaseThingHandlerFactory {
    
    private Logger logger = LoggerFactory.getLogger(ChamberlainMyQFactory.class);
    
    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_SAMPLE);
    
    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_MYQ_BRIDGE)) {
            return new ChamberlainMyQGatewayHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_DOOR_OPENER)) {
            return new ChamberlainMyQDoorOpenerHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_LIGHT)) {
            return new ChamberlainMyQLightHandler(thing);
        }

        return null;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    private ThingUID getBridgeThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration) {
        if (thingUID == null) {
            String myqID = (String) configuration.get(MYQ_ID);
            thingUID = new ThingUID(thingTypeUID, myqID);
        }
        return thingUID;
    }

    private ThingUID getDeviceUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        String myqID = (String) configuration.get(MYQ_ID);

        if (thingUID == null) {
            thingUID = new ThingUID(thingTypeUID, myqID, bridgeUID.getId());
        }
        return thingUID;
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_SAMPLE)) {
            return new ChamberlainMyQHandler(thing);
        }

        return null;
    }
}

