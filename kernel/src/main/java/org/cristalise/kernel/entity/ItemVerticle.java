package org.cristalise.kernel.entity;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;

public class ItemVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        // start service and register the handler
        Item service = new TraceableEntity();

        new ServiceBinder(vertx)
          .setAddress(Item.ebAddress)
          .register(Item.class, service);

        startPromise.complete();
    }
}
