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

import static org.cristalise.kernel.SystemProperties.TcpBridge_port;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TcpBridgeVerticle extends AbstractVerticle {

    private TcpEventBusBridge bridge;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        bridge = TcpEventBusBridge.create(
            vertx,
            new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex(".*"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(".*t")));

        int port = TcpBridge_port.getInteger();

        bridge.listen(port, res -> {
          if (res.succeeded()) {
              log.info("start() - listen to port:{}", port);
              startPromise.complete();
          }
          else {
              log.error("start() - CANNOT listen to port:{}", port, res.cause());
              startPromise.fail(res.cause());
          }
        });
    }

    @Override
    public void stop() throws Exception {
        int port = TcpBridge_port.getInteger();
        log.info("stop() - closing listening to port:{}", port);
        bridge.close();
    }
}
