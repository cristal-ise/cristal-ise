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

import static org.cristalise.kernel.SystemProperties.ItemVerticle_ebAddress;
import static org.cristalise.kernel.SystemProperties.ItemVerticle_includeDebugInfo;
import static org.cristalise.kernel.SystemProperties.ItemVerticle_instances;
import static org.cristalise.kernel.SystemProperties.ItemVerticle_requestTimeoutSeconds;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemVerticle extends VerticleBase {

    public static final String  ebAddress      = ItemVerticle_ebAddress.getString();
    public static final int     instances      = ItemVerticle_instances.getInteger();
    public static final boolean includeDebug   = ItemVerticle_includeDebugInfo.getBoolean();
    public static final int     requestTimeout = ItemVerticle_requestTimeoutSeconds.getInteger();

    private MessageConsumer<?> serviceRegistration;

    @Override
    public Future<?> start() throws Exception {
        serviceRegistration = new ServiceBinder(vertx)
                .setAddress(ebAddress)
                .setIncludeDebugInfo(includeDebug)
                .register(Item.class, new TraceableEntity());

        log.info("start() - service register done");
        return serviceRegistration.completion();
    }

    @Override
    public Future<?> stop() throws Exception {
        if (serviceRegistration != null) return serviceRegistration.unregister();
        else                             return Future.succeededFuture();
    }
}
