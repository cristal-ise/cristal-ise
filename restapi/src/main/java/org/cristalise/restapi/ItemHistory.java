package org.cristalise.restapi;

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

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

@Path("/item/{uuid}/history")
public class ItemHistory extends RemoteMapAccess {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response list(@PathParam("uuid") String uuid, @QueryParam("start") Integer start, 
			@QueryParam("batch") Integer batchSize,	@Context UriInfo uri) {

		ItemProxy item = getProxy(uuid);
		if (start == null) start = 0;
		if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Event.DefaultBatchSize", 
				Gateway.getProperties().getInt("REST.DefaultBatchSize", 50));
		
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
	public Response getEvent(@PathParam("uuid") String uuid, @PathParam("eventId") String eventId, @Context UriInfo uri) {
		ItemProxy item = getProxy(uuid);
		Event ev = (Event)get(item, ClusterStorage.HISTORY, eventId);
		return toJSON(makeEventData(ev, uri));
	}
	
	@GET
	@Path("{eventId}/data")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEventOutcome(@PathParam("uuid") String uuid, @PathParam("eventId") String eventId, @Context UriInfo uri) {
		ItemProxy item = getProxy(uuid);
		Event ev = (Event)get(item, ClusterStorage.HISTORY, eventId);
		if (ev.getSchemaName() == null || ev.getSchemaName().equals(""))
			throw new WebApplicationException("This event has no data", 404);
		Outcome oc;
		try {
			oc = (Outcome)item.getObject(ClusterStorage.OUTCOME+"/"+ev.getSchemaName()+"/"+ev.getSchemaVersion()+"/"+ev.getID());
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException("Referenced data not found");
		}
		return getOutcomeResponse(oc, ev);
	}
}
