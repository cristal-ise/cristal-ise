package org.cristalise.restapi;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.LinkedHashMap;

@Path("/item/{uuid}/history")
public class ItemHistory extends RemoteMapAccess {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response list(@PathParam("uuid") String uuid, @QueryParam("start") Integer start, 
			@QueryParam("batch") Integer batchSize,	@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		if (start == null) start = 0;
		if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Event.DefaultBatchSize", 
				Gateway.getProperties().getInt("REST.DefaultBatchSize", 20));
		
		// fetch this batch of events from the RemoteMap
		LinkedHashMap<String, Object> events = super.list(item, ClusterStorage.HISTORY, start, batchSize, uri);
		
		// replace Events with their JSON form. Leave any other object (like the nextBatch URI) as they are
		for (String key : events.keySet()) {
			Object obj = events.get(key);
			if (obj instanceof Event) {
				events.put(key, makeEventData((Event)obj, uri));
			}
		}
		return toJSON(events);
	}
	
	@GET
	@Path("{eventId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvent(@PathParam("uuid") String uuid, @PathParam("eventId") String eventId, 
			@CookieParam(COOKIENAME) Cookie authCookie,	@Context UriInfo uri)
	{
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		Event ev = (Event)get(item, ClusterStorage.HISTORY, eventId);
		return toJSON(makeEventData(ev, uri));
	}

	/**
	 *
	 * @param uuid
	 * @param eventId
	 * @param authCookie
	 * @param uri
	 * @param json
     * @return
     */
	private Response getEventOutcome(String uuid, String eventId, Cookie authCookie, UriInfo uri, boolean json) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		Event ev = (Event)get(item, ClusterStorage.HISTORY, eventId);
		if (ev.getSchemaName() == null || ev.getSchemaName().equals(""))
			throw ItemUtils.createWebAppException("This event has no data", Response.Status.NOT_FOUND);
		Outcome oc;
		try {
			oc = (Outcome)item.getObject(ClusterStorage.OUTCOME+"/"+ev.getSchemaName()+"/"+ev.getSchemaVersion()+"/"+ev.getID());
		} catch (ObjectNotFoundException e) {
			throw ItemUtils.createWebAppException("Referenced data not found", Response.Status.NOT_FOUND);
		}
		return getOutcomeResponse(oc, ev, json);
	}

	@GET
	@Path("{eventId}/data")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEventJSONOutcome(@PathParam("uuid") 		 String uuid,
										@PathParam("eventId") 	 String eventId,
										@CookieParam(COOKIENAME) Cookie authCookie,
										@Context 				 UriInfo uri)
	{
		return getEventOutcome(uuid, eventId, authCookie, uri, true);
	}

	@GET
	@Path("{eventId}/data")
	@Produces(MediaType.TEXT_XML)
	public Response getEventXMLOutcome(@PathParam("uuid") 		String uuid,
									   @PathParam("eventId") 	String eventId,
									   @CookieParam(COOKIENAME) Cookie authCookie,
									   @Context UriInfo 		uri)
	{
		return getEventOutcome(uuid, eventId, authCookie, uri, false);
	}
}
