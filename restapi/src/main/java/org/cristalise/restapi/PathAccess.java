package org.cristalise.restapi;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
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
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;

@Path("/domain/{path: .*}")
public class PathAccess {
	ObjectMapper mapper;
	
	public PathAccess() {
		mapper = new ObjectMapper();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response queryPath(@PathParam("path") String path, 
			@QueryParam("start") Integer start, @QueryParam("batch") Integer batchSize, @QueryParam("search") String search,
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
		Iterator<org.cristalise.kernel.lookup.Path> childSearch;
		if (search == null)
				childSearch = Gateway.getLookup().getChildren(domPath);
		else {
			String[] terms; // format: name,prop:val,prop:val
			terms = search.split(",");
				
			Property[] props = new Property[terms.length];
			for (int i=0; i<terms.length; i++) {
				if (terms[i].contains(":")) { // assemble property if we have name:val
					String[] nameval = terms[i].split(":");
					if (nameval.length != 2) throw new WebApplicationException("Invalid search term: "+terms[i], 400);
					props[i] = new Property(nameval[0], nameval[1]);
				}
				else if (i==0) { // first search term can imply Name if no propname given
					props[i] = new Property("Name", terms[i]);
				}
				else {
					throw new WebApplicationException("Only the first search term may omit property name", 400);
				}
			}
			childSearch = Gateway.getLookup().search(domPath, props);
		}
		
		// skip to the start
		for ( int i = 0; i<start; i++ )  {
			if (childSearch.hasNext())
				childSearch.next();
			else
				break;
		}
		// create list
		
		for  (int i = 0; i<batchSize; i++) {
			if (childSearch.hasNext()) {
				org.cristalise.kernel.lookup.Path nextPath = childSearch.next();
				Logger.msg(nextPath.toString());
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
				else if (nextPath instanceof ItemPath) {
					ItemPath itemPath = (ItemPath)nextPath;
					URI nextPathURI = uri.getBaseUriBuilder().path("item").path(itemPath.getUUID().toString()).build();
					String itemName;
					try {
						itemName = Gateway.getProxyManager().getProxy(itemPath).getName();
					} catch (ObjectNotFoundException e) {
						itemName = itemPath.getUUID().toString();
					}
					childPathData.put(itemName, nextPathURI);
				}
			}
			else // all done
				break;
		}
		// if there are more, give a link
		if (childSearch.hasNext())
				childPathData.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", start+batchSize+1).replaceQueryParam("batch", batchSize).build());
		String childPathDataJSON;
		try {
			childPathDataJSON = mapper.writeValueAsString(childPathData);
		} catch (IOException e) {
			Logger.error(e);
			throw new WebApplicationException("Problem building response JSON: "+e.getMessage());
		}
		r = Response.ok(childPathDataJSON).build();
		return r;
	}
}
