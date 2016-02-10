package org.cristalise.restapi;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterStorage;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/item/{uuid}/collection")
public class ItemCollection extends ItemUtils {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCollections(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.COLLECTION, "collection", uri));
	}
	
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLastCollection(@PathParam("uuid") String uuid, @PathParam("name") String collName, 
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		try {
			return toJSON(makeCollectionData(item.getCollection(collName), uri));
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
		}
	}
	
	@GET
	@Path("{name}/version")
	public Response getCollectionVersions(@PathParam("uuid") String uuid, @PathParam("name") String collName, 
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.COLLECTION+"/"+collName, "collection/"+collName+"/version", uri));
	}
	
	@GET
	@Path("{name}/version/{version}")
	public Response getCollectionVersion(@PathParam("uuid") String uuid, @PathParam("name") String collName, 
			@PathParam("version") String collVersion, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		try {
			return toJSON(makeCollectionData(item.getCollection(collName, collVersion.equals("last")?null:Integer.valueOf(collVersion)), uri));
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
		} catch (NumberFormatException e) {
			throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
		}
	}
}
