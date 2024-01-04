package org.cristalise.kernel.process;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.ServerWebSocket;

public class WebSocketVerticle extends AbstractVerticle {

  @Override
  public void start() {
    vertx.createHttpServer().webSocketHandler(this::handleWebSocket).listen(8080);

    vertx.createHttpServer().webSocketHandler(ws -> ws.handler(ws::writeBinaryMessage)).requestHandler(req -> {
      if (req.uri().equals("/"))
        req.response().sendFile("io/vertx/example/core/http/websockets/ws.html");
    }).listen(8080);
  }

  private void handleWebSocket(ServerWebSocket ws) {
    ws.handler(ws::writeBinaryMessage);
  }
}

