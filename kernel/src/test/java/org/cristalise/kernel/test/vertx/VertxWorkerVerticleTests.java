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
