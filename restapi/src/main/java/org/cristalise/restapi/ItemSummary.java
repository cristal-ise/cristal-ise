package org.cristalise.restapi;
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.utils.Logger;

@Path("item")
public class ItemSummary extends ItemUtils {

	
	public ItemSummary() {
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}")
	public Response getItemSummary(@PathParam("uuid") String uuid,
			@Context UriInfo uri) {
		ItemProxy item = getProxy(uuid);
		
		LinkedHashMap<String, Object> itemSummary = new LinkedHashMap<String, Object>();
		itemSummary.put("Name", item.getName());
		try {
			itemSummary.put("Properties", getPropertySummary(item));
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("No Properties found", 400);
		}
		
		itemSummary.put("Data", enumerate(item, ClusterStorage.VIEWPOINT, "data", uri));
		
		try {
			return Response.ok(mapper.writeValueAsString(itemSummary), "application/json").build();
		} catch (IOException e) {
			Logger.error(e);
			throw new WebApplicationException("Error building JSON object");
		}
	}
	

}
