package org.cristalise.restapi;

import org.cristalise.kernel.process.Gateway;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * Add CORS headers to each Response
 */
public class CORSResponseFilter implements ContainerResponseFilter {

    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException
    {
        MultivaluedMap<String, Object> respHeaders = responseContext.getHeaders();

        respHeaders.add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia");
        respHeaders.add("Access-Control-Allow-Origin", Gateway.getProperties().getString("REST.corsAllowOrigin", "*"));
        respHeaders.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
    }
}
