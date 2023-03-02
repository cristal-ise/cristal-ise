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

import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.restapi.SystemProperties.REST_DefaultBatchSize;
import static org.cristalise.restapi.SystemProperties.REST_Event_DefaultBatchSize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.persistency.outcome.Outcome;

@Path("/item/{uuid}/history")
public class ItemHistory extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listEvents(
            @PathParam("uuid")        String  uuid,
            @QueryParam("start")      Integer start,
            @QueryParam("batch")      Integer batchSize,
            @QueryParam("descending") Boolean descending,
            @CookieParam(COOKIENAME)  Cookie  authCookie,
            @Context                  UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        if (start == null) start = 0;
        descending = descending != null;

        if (batchSize == null) {
            batchSize = REST_Event_DefaultBatchSize.getInteger(REST_DefaultBatchSize.getInteger());
        }

        // fetch this batch of events from the RemoteMap
        Map<Integer, Event> batch;
        try {
            batch = item.getHistory().list(start, batchSize, descending);
        } 
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }

        ArrayList<LinkedHashMap<String, Object>> events = new ArrayList<>();

        // replace Events with their JSON form. Leave any other object (like the nextBatch URI) as they are
        for (Integer key : batch.keySet()) {
            events.add(makeEventData(batch.get(key), uri));
        }

        return toJSON(events, cookie).build();
    }

    @GET
    @Path("{eventId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvent(
            @PathParam("uuid")       String  uuid,
            @PathParam("eventId")    String  eventId,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            Event ev = item.getEvent(Integer.parseInt(eventId));
            return toJSON(makeEventData(ev, uri), cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    /**
     * 
     * @param uuid
     * @param eventId
     * @param uri
     * @param json
     * @return
     */
    private Response.ResponseBuilder getEventOutcome(ItemProxy item, String eventId, UriInfo uri, boolean json, NewCookie cookie) {
        try {
            Event ev = item.getEvent(Integer.valueOf(eventId));

            if (ev.getSchemaName() == null || ev.getSchemaName().equals("")) {
                throw new ObjectNotFoundException( "This event has no data" );
            }
    
            Outcome oc = (Outcome) item.getObject(OUTCOME+"/"+ev.getSchemaName()+"/"+ev.getSchemaVersion()+"/"+ev.getID());
            return getOutcomeResponse(oc, ev, json, cookie);
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Path("{eventId}/data")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getEventOutcome(
            @Context                 HttpHeaders headers,
            @PathParam("uuid")       String      uuid,
            @PathParam("eventId")    String      eventId,
            @CookieParam(COOKIENAME) Cookie      authCookie,
            @Context                 UriInfo     uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        return getEventOutcome(item, eventId, uri, produceJSON(headers.getAcceptableMediaTypes()), cookie).build();
    }
}
