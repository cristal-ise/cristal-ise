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

import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_host;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_path;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_port;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.cristalise.kernel.entity.proxy.ProxyMessage;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebSocketVerticle extends AbstractVerticle {

    private final Set<ServerWebSocket> subscribers = ConcurrentHashMap.newKeySet();
    private HttpServer server;
    private MessageConsumer<JsonArray> proxyConsumer;

    @Override
    public void start(Promise<Void> startPromise) {
        String host = WebSocketVerticle_host.getString();
        int port = WebSocketVerticle_port.getInteger();
        String path = normalizePath(WebSocketVerticle_path.getString());

        proxyConsumer = vertx.eventBus().consumer(ProxyMessage.ebAddress, message -> {
            JsonArray proxyMessages = (JsonArray) message.body();
            publishToSubscribers(proxyMessages);
        });

        server = vertx.createHttpServer().webSocketHandler(ws -> handleWebSocket(ws, path));

        server.listen(port, host).onComplete(result -> {
            if (result.succeeded()) {
                log.info("start() - listening on ws://{}:{}{}", host, port, path);
                startPromise.complete();
            }
            else {
                if (proxyConsumer != null) proxyConsumer.unregister();
                startPromise.fail(result.cause());
            }
        });
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/ws/proxy-message";
        return path.startsWith("/") ? path : "/" + path;
    }

    private void handleWebSocket(ServerWebSocket ws, String path) {
        if (!path.equals(ws.path())) {
            ws.close();
            return;
        }

        subscribers.add(ws);
        log.debug("handleWebSocket() - connected:{} subscribers:{}", ws.remoteAddress(), subscribers.size());

        ws.closeHandler(v -> subscribers.remove(ws));
        ws.exceptionHandler(error -> {
            log.debug("handleWebSocket() - websocket failure", error);
            subscribers.remove(ws);
            if (!ws.isClosed()) ws.close();
        });
    }

    private void publishToSubscribers(JsonArray proxyMessages) {
        String payload = proxyMessages.encode();

        for (ServerWebSocket ws : subscribers) {
            if (ws.isClosed()) {
                subscribers.remove(ws);
                continue;
            }

            try {
                ws.writeTextMessage(payload);
            }
            catch (Exception e) {
                subscribers.remove(ws);
                if (!ws.isClosed()) ws.close();
            }
        }
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        if (proxyConsumer != null) proxyConsumer.unregister();

        for (ServerWebSocket ws : subscribers) {
            if (!ws.isClosed()) ws.close();
        }
        subscribers.clear();

        if (server == null) {
            stopPromise.complete();
            return;
        }

        server.close().onComplete(result -> {
            if (result.succeeded()) stopPromise.complete();
            else                    stopPromise.fail(result.cause());
        });
    }
}
