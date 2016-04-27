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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.utils.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.LinkedHashMap;

@Path("/item/{uuid}/data")
public class ItemData extends ItemUtils {

	@GET
    @Produces(MediaType.APPLICATION_JSON)
	public Response getSchemas(@PathParam("uuid") String uuid,
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.VIEWPOINT, "data", uri));
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{schema}")
	public Response getViewNames(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.VIEWPOINT+"/"+schema, "data/"+schema, uri));
	}

    /**
     *
     * @param uuid
     * @param schema
     * @param viewName
     * @param authCookie
     * @param uri
     * @param json
     * @return
     */
	private Response queryData(String uuid, String schema, String viewName, Cookie authCookie, UriInfo uri, boolean json) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		Viewpoint view;
		try {
			view = item.getViewpoint(schema, viewName);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
			//throw ItemUtils.createWebAppException("Database error loading view "+viewName+" of schema "+schema, Response.Status.NOT_FOUND);
		}
		Outcome oc;
		try {
			oc = view.getOutcome();
		} catch (ObjectNotFoundException | PersistencyException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Database error loading outcome for view "+viewName+" of schema "+schema);
		}

		try {
			return getOutcomeResponse(oc, view.getEvent(), json);
		}
        catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Database error loading event data for view "+viewName+" of schema "+schema);
		}
	}

	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("{schema}/{viewName}")
	public Response queryXMLData(@PathParam("uuid")       String  uuid,
							     @PathParam("schema")     String  schema,
							     @PathParam("viewName")   String  viewName,
							     @CookieParam(COOKIENAME) Cookie  authCookie,
							     @Context                 UriInfo uri)
	{
		return queryData(uuid, schema, viewName, authCookie, uri, false);
	}

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{viewName}")
    public Response queryJSONData(@PathParam("uuid")        String uuid,
                                  @PathParam("schema")      String schema,
                                  @PathParam("viewName")    String viewName,
                                  @CookieParam(COOKIENAME)  Cookie authCookie,
                                  @Context                  UriInfo uri)
    {
        return queryData(uuid, schema, viewName, authCookie, uri, true);
    }

    @GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{schema}/{viewName}/event")
	public Response getViewEvent(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		Viewpoint view;
		try {
			view = item.getViewpoint(schema, viewName);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Database error loading view "+viewName+" of schema "+schema);
		}
		Event ev;
		try {
			ev = view.getEvent();
		} catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Database error loading event data for view "+viewName+" of schema "+schema);
		}
		
		return toJSON(makeEventData(ev, uri));
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{schema}/{viewName}/history")
	public Response getAllEventsForView(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		History history;
		try {
			history = (History)item.getObject(ClusterStorage.HISTORY);
			history.activate();
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException("Could not load History");
		}
		LinkedHashMap<String, Object> eventList = new LinkedHashMap<String, Object>();
		for (int i=0; i<=history.getLastId(); i++) {
			Event ev = history.get(i);
			if (schema.equals(ev.getSchemaName()) && viewName.equals(ev.getViewName())) {
				String evId = String.valueOf(i);
				LinkedHashMap<String, Object> eventDetails = new LinkedHashMap<String, Object>();
				eventDetails.put("timestamp", ev.getTimeString());
				eventDetails.put("data", uri.getAbsolutePathBuilder().path(evId).build());
				eventList.put(evId, eventDetails);
			}
		}
		return toJSON(eventList);
	}
	
	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("{schema}/{viewName}/history/{event}")
	public Response getOutcomeForEvent(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@PathParam("event") Integer eventId,
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		Event ev;
		try {
			ev = (Event)item.getObject(ClusterStorage.HISTORY+"/"+eventId);
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException("Event "+eventId+" was not found", Response.Status.NOT_FOUND);
		}
		if (!schema.equals(ev.getSchemaName()) || !viewName.equals(ev.getViewName())) {
			throw ItemUtils.createWebAppException("Event does not belong to this data", Response.Status.BAD_REQUEST);
		}
		Outcome oc;
		try {
			oc = (Outcome)item.getObject(ClusterStorage.OUTCOME+"/"+schema+"/"+ev.getSchemaVersion()+"/"+eventId);
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException("Outcome "+eventId+" was not found", Response.Status.NOT_FOUND);
		}
		//TODO: implement retrieving json media type as well
		return getOutcomeResponse(oc, ev, false);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{schema}/{viewName}/history/{event}/event")
	public Response getOutcomeEvent(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@PathParam("event") Integer eventId,
			@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
		ItemProxy item = ItemRoot.getProxy(uuid);
		Event ev;
		try {
			ev = (Event)item.getObject(ClusterStorage.HISTORY+"/"+eventId);
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException("Event "+eventId+" was not found", Response.Status.NOT_FOUND);
		}
		if (!schema.equals(ev.getSchemaName()) || !viewName.equals(ev.getViewName())) {
			throw ItemUtils.createWebAppException("Event does not belong to this data", Response.Status.BAD_REQUEST);
		}
		return toJSON(makeEventData(ev, uri));
	}
}
