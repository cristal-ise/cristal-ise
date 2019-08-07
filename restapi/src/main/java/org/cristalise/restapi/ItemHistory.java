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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

import static org.cristalise.kernel.persistency.ClusterType.*;

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
        AuthData authData = checkAuthCookie(authCookie);

        ItemProxy item;
        try {
            item = getProxy(uuid);
        } catch (InvalidItemPathException | ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e)
                        .newCookie(checkAndCreateNewCookie( authData )).build();
        }

        if (start == null) start = 0;
        descending = descending != null;

        if (batchSize == null) {
            batchSize = Gateway.getProperties().getInt("REST.Event.DefaultBatchSize", Gateway.getProperties().getInt("REST.DefaultBatchSize", 20));
        }

        // fetch this batch of events from the RemoteMap
        LinkedHashMap<String, Object> batch;
        try {
            batch = RemoteMapAccess.list(item, HISTORY, start, batchSize, descending, uri);
        } catch (ClassCastException | ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e)
                    .newCookie(checkAndCreateNewCookie( authData )).build();
        }

        ArrayList<LinkedHashMap<String, Object>> events = new ArrayList<>();

        // replace Events with their JSON form. Leave any other object (like the nextBatch URI) as they are
        for (String key : batch.keySet()) {
            Object obj = batch.get(key);
            if (obj instanceof Event) {
                events.add(makeEventData((Event) obj, uri));
            }
        }

        try {
            return toJSON(events).cookie(checkAndCreateNewCookie( authData )).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authData )).build();
        }
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
        AuthData authData = checkAuthCookie(authCookie);

        try {
            ItemProxy item = getProxy(uuid);
            Event ev = (Event) RemoteMapAccess.get(item, HISTORY, eventId);

            return toJSON(makeEventData(ev, uri)).cookie(checkAndCreateNewCookie( authData )).build();
        } catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authData )).build();
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
    private Response.ResponseBuilder getEventOutcome(String uuid, String eventId, UriInfo uri, boolean json)
            throws ObjectNotFoundException, InvalidItemPathException, ClassCastException, Exception {
        ItemProxy item = getProxy(uuid);
        Event ev = (Event) RemoteMapAccess.get(item, HISTORY, eventId);


        if (ev.getSchemaName() == null || ev.getSchemaName().equals("")) {
            throw new ObjectNotFoundException( "This event has no data" );
        }

        try {
            Outcome oc = (Outcome) item.getObject(OUTCOME+"/"+ev.getSchemaName()+"/"+ev.getSchemaVersion()+"/"+ev.getID());
            return getOutcomeResponse(oc, ev, json);
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw new ObjectNotFoundException( "Referenced data not" );
        } catch ( Exception e ) {
            throw e;
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
        AuthData authData = checkAuthCookie(authCookie);
        try {
            return getEventOutcome(uuid, eventId, uri, produceJSON(headers.getAcceptableMediaTypes()))
                    .cookie(checkAndCreateNewCookie( authData )).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authData )).build();
        }
    }
}
