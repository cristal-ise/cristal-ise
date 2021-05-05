package org.cristalise.kernel.entity.proxy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalMessageVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        EventBus eb = vertx.eventBus();

        eb.consumer(ProxyMessage.ebAddress, ebMessage -> {
            Object body = ebMessage.body();
            log.trace("handler() - message.body:{}", body);

            try {
                DeliveryOptions opt = new DeliveryOptions().setLocalOnly(true);

                for (Object element: (JsonArray)body) {
                    ProxyMessage msg = new ProxyMessage((String)element);

                    eb.publish(msg.getLocalEventBusAddress(), msg.getLocalEventBusMessage(), opt);
                }
            }
            catch (Exception e) {
                log.error("handler()", e);
            }
        });

        startPromise.complete();
        log.info("start() - deployed '{}' consumer", ProxyMessage.ebAddress);
    }
}
