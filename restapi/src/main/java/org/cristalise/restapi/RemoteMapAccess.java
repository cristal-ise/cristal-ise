package org.cristalise.restapi;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.utils.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;

public class RemoteMapAccess extends ItemUtils {

	public LinkedHashMap<String, Object> list(ItemProxy item, String root, int start, int batchSize, UriInfo uri) {
		RemoteMap<?> map;
		try {
			map = (RemoteMap<?>) item.getObject(root);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Could not access item history");
		} catch (ClassCastException e) {
			throw ItemUtils.createWebAppException("Object was not a RemoteMap: "+root, Response.Status.BAD_REQUEST);
		}

		LinkedHashMap<String, Object> batch = new LinkedHashMap<String, Object>();
		int i = start;
		int last = map.getLastId();
		while (batch.size() < batchSize && i <= last) {
			Object obj = map.get(i);
			if (obj != null) batch.put(String.valueOf(i), obj);
			i++;
		}
		if (i < last) {
			while (map.get(i) == null) i++;
			batch.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", i).replaceQueryParam("batch", batchSize).build());
		}
		
		return batch;
	}
	
	public C2KLocalObject get(ItemProxy item, String root, String id) {
		RemoteMap<?> map;
		try {
			map = (RemoteMap<?>) item.getObject(root);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
		} catch (ClassCastException e) {
			throw ItemUtils.createWebAppException("Object was not a RemoteMap: "+root, Response.Status.BAD_REQUEST);
		}
		if (id.equals("last")) id = String.valueOf(map.getLastId());

		if(map.containsKey(id)) return map.get(id);
		else 			        throw ItemUtils.createWebAppException("Object was not found in "+root+" id:"+id, Response.Status.NOT_FOUND);
	}
}
