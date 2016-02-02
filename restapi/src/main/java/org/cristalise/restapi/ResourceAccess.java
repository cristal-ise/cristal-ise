package org.cristalise.restapi;

import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;

public class ResourceAccess extends ItemUtils {

	public Response listAllResources(String typeName, UriInfo uri) {
		LinkedHashMap<String, String> resourceNameData = new LinkedHashMap<>();
		Iterator<org.cristalise.kernel.lookup.Path> iter =
				Gateway.getLookup().search(new DomainPath("/desc/"+typeName), new Property("Type", typeName));
		while (iter.hasNext()) {
			ItemPath schemaPath = (ItemPath)iter.next();
			try {
				ItemProxy proxy = Gateway.getProxyManager().getProxy(schemaPath);
				String name = proxy.getName();
				resourceNameData.put(name, uri.getAbsolutePathBuilder().path(name).build().toString());
			} catch (ObjectNotFoundException e) {
				resourceNameData.put(schemaPath.getUUID().toString(), "Name not found");
			}
		}
		return toJSON(resourceNameData);
	}
	
	public Response listResourceVersions(String typeName, String schemaName, String uriBase, String name, UriInfo uri) {
		Iterator<org.cristalise.kernel.lookup.Path> iter =
				Gateway.getLookup().search(new DomainPath("/desc/"+typeName), name);
		if (!iter.hasNext())
			throw new WebApplicationException(schemaName+" not found", 404);
		
		try {
			ItemProxy item = Gateway.getProxyManager().getProxy(iter.next());
			return toJSON(enumerate(item, ClusterStorage.VIEWPOINT+"/"+schemaName, "/"+uriBase+"/"+name, uri));
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(schemaName+" has no versions", 404);
		}
		
	}

	public Response getResource(String typeName, String schemaName, String name, Integer version) {
		Iterator<org.cristalise.kernel.lookup.Path> iter =
				Gateway.getLookup().search(new DomainPath("/desc/"+typeName), name);
		if (!iter.hasNext())
			throw new WebApplicationException(schemaName+" not found", 404);
		
		try {
			ItemProxy item = Gateway.getProxyManager().getProxy(iter.next());
			Viewpoint view = item.getViewpoint(schemaName, String.valueOf(version));
			return getOutcomeResponse(view.getOutcome(), view.getEvent(), false);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(schemaName+" v"+version+" does not exist", 404);
		} catch (PersistencyException e) {
			Logger.error(e);
			throw new WebApplicationException("Database error");
		} catch (InvalidDataException e) {
			throw new WebApplicationException(schemaName+" v"+version+" doesn't point to any data", 404);
		}
	}
}
