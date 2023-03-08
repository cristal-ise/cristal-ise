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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper;
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpBridgeClientVerticle extends AbstractVerticle {

    NetSocket socket;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        NetClient tcpClient = Vertx.vertx().createNetClient();

        String host = TcpBridge_host.getString();
        int    port = TcpBridge_port.getInteger();

        tcpClient.connect(port, host, result -> {
            if (result.succeeded()) {
                socket = result.result();

                vertx.eventBus().consumer(ItemVerticle.ebAddress, message -> {
                    JsonObject body = (JsonObject) message.body();
                    String action = message.headers().get("action");

                    log.debug("handler() - action:{} body:{}", action, body);

                    JsonObject header = new JsonObject();
                    header.put("action", action);

                    FrameHelper.sendFrame("send", ItemVerticle.ebAddress, ItemVerticle.ebAddress, header, true, body , socket);

                    socket.handler(new FrameParser(bufferResult -> {
                        if (bufferResult.succeeded()) {
                            JsonObject resultJson = bufferResult.result();

                            log.trace("handler() - returnJson:{}", resultJson);

                            if (resultJson.getString("type").equals("err")) {
                                message.fail(500, resultJson.toString());
                            }
                            else if (resultJson.getString("type").equals("pong")) {
                                // should never really happen, as it is the response to a 'ping' message
                                log.warn("handler() - received 'pong' returnJson:{}", this, resultJson);
                                message.reply("pong");
                            }
                            else {
                                log.debug("handler() - returning:{}", resultJson.getString("body"));
                                message.reply(resultJson.getString("body"));
                            }
                        }
                        else {
                            message.fail(500, bufferResult.cause().getMessage());
                        }
                    }));
                });
                startPromise.complete();
                log.info("start() - connected to {}:{}", host, port);
            }
            else {
                startPromise.fail(result.cause());
                log.error("start() - failed connection to {}:{}", host, port, result.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        String host = TcpBridge_host.getString();
        int    port = TcpBridge_port.getInteger();

        log.info("stop() - closing connection to {}:{}", host, port);
        socket.close();
    }
}
