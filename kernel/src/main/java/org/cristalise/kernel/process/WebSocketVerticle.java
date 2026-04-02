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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_host;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_path;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_port;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import org.cristalise.kernel.entity.proxy.ProxyMessage;

import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketVerticle extends VerticleBase {

    String  wsHost = WebSocketVerticle_host.getString();
    Integer wsPort = WebSocketVerticle_port.getInteger();
    String  wsPath = getNormalizedWsPath();

    private final Set<ServerWebSocket> proxyMsgSubscribers = ConcurrentHashMap.newKeySet();
    private HttpServer wsServer;
    private MessageConsumer<JsonArray> proxyMsgConsumer;

    @Override
    public Future<?> start() {
        proxyMsgConsumer = vertx.eventBus().consumer(ProxyMessage.ebAddress, this::publishToSubscribers);
        wsServer         = vertx.createHttpServer().webSocketHandler(this::handleWebSocket);

        return wsServer.listen(wsPort, wsHost)
            .mapEmpty()
            .onSuccess(ignored -> log.info("start() - listening on ws://{}:{}{}", wsHost, wsPort, wsPath))
            .onFailure(error -> {
                log.error("start() - error starting WebSocket server", error);
                if (proxyMsgConsumer != null) proxyMsgConsumer.unregister();
            });
    }

    private String getNormalizedWsPath() {
        String path = WebSocketVerticle_path.getString();
        if (isBlank(path)) return WebSocketVerticle_path.getDefaultValue().toString();
        return path.startsWith("/") ? path : "/" + path;
    }

    private void handleWebSocket(ServerWebSocket ws) {
        if (!wsPath.equals(ws.path())) {
            ws.close();
            return;
        }

        proxyMsgSubscribers.add(ws);
        log.debug("handleWebSocket() - connected:{} subscribers:{}", ws.remoteAddress(), proxyMsgSubscribers.size());

        ws.closeHandler(v -> proxyMsgSubscribers.remove(ws));
        ws.exceptionHandler(error -> {
            log.debug("handleWebSocket() - websocket failure", error);
            proxyMsgSubscribers.remove(ws);
            if (!ws.isClosed()) ws.close();
        });
    }

    private void publishToSubscribers(Message<JsonArray> proxyMessages) {
        String payload = proxyMessages.body().encode();

        for (ServerWebSocket ws : proxyMsgSubscribers) {
            if (ws.isClosed()) {
                proxyMsgSubscribers.remove(ws);
            } else {
                try {
                    ws.writeTextMessage(payload);
                } catch (Exception e) {
                    log.error("publishToSubscribers() - error messaging proxyMsgSubscribers", e);
                    proxyMsgSubscribers.remove(ws);
                    if (!ws.isClosed()) ws.close();
                }
            }
        }
    }

    @Override
    public Future<?> stop() {
        if (proxyMsgConsumer != null) proxyMsgConsumer.unregister();

        for (ServerWebSocket ws : proxyMsgSubscribers) {
            if (!ws.isClosed()) ws.close();
        }
        proxyMsgSubscribers.clear();

        if (wsServer == null) {
            return Future.succeededFuture();
        }

        return wsServer.close().mapEmpty();
    }
}
