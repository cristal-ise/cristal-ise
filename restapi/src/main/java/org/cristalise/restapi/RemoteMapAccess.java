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

import java.util.LinkedHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.RemoteMap;
import org.cristalise.kernel.utils.Logger;

/**
 * 
 *
 */
public class RemoteMapAccess {

    private RemoteMapAccess() {}

    public static LinkedHashMap<String, Object> list(ItemProxy item, ClusterType root, int start, int batchSize, UriInfo uri) {
        RemoteMap<?> map;
        try {
            map = (RemoteMap<?>) item.getObject(root);
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Could not access item history");
        }
        catch (ClassCastException e) {
            throw ItemUtils.createWebAppException("Object was not a RemoteMap: " + root, Response.Status.BAD_REQUEST);
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

    public static C2KLocalObject get(ItemProxy item, ClusterType root, String id) {
        RemoteMap<?> map;
        try {
            map = (RemoteMap<?>) item.getObject(root);
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
        }
        catch (ClassCastException e) {
            throw ItemUtils.createWebAppException("Object was not a RemoteMap: " + root, Response.Status.BAD_REQUEST);
        }

        if (id.equals("last")) id = String.valueOf(map.getLastId());

        if (map.containsKey(id)) return map.get(id);
        else throw ItemUtils.createWebAppException("Object was not found in " + root + " id:" + id, Response.Status.NOT_FOUND);
    }
}
