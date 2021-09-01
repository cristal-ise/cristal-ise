package org.cristalise.kernel.process;

import org.cristalise.kernel.entity.ItemVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpBridgeVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        TcpEventBusBridge bridge = TcpEventBusBridge.create(
            vertx,
            new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddress("in"))
                .addOutboundPermitted(new PermittedOptions().setAddress("out")));

        bridge.listen(7000, res -> {
          if (res.succeeded()) {
            // succeed...
          } else {
            // fail...
          }
        });

        startPromise.complete();
        log.info("start() - deployed to bridge '{}'", ItemVerticle.ebAddress);
    }
}
