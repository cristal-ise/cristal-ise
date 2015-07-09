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

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer(String uri) {
    	
        // create a resource config that scans for JAX-RS resources and providers
        // in org.cristalise.restapi package
        final ResourceConfig rc = new ResourceConfig().packages("org.cristalise.restapi");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at the given URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(uri), rc);
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
    	String uri = Gateway.getProperties().getString("REST.URI", "http://localhost:8081/");
    	if (uri == null || uri.length()==0)
    		throw new BadArgumentsException("Please specify REST.URI on which to listen in config.");
        final HttpServer server = startServer(uri);
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", uri));
        System.in.read();
        server.stop();
    }
}

