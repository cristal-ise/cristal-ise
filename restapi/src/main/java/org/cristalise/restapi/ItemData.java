package org.cristalise.restapi;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.utils.Logger;

@Path("item")
public class ItemData extends ItemUtils {

	DateFormat dateFormatter;
	public ItemData() {
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	@GET
    @Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}/data")
	public Response getSchemas(@PathParam("uuid") String uuid,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.VIEWPOINT, "data", uri));
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}/data/{schema}")
	public Response getViewNames(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		return toJSON(enumerate(item, ClusterStorage.VIEWPOINT+"/"+schema, "data/"+schema, uri));
	}
	
	@GET
	@Produces(MediaType.TEXT_XML)
	@Path("{uuid}/data/{schema}/{viewName}")
	public Response queryData(@PathParam("uuid") String uuid,
			@PathParam("schema") String schema,
			@PathParam("viewName") String viewName,
			@Context UriInfo uri) {
		ItemProxy item = ItemSummary.getProxy(uuid);
		Viewpoint view;
		try {
			view = item.getViewpoint(schema, viewName);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading view "+viewName+" of schema "+schema);
		}
		Outcome oc;
		try {
			oc = view.getOutcome();
		} catch (ObjectNotFoundException | PersistencyException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading outcome for view "+viewName+" of schema "+schema);
		}
		Event ev;
		try {
			ev = view.getEvent();
		} catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error loading event data for view "+viewName+" of schema "+schema);
		}
		Date eventDate;
		try {
			eventDate = dateFormatter.parse(ev.getTimeString());
		} catch (ParseException e) {
			Logger.error(e);
			throw new WebApplicationException("Invalid timestamp in event for view "+viewName+" of schema "+schema+": "+ev.getTimeString());
		}
		return Response.ok(oc.getData()).lastModified(eventDate).build();
		
	}
}
