package org.cristalise.restapi;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.utils.Logger;

@Path("item/{uuid}")
public class ItemAccess {

	public ItemAccess() {
		// TODO Auto-generated constructor stub
	}

	@GET
    @Produces(MediaType.TEXT_XML)
	public String queryData(@PathParam("uuid") String uuid,
			@Context UriInfo uri) {
		ItemProxy proxy;
		ItemPath itemPath;
		try {
			itemPath = new ItemPath(uuid);
		} catch (InvalidItemPathException e) {
			throw new WebApplicationException(404);
		}
		
		try {
			proxy = Gateway.getProxyManager().getProxy(itemPath);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(404);
		}
		
		String objectPath = uri.getRequestUri().getQuery();
		try {
			return proxy.queryData(objectPath);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(404);
		}
	}
	
	protected String error(String... msgs) {
		ErrorInfo error = new ErrorInfo();
		for (String msg : msgs) {
			error.addError(msg);
		}
		try {
			return Gateway.getMarshaller().marshall(error);
		} catch (Exception e) {
			Logger.error("Error marshalling error");
			Logger.error(e);
			return "<ErrorInfo fatal=\"true\"><Message>Error marshalling error. See REST server log.</Message></ErrorInfo>";
		}
	}
	
}
