package org.cristalise.restapi;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public abstract class ItemUtils {
	
	ObjectMapper mapper;
	DateFormat dateFormatter;
	
	public ItemUtils() {
		mapper = new ObjectMapper();
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	protected static LinkedHashMap<String, String> getPropertySummary(ItemProxy item) throws ObjectNotFoundException {
		LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
		for (String propName : item.getContents(ClusterStorage.PROPERTY)) {
			if (!propName.equalsIgnoreCase("name"))
				props.put(propName, item.getProperty(propName));
		}
		return props;
	}	
	
	protected static ItemProxy getProxy(String uuid) throws WebApplicationException {
		ItemProxy item;
		ItemPath itemPath;
		try {
			itemPath = new ItemPath(uuid);
		} catch (InvalidItemPathException e) {
			Logger.error(e);
			throw new WebApplicationException(400); // Bad Request - the UUID wasn't valid
		}
		
		try {
			item = Gateway.getProxyManager().getProxy(itemPath);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException(404); // Not found - the path doesn't exist
		}
		return item;
	}
	
	public LinkedHashMap<String, URI> enumerate(ItemProxy item, String dataPath, String uriPath, UriInfo uri) {
		String[] children;
		try {
			children = Gateway.getStorage().getClusterContents(item.getPath(), dataPath);
		} catch (PersistencyException e) {
			Logger.error(e);
			throw new WebApplicationException("Database Error");
		}
		
		LinkedHashMap<String, URI> childrenWithLinks = new LinkedHashMap<>();
		for (String child : children) {
			childrenWithLinks.put(child, uri.getBaseUriBuilder().path("item").path(item.getPath().getUUID().toString()).
					path(uriPath).path(child).build());
		}
		
		return childrenWithLinks;
	}
	
	protected Response getOutcomeResponse(Outcome oc, Event ev) {
		Date eventDate;
		try {
			eventDate = dateFormatter.parse(ev.getTimeString());
		} catch (ParseException e) {
			Logger.error(e);
			throw new WebApplicationException("Invalid timestamp in event "+ev.getID()+": "+ev.getTimeString());
		}
		return Response.ok(oc.getData()).lastModified(eventDate).build();
	}
	
	public Response toJSON(LinkedHashMap<String, ?> map) {
		String childPathDataJSON;
		try {
			childPathDataJSON = mapper.writeValueAsString(map);
		} catch (IOException e) {
			Logger.error(e);
			throw new WebApplicationException("Problem building response JSON: "+e.getMessage());
		}
		return Response.ok(childPathDataJSON).build();
	}
}
