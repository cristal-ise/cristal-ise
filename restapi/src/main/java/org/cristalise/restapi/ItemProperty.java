package org.cristalise.restapi;

import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.property.Property;

@Path("/item/{uuid}/property")
public class ItemProperty extends ItemUtils {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listProperties(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie) {
		checkAuth(authCookie);
		try {
			return toJSON(getPropertySummary(getProxy(uuid)));
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(404);
		} 
	}
	
	@GET
	@Path("{name}")
	@Produces(MediaType.TEXT_PLAIN)
	public String getProperty(@PathParam("uuid") String uuid, @PathParam("name") String name, 
			@CookieParam(COOKIENAME) Cookie authCookie) {
		checkAuth(authCookie);
		try {
			return getProxy(uuid).getProperty(name);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(404);
		}
	}
	
	@GET
	@Path("{name}/details")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPropertyDetails(@PathParam("uuid") String uuid, @PathParam("name") String name, 
			@CookieParam(COOKIENAME) Cookie authCookie) {
		checkAuth(authCookie);
		LinkedHashMap<String, Object> propDetails = new LinkedHashMap<String, Object>();
		try {
			Property prop = (Property)getProxy(uuid).getObject(ClusterStorage.PROPERTY+"/"+name);
			propDetails.put("name", prop.getName());
			propDetails.put("value", prop.getValue());
			propDetails.put("readOnly", !prop.isMutable());
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(404);
		}
		return toJSON(propDetails);
	}
}
