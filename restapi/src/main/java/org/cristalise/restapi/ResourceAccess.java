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

import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;

public class ResourceAccess extends ItemUtils {

    public Response listAllResources(String typeName, UriInfo uri) {
        LinkedHashMap<String, String> resourceNameData = new LinkedHashMap<>();
        Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(new DomainPath("/desc/" + typeName), new Property("Type", typeName));

        while (iter.hasNext()) {
            Path p = iter.next();
            try {
                ItemProxy proxy = Gateway.getProxyManager().getProxy(p.getItemPath());
                String name = proxy.getName();
                resourceNameData.put(name, uri.getAbsolutePathBuilder().path(name).build().toString());
            }
            catch (ObjectNotFoundException e) {
                resourceNameData.put(p.getStringPath(), "Path not found");
            }
        }
        return toJSON(resourceNameData);
    }

    public Response listResourceVersions(String typeName, String schemaName, String uriBase, String name, UriInfo uri) {
        Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(new DomainPath("/desc/" + typeName), name);
        if (!iter.hasNext())
            throw ItemUtils.createWebAppException(schemaName + " not found", Response.Status.NOT_FOUND);

        try {
            ItemProxy item = Gateway.getProxyManager().getProxy(iter.next());
            return toJSON(enumerate(item, ClusterStorage.VIEWPOINT + "/" + schemaName, "/" + uriBase + "/" + name, uri));
        }
        catch (ObjectNotFoundException e) {
            throw ItemUtils.createWebAppException(schemaName + " has no versions", Response.Status.NOT_FOUND);
        }

    }

    public Response getResource(String typeName, String schemaName, String name, Integer version) {
        Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(new DomainPath("/desc/" + typeName), name);
        if (!iter.hasNext())
            throw ItemUtils.createWebAppException(schemaName + " not found", Response.Status.NOT_FOUND);

        try {
            ItemProxy item = Gateway.getProxyManager().getProxy(iter.next());
            Viewpoint view = item.getViewpoint(schemaName, String.valueOf(version));
            return getOutcomeResponse(view.getOutcome(), view.getEvent(), false);
        }
        catch (ObjectNotFoundException e) {
            throw ItemUtils.createWebAppException(schemaName + " v" + version + " does not exist", Response.Status.NOT_FOUND);
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Database error");
        }
        catch (InvalidDataException e) {
            throw ItemUtils.createWebAppException(schemaName + " v" + version + " doesn't point to any data", Response.Status.NOT_FOUND);
        }
    }
}
