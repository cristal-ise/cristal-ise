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
package org.cristalise.kernel.test.vertx;

import org.junit.Test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxWorkerVerticleTests {

    @Test
    public void executeBlocking() throws InterruptedException {
        Vertx vertx = Vertx.vertx();
        log.info("Hello vertx");

        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start(Promise<Void> startFuture) {
                // work handler
                Handler<Message<String>> handler = message -> {
                    log.info("Received message '{}'", message.body());

                    // do work
                    try {
                        log.info("Working ...");
                        Thread.sleep(5000);
                        log.info("Work done!");
                    }
                    catch (InterruptedException e) {
                        log.error("hadler()", e);
                    }

                    message.reply("Ca va bien!");
                };

                // wait for work
                vertx.eventBus().consumer("worker", handler).completionHandler(r -> {
                    startFuture.complete();
                });
            }
        }, new DeploymentOptions().setWorker(true));

        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() {
                // reply handler
                Handler<AsyncResult<Message<String>>> replyHandler = message -> {
                    log.info("Received reply '" + message.result().body() + "'");
                };

                // dispatch work
                vertx.eventBus().request("worker", "Ca va!", replyHandler);
            }
        });
        Thread.sleep(5000);
        log.info("Bye vertx");

    }
}
