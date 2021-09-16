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
package org.cristalise.kernel.process;

import java.util.ArrayList;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.proxy.ProxyMessage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

/**
 * Subscribes to the ProxyMessages channel to perform 2 actions: cleans the local cache of ClusterStorage 
 * and sends change notifications to local subscribers. The order is very important, the cache 
 * should be cleared first so the notified consumer will read the new value.
 */
@Slf4j
public class LocalChangeVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        vertx.eventBus().consumer(ProxyMessage.ebAddress, message -> {
            Object body = message.body();
            log.debug("handler() - message.body:{}", body);

            boolean publish = Gateway.getProperties().getBoolean("LocalChangeVerticle.publishLocalMessage", false);

            try {
                //order is important: see above
                clearCache((JsonArray)body);
                publishLocalMessages((JsonArray)body, publish);
            }
            catch (Exception e) {
                log.error("handler()", e);
            }
        });

        startPromise.complete();
        log.info("start() - '{}' consumer configured", ProxyMessage.ebAddress);
    }

    private void publishLocalMessages(JsonArray body, boolean publish) throws InvalidDataException {
        DeliveryOptions opt = new DeliveryOptions().setLocalOnly(true);

        for (Object element: body) {
            ProxyMessage msg = new ProxyMessage((String)element);
            String ebAddress = msg.getLocalEventBusAddress();
            String ebMsg     = msg.getLocalEventBusMessage();

            if (publish) {
                log.trace("handler() - publishing to address:{}, msg:{}", ebAddress, ebMsg);
                vertx.eventBus().publish(ebAddress, ebMsg, opt);
            }
            else {
                log.trace("handler() - sending to address:{}, msg:{}", ebAddress, ebMsg);
                vertx.eventBus().send(ebAddress, ebMsg, opt);
            }
        }
    }

    private void clearCache(JsonArray body) throws InvalidDataException {
        ArrayList<String> clearCacheList = new ArrayList<String>();

        for (Object element: body) {
            ProxyMessage msg = new ProxyMessage((String)element);

            if (msg.isClusterStoreMesssage()) {
                String key = msg.getItemPath().getUUID() + "/" + msg.getPath();
                log.trace("clearCache() - adding entry:{}", key);
                clearCacheList.add(key);
            }
        }

        Gateway.getStorage().clearCache(clearCacheList);
    }

    @Override
    public void stop() throws Exception {
        log.info("stop() - '{}' consumer", ProxyMessage.ebAddress);
    }
}
