package org.cristalise.restapi;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;

@Path("/domain/{path: .*}")
public class PathAccess {

	public PathAccess() {
		// TODO Auto-generated constructor stub
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryPath(@PathParam("path") String path, 
			@QueryParam("start") Integer start, @QueryParam("batch") Integer batchSize, 
			@Context UriInfo uri) {	
		DomainPath domPath = new DomainPath(path);
		Response r;
		if (start == null) start = 0;
		if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Path.DefaultBatchSize", 75);
		
		// Return 404 if the domain path doesn't exist
		if (!domPath.exists()) 
			throw new WebApplicationException(404);
		
		// If the domain path represents an item, redirect to it
		try {
			ItemPath item = domPath.getItemPath();
			r = Response.seeOther(uri.getBaseUriBuilder().path("item").path(item.getUUID().toString()).build()).build();
			return r;
		} catch (ObjectNotFoundException ex) { } // not an item
		LinkedHashMap<String, URI> childPathData = new LinkedHashMap<String, URI>();
		Iterator<org.cristalise.kernel.lookup.Path> childSearch = Gateway.getLookup().getChildren(domPath);
		// skip to the start
		for ( int i = 0; i<start; i++ )  {
			if (childSearch.hasNext())
				childSearch.next();
			else
				throw new WebApplicationException(404);
		}
		// create list
		
		for  (int i = 0; i<batchSize; i++) {
			if (childSearch.hasNext()) {
				org.cristalise.kernel.lookup.Path nextPath = childSearch.next();
				if (nextPath instanceof DomainPath) {
					DomainPath nextDom = (DomainPath)nextPath;
					URI nextPathURI;
					try {
						ItemPath nextItem = nextDom.getItemPath();
						nextPathURI = uri.getBaseUriBuilder().path("item").path(nextItem.getUUID().toString()).build();
					} catch (ObjectNotFoundException ex) { 
						nextPathURI = uri.getAbsolutePathBuilder().path(nextDom.getName()).build();
					}
					childPathData.put(nextDom.getName(), nextPathURI);
				}
			}
			else // all done
				break;
		}
		// if there are more, give a link
		if (childSearch.hasNext())
				childPathData.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", start+batchSize+1).replaceQueryParam("batch", batchSize).build());
		ObjectMapper mapper = new ObjectMapper();
		String childPathDataJSON;
		try {
			childPathDataJSON = mapper.writeValueAsString(childPathData);
		} catch (IOException e) {
			e.printStackTrace();
			r = Response.serverError().entity("Problem building response JSON: "+e.getMessage()).build();
			return r;
		}
		r = Response.ok(childPathDataJSON).build();
		return r;
	}
}
