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

import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.RemoteMap;

/**
 * 
 *
 */
public class RemoteMapAccess {

    private RemoteMapAccess() {}

    public static LinkedHashMap<String, Object> list(ItemProxy item, ClusterType root, int start,
                                                     int batchSize, Boolean descending, UriInfo uri)
            throws ObjectNotFoundException, ClassCastException
    {
        RemoteMap<?> map= (RemoteMap<?>) item.getObject(root);
        map.activate();

        LinkedHashMap<String, Object> batch = new LinkedHashMap<String, Object>();
        int last = map.getLastId();

        if (descending) {
            int i = last-start;

            while (i >= 0 && batch.size() < batchSize) {
                Object obj = map.get(i);
                if (obj != null) batch.put(String.valueOf(i), obj);
                i--;
            }

            //'nextBatch' is not returned to the client, check ItemHistory.listEvents()
            //while (i >= 0 && map.get(i) == null) i--;
            //if (i >= 0) {
            //    batch.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", i).replaceQueryParam("batch", batchSize).build());
            //}
        }
        else {
            int i = start;

            while (i <= last && batch.size() < batchSize) {
                Object obj = map.get(i);
                if (obj != null) batch.put(String.valueOf(i), obj);
                i++;
            }

            //'nextBatch' is not returned to the client, check ItemHistory.listEvents()
            //while (i <= last && map.get(i) == null) i++;
            //if (i <= last) {
            //    batch.put("nextBatch", uri.getAbsolutePathBuilder().replaceQueryParam("start", i).replaceQueryParam("batch", batchSize).build());
            //}
        }

        return batch;
    }

    public static C2KLocalObject get(ItemProxy item, ClusterType root, String id) throws ObjectNotFoundException, ClassCastException {
        RemoteMap<?> map = (RemoteMap<?>) item.getObject(root);

        if (id.equals("last")) id = String.valueOf(map.getLastId());

        if (map.containsKey(id)) {
            return map.get(id);
        } else {
            throw new ObjectNotFoundException("Object was not found in " + root + " id:" + id);
        }
    }
}
