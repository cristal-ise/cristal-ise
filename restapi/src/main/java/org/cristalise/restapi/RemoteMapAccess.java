package org.cristalise.restapi;

import java.util.LinkedHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.utils.Logger;

public class RemoteMapAccess extends ItemUtils {

	public LinkedHashMap<String, Object> list(ItemProxy item, String root, int start, int batchSize, UriInfo uri) {
		RemoteMap<?> map;
		try {
			map = (RemoteMap<?>) item.getObject(root);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Could not access item history");
		} catch (ClassCastException e) {
			throw new WebApplicationException("Object was not a RemoteMap: "+root, 400);
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
			throw new WebApplicationException("Could not access item history");
		} catch (ClassCastException e) {
			throw new WebApplicationException("Object was not a RemoteMap: "+root, 400);
		}
		if (id.equals("last"))
			id = String.valueOf(map.getLastId());
		return map.get(id);
	}
}
