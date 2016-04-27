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
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterStorage;

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
			throw new WebApplicationException(404);
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
			throw new WebApplicationException(404);
		} catch (NumberFormatException e) {
			throw new WebApplicationException(404);
		}
	}
}
