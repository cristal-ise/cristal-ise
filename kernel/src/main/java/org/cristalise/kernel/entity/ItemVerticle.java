/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.kernel.entity;

import static org.cristalise.kernel.process.Gateway.getProperties;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.serviceproxy.ServiceException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemVerticle extends AbstractVerticle {

    public static final String EB_ADDRESS_PROPERTY    = "ItemVerticle.ebAddress";
    public static final String IS_WORKER_PROPERTY     = "ItemVerticle.isWorker";
    public static final String INSTANCES_PROPERTY     = "ItemVerticle.instances";
    public static final String INCLUDE_DEBUG_PROPERTY = "ItemVerticle.includeDebugInfo";

    /**
     * The address name of the vertx event bus. Use {@value #EB_ADDRESS_PROPERTY} SystemProperty to configure it. 
     * Default value is 'cristalise-items'
     */
    public static final String ebAddress = getProperties().getString(EB_ADDRESS_PROPERTY, "cristalise-items");
    /**
     * If the verticle is a worker verticle or not. Use {@value #IS_WORKER_PROPERTY} SystemProperty to configure it. 
     * Default value is 'true'
     */
    public static final boolean isWorker = getProperties().getBoolean(IS_WORKER_PROPERTY, true);
    /**
     * The number of deployed verticle instances. Use {@value #INSTANCES_PROPERTY} SystemProperty to configure it. 
     * Default value is 8
     */
    public static final int instances = getProperties().getInt(INSTANCES_PROPERTY, 8);
    /**
     * If the {@link ServiceException} includes debug information or not. Use {@value #INCLUDE_DEBUG_PROPERTY} 
     * SystemProperty to configure it. Default value is 'true'
     */
    public static final boolean includeDebug = getProperties().getBoolean(INCLUDE_DEBUG_PROPERTY, true);

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // start service and register the handler
        Item service = new TraceableEntity();

        new ServiceBinder(vertx)
                .setAddress(ebAddress)
                .setIncludeDebugInfo(includeDebug)
                .register(Item.class, service);

        startPromise.complete();
        log.info("start() - service register done");
    }
}
