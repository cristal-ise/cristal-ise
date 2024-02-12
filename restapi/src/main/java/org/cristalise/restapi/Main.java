/**
 * This file is part of the CRISTAL-iSE REST API.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.restapi;

import static org.cristalise.restapi.SystemProperties.REST_URI;
import static org.cristalise.restapi.SystemProperties.REST_addCorsHeaders;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.ShutdownHandler;
import org.cristalise.kernel.process.StandardClient;
import org.cristalise.kernel.process.resource.BadArgumentsException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Main class to launch the Test Restapi server. It is based on grizzly HTTP server.
 */
@Slf4j
public class Main extends StandardClient {

    static HttpServer server;

    /**
     * Initialise standard CRISTAL-iSE client process.
     * Creates ResourceConfig that scans for JAX-RS resources and providers in 'org.cristalise.restapi' package
     * Creates Grizzly HTTP server exposing the Jersey application at the given URI.
     * @throws Exception
     */
    public static void startServer(String[] args) throws Exception {
        setShutdownHandler(new ShutdownHandler() {
            @Override
            public void shutdown(int errCode, boolean isServer) {
                if (server != null) server.shutdownNow();
            }
        });

        standardInitialisation(args);

        String uri = REST_URI.getString();;

        if (StringUtils.isBlank(uri)) {
            throw new BadArgumentsException("Please specify REST.URI on which to listen in config.");
        }

        final ResourceConfig rc = new ResourceConfig().packages("org.cristalise.restapi");

        rc.register(MultiPartFeature.class);

        if (REST_addCorsHeaders.getBoolean()) rc.register(CORSResponseFilter.class);

        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(uri), rc);
        log.info("startServer() - Jersey app started with WADL available at {}/application.wadl", uri);

        server.start();
        log.info("startServer() - DONE");

        Thread.currentThread().join();
    }

    /**
     * Very basic main method to Start HTTP server and initialise CRISTAL-iSE connection.
     *
     * @param args input parameters
     */
    public static void main(String[] args) {
        try {
            startServer(args);
        }
        catch (Exception e) {
            log.error("main()", e);
            AbstractMain.shutdown(1);
        }

        AbstractMain.shutdown(0);
    }
}