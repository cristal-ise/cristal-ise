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
package org.cristalise.kernel.persistency;

import java.util.ArrayList;

import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.process.Gateway;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StorageInvalidatorVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        EventBus eb = vertx.eventBus();

        eb.consumer(ProxyMessage.ebAddress, message -> {
            Object body = message.body();
            log.info("handler() - message.body:{}", body);

            try {
                ArrayList<String> clearCacheList = new ArrayList<String>();

                if (body instanceof String) {
                    ProxyMessage m = new ProxyMessage((String) body);
                    clearCacheList.add(m.getItemPath().getUUID().toString() + "/" + m.getPath());
                }
                else if (body instanceof JsonArray) {
                    for (Object msg: (JsonArray)body) {
                        ProxyMessage m = new ProxyMessage((String) msg);
                        clearCacheList.add(m.getItemPath().getUUID().toString() + "/" + m.getPath());
                    }
                }
                Gateway.getStorage().clearCache(clearCacheList);
            }
            catch (Exception e) {
                log.error("handler()", e);
            }
        });

        log.info("start()");
        startPromise.complete();
    }
}
