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

import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;

@Path("/item/{uuid}/viewpoint")
public class ItemViewpoint extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchemas(@PathParam("uuid") String uuid,
                               @CookieParam(COOKIENAME) Cookie authCookie,
                               @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));

        return toJSON(enumerate(getProxy(uuid, cookie), VIEWPOINT, "viewpoint", uri, cookie), cookie).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}")
    public Response getViewNames(@PathParam("uuid") String uuid,
                                 @PathParam("schema") String schema,
                                 @CookieParam(COOKIENAME) Cookie authCookie,
                                 @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));

        return toJSON(enumerate(getProxy(uuid, cookie), VIEWPOINT + "/" + schema, "viewpoint/" + schema, uri, cookie), cookie).build();
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{schema}/{viewName}")
    public Response queryXMLData(@PathParam("uuid") String uuid,
                                 @PathParam("schema") String schema,
                                 @PathParam("viewName") String viewName,
                                 @CookieParam(COOKIENAME) Cookie authCookie,
                                 @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));

        return getViewpointOutcome(uuid, schema, viewName, false, cookie).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{viewName}")
    public Response queryJSONData(@PathParam("uuid") String uuid,
                                  @PathParam("schema") String schema,
                                  @PathParam("viewName") String viewName,
                                  @CookieParam(COOKIENAME) Cookie authCookie,
                                  @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));

        return getViewpointOutcome(uuid, schema, viewName, true, cookie).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{viewName}/event")
    public Response getViewEvent(@PathParam("uuid") String uuid,
                                 @PathParam("schema") String schema,
                                 @PathParam("viewName") String viewName,
                                 @CookieParam(COOKIENAME) Cookie authCookie,
                                 @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            Viewpoint view = item.getViewpoint(schema, viewName);
            Event ev = view.getEvent();
            return toJSON(makeEventData(ev, uri), cookie).build();
        }
        catch (ObjectNotFoundException | InvalidDataException | PersistencyException e) {
            throw new WebAppExceptionBuilder()
                .message("Database error loading view " + viewName + " of schema " + schema)
                .exception(e)
                .newCookie(cookie)
                .build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{viewName}/history")
    public Response getAllEventsForView(@PathParam("uuid") String uuid,
                                        @PathParam("schema") String schema,
                                        @PathParam("viewName") String viewName,
                                        @CookieParam(COOKIENAME) Cookie authCookie,
                                        @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        History history;
        try {
            history = (History) item.getObject(HISTORY);
            history.activate();

            LinkedHashMap<String, Object> eventList = new LinkedHashMap<String, Object>();
            for (int i = 0; i <= history.getLastId(); i++) {
                Event ev = history.get(i);
                if (schema.equals(ev.getSchemaName()) && viewName.equals(ev.getViewName())) {
                    String evId = String.valueOf(i);
                    LinkedHashMap<String, Object> eventDetails = new LinkedHashMap<String, Object>();
                    eventDetails.put("timestamp", ev.getTimeString());
                    eventDetails.put("data", uri.getAbsolutePathBuilder().path(evId).build());
                    eventList.put(evId, eventDetails);
                }
            }

            return toJSON(eventList, cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().message("Could not load History").exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{schema}/{viewName}/history/{event}")
    public Response getOutcomeForEvent(@Context           HttpHeaders headers,
                                       @PathParam("uuid") String uuid,
                                       @PathParam("schema") String schema,
                                       @PathParam("viewName") String viewName,
                                       @PathParam("event") Integer eventId,
                                       @CookieParam(COOKIENAME) Cookie authCookie,
                                       @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        Event ev;
        try {
            ev = (Event) item.getObject(HISTORY + "/" + eventId);
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().message("Event " + eventId + " was not found").exception(e).newCookie(cookie).build();
        }

        if (!schema.equals(ev.getSchemaName()) || !viewName.equals(ev.getViewName())) {
            throw new WebAppExceptionBuilder().message("Event does not belong to this data")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }

        try {
            Outcome oc = (Outcome) item.getObject(OUTCOME + "/" + schema + "/" + ev.getSchemaVersion() + "/" + eventId);
            // TODO: implement retrieving json media type as well
            return getOutcomeResponse(oc, ev, produceJSON(headers.getAcceptableMediaTypes()), cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().message("Outcome " + eventId + " was not found").exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{viewName}/history/{event}/event")
    public Response getOutcomeEvent(@Context           HttpHeaders headers,
                                    @PathParam("uuid") String uuid,
                                    @PathParam("schema") String schema,
                                    @PathParam("viewName") String viewName,
                                    @PathParam("event") Integer eventId,
                                    @CookieParam(COOKIENAME) Cookie authCookie,
                                    @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            Event ev = (Event) item.getObject(HISTORY + "/" + eventId);

            if (!schema.equals(ev.getSchemaName()) || !viewName.equals(ev.getViewName())) {
                throw new WebAppExceptionBuilder().message("Event does not belong to this data")
                        .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
            }

            return toJSON(makeEventData(ev, uri), cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().message("Event " + eventId + " was not found").exception(e).newCookie(cookie).build();
        }
    }
}
