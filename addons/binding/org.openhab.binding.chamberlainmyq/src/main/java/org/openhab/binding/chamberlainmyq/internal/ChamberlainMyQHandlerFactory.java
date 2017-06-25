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

//import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQHandler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQDoorOpenerHandler;
import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQGatewayHandler;
import org.openhab.binding.chamberlainmyq.handler.ChamberlainMyQLightHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * The {@link ChamberlainMyQHandlerFactory} is responsible for creating things and thing 
 * handlers.
 * 
 * @author Scott Hanson - Initial contribution
 */
public class ChamberlainMyQHandlerFactory extends BaseThingHandlerFactory {
    
    private Logger logger = LoggerFactory.getLogger(ChamberlainMyQHandlerFactory.class);
    
    public final static Set<ThingTypeUID> DISCOVERABLE_DEVICE_TYPES_UIDS = ImmutableSet.of(THING_TYPE_DOOR_OPENER,
            THING_TYPE_LIGHT);

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_MYQ_BRIDGE);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        logger.info("Checking if the factory supports {}", thingTypeUID.toString());
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)
                || DISCOVERABLE_DEVICE_TYPES_UIDS.contains(thingTypeUID);
    }
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
}

