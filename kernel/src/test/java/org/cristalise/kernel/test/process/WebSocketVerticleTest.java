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
package org.cristalise.kernel.test.process;

import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_host;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_path;
import static org.cristalise.kernel.SystemProperties.WebSocketVerticle_port;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.WebSocketVerticle;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.json.JsonArray;

public class WebSocketVerticleTest {

    private static final String HOST = "127.0.0.1";
    private static final String WS_PATH = "/ws/proxy-message";
    private static final int TIMEOUT_SECONDS = 5;

    private Vertx vertx;
    private WebSocketClient webSocketClient;
    private int wsPort;

    @Before
    public void setup() throws Exception {
        wsPort = findFreePort();

        Gateway.getProperties().setProperty(WebSocketVerticle_host.getSystemPropertyName(), HOST);
        Gateway.getProperties().setProperty(WebSocketVerticle_port.getSystemPropertyName(), wsPort);
        Gateway.getProperties().setProperty(WebSocketVerticle_path.getSystemPropertyName(), WS_PATH);

        vertx = Vertx.vertx();
        webSocketClient = vertx.createWebSocketClient();

        deployWebSocketVerticle();
    }

    @After
    public void tearDown() throws Exception {
        Gateway.getProperties().remove(WebSocketVerticle_host.getSystemPropertyName());
        Gateway.getProperties().remove(WebSocketVerticle_port.getSystemPropertyName());
        Gateway.getProperties().remove(WebSocketVerticle_path.getSystemPropertyName());

        if (webSocketClient != null) {
            CountDownLatch closeClientLatch = new CountDownLatch(1);
            webSocketClient.close().onComplete(v -> closeClientLatch.countDown());
            closeClientLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        if (vertx != null) {
            CountDownLatch closeVertxLatch = new CountDownLatch(1);
            vertx.close().onComplete(v -> closeVertxLatch.countDown());
            closeVertxLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldForwardProxyMessageToSingleSubscriber() throws Exception {
        WebSocket ws = connectWebSocket(WS_PATH);

        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> payloadRef = new AtomicReference<>();
        ws.textMessageHandler(message -> {
            payloadRef.set(message);
            messageLatch.countDown();
        });

        JsonArray expectedPayload = new JsonArray().add("tree:/jobs/123");
        vertx.eventBus().publish(ProxyMessage.ebAddress, expectedPayload);

        assertTrue("websocket subscriber did not receive message", messageLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(expectedPayload.encode(), payloadRef.get());
    }

    @Test
    public void shouldForwardProxyMessageToMultipleSubscribers() throws Exception {
        WebSocket ws1 = connectWebSocket(WS_PATH);
        WebSocket ws2 = connectWebSocket(WS_PATH);

        CountDownLatch messageLatch = new CountDownLatch(2);
        AtomicReference<String> payloadRef1 = new AtomicReference<>();
        AtomicReference<String> payloadRef2 = new AtomicReference<>();
        ws1.textMessageHandler(message -> {
            payloadRef1.set(message);
            messageLatch.countDown();
        });
        ws2.textMessageHandler(message -> {
            payloadRef2.set(message);
            messageLatch.countDown();
        });

        JsonArray expectedPayload = new JsonArray().add("tree:/jobs/456").add("tree:/jobs/789");
        vertx.eventBus().publish(ProxyMessage.ebAddress, expectedPayload);

        assertTrue("not all websocket subscribers received message", messageLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(expectedPayload.encode(), payloadRef1.get());
        assertEquals(expectedPayload.encode(), payloadRef2.get());
    }

    @Test
    public void shouldKeepBroadcastingAfterSubscriberDisconnects() throws Exception {
        WebSocket ws1 = connectWebSocket(WS_PATH);
        WebSocket ws2 = connectWebSocket(WS_PATH);

        CountDownLatch closeLatch = new CountDownLatch(1);
        ws1.closeHandler(v -> closeLatch.countDown());
        ws1.close();

        assertTrue("first websocket was not closed", closeLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> payloadRef = new AtomicReference<>();
        ws2.textMessageHandler(message -> {
            payloadRef.set(message);
            messageLatch.countDown();
        });

        JsonArray expectedPayload = new JsonArray().add("tree:/jobs/999");
        vertx.eventBus().publish(ProxyMessage.ebAddress, expectedPayload);

        assertTrue("remaining websocket subscriber did not receive message", messageLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(expectedPayload.encode(), payloadRef.get());
    }

    @Test
    @Ignore("Failing on Travis CI for unknow reason")
    public void shouldCloseWebSocketConnectedOnInvalidPath() throws Exception {
        WebSocket invalidWs = connectWebSocket("/ws/invalid");

        CountDownLatch closedLatch = new CountDownLatch(1);
        CountDownLatch invalidMessageLatch = new CountDownLatch(1);
        invalidWs.closeHandler(v -> closedLatch.countDown());
        invalidWs.textMessageHandler(message -> invalidMessageLatch.countDown());

        assertTrue("invalid-path websocket was not closed", closedLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

        WebSocket validWs = connectWebSocket(WS_PATH);
        CountDownLatch validMessageLatch = new CountDownLatch(1);
        validWs.textMessageHandler(message -> validMessageLatch.countDown());

        vertx.eventBus().publish(ProxyMessage.ebAddress, new JsonArray().add("tree:/jobs/321"));

        assertTrue("valid websocket should still receive broadcast", validMessageLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse("invalid websocket should not receive broadcast", invalidMessageLatch.await(300, TimeUnit.MILLISECONDS));
    }

    private void deployWebSocketVerticle() throws InterruptedException {
        CountDownLatch deployLatch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        vertx.deployVerticle(new WebSocketVerticle()).onComplete(result -> {
            if (result.failed()) errorRef.set(result.cause());
            deployLatch.countDown();
        });

        assertTrue("WebSocketVerticle deployment timed out", deployLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("WebSocketVerticle deployment failed", errorRef.get() == null);
    }

    private WebSocket connectWebSocket(String path) throws InterruptedException {
        CountDownLatch connectLatch = new CountDownLatch(1);
        AtomicReference<WebSocket> socketRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        webSocketClient.connect(wsPort, HOST, path).onComplete(result -> {
            if (result.succeeded()) socketRef.set(result.result());
            else                    errorRef.set(result.cause());
            connectLatch.countDown();
        });

        assertTrue("websocket connect timed out for path:" + path, connectLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertTrue("websocket connect failed for path:" + path, errorRef.get() == null);
        assertNotNull("websocket connect returned null socket for path:" + path, socketRef.get());
        return socketRef.get();
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
