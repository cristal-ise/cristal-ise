package org.cristalise.restapi;

import java.io.IOException;
import java.net.URI;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.StandardClient;
import org.cristalise.kernel.process.resource.BadArgumentsException;
import org.cristalise.kernel.utils.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Main class.
 *
 */
public class Main extends StandardClient {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://localhost:8081/";

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
    	
        // create a resource config that scans for JAX-RS resources and providers
        // in org.cristalise.restapi package
        final ResourceConfig rc = new ResourceConfig().packages("org.cristalise.restapi");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    /**
     * Main method.
     * @param args
     * @throws IOException
     * @throws BadArgumentsException 
     * @throws InvalidDataException 
     * @throws PersistencyException 
     */
    public static void main(String[] args) throws IOException, InvalidDataException, BadArgumentsException, PersistencyException {
    	Gateway.init(readC2KArgs(args));
    	Gateway.connect();
        final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        server.stop();
    }
}

