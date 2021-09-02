package org.cristalise.kernel.process;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpBridgeVerticle extends AbstractVerticle {
    
    public static final int PORT = Gateway.getProperties().getInt("TcpBridgeVerticle.port", 7000);

    private TcpEventBusBridge bridge;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        bridge = TcpEventBusBridge.create(
            vertx,
            new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(".*t")));

        bridge.listen(PORT, res -> {
          if (res.succeeded()) {
              log.info("start() - listen to port:{}", PORT);
              startPromise.complete();
          }
          else {
              log.error("start() - CANNOT listen to port:{}", PORT, res.cause());
              startPromise.fail(res.cause());
          }
        });
    }

    @Override
    public void stop() throws Exception {
        bridge.close();
    }
}
