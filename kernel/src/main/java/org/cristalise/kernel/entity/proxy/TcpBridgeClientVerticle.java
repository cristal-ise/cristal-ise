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
package org.cristalise.kernel.entity.proxy;

import static org.cristalise.kernel.SystemProperties.*;

import org.cristalise.kernel.entity.ItemVerticle;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper;
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpBridgeClientVerticle extends VerticleBase {

    NetSocket socket;
    NetClient tcpClient;
    MessageConsumer<JsonObject> itemConsumer;

    @Override
    public Future<?> start() throws Exception {
        tcpClient = vertx.createNetClient();

        String host = TcpBridge_host.getString();
        int    port = TcpBridge_port.getInteger();

        return tcpClient.connect(port, host)
            .onSuccess(connectedSocket -> onConnected(connectedSocket, host, port))
            .onFailure(error -> onConnectionFailed(error, host, port))
            .mapEmpty();
    }

    private void onConnected(NetSocket connectedSocket, String host, int port) {
        socket = connectedSocket;
        itemConsumer = vertx.eventBus().consumer(ItemVerticle.ebAddress, this::handleItemRequest);
        log.info("onConnected() - connected to {}:{}", host, port);
    }

    private void onConnectionFailed(Throwable error, String host, int port) {
        if (itemConsumer != null) itemConsumer.unregister();
        log.error("onConnectionFailed() - failed connection to {}:{}", host, port, error);
    }

    private void handleItemRequest(Message<JsonObject> message) {
        JsonObject body = message.body();
        String action = message.headers().get("action");

        log.debug("handleItemRequest() - action:{} body:{}", action, body);

        JsonObject header = new JsonObject().put("action", action);
        FrameHelper.sendFrame("send", ItemVerticle.ebAddress, ItemVerticle.ebAddress, header, true, body, socket);

        socket.handler(new FrameParser(bufferResult ->
                handleFrameParseResult(message, bufferResult))
        );
    }

    private void handleFrameParseResult(Message<JsonObject> message, AsyncResult<JsonObject> bufferResult) {
        if (bufferResult.failed()) {
            message.fail(500, bufferResult.cause().getMessage());
            return;
        }

        JsonObject resultJson = bufferResult.result();
        log.trace("handleFrameParseResult() - returnJson:{}", resultJson);

        String type = resultJson.getString("type");
        if ("err".equals(type)) {
            message.fail(500, resultJson.toString());
        }
        else if ("pong".equals(type)) {
            // should never really happen, as it is the response to a 'ping' message
            log.warn("handleFrameParseResult() - received 'pong' returnJson:{}", resultJson);
            message.reply("pong");
        }
        else {
            log.debug("handleFrameParseResult() - returning:{}", resultJson.getString("body"));
            message.reply(resultJson.getString("body"));
        }
    }

    @Override
    public Future<?> stop() throws Exception {
        log.info("stop() - closing connection to {}:{}", TcpBridge_host.getString(), TcpBridge_port.getInteger());

        if (socket != null) socket.close();
        if (tcpClient != null) tcpClient.close();

        if (itemConsumer != null) return itemConsumer.unregister();
        else                      return Future.succeededFuture();
    }
}
