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

import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.DescriptionObject;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.json.XML;
import org.python.antlr.PythonParser.continue_stmt_return;

public class ResourceAccess extends ItemUtils {

    public Response listAllResources(BuiltInResources resource, UriInfo uri) {
        LinkedHashMap<String, String> resourceNameData = new LinkedHashMap<>();
        Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(
                new DomainPath("/desc/" + resource.getSchemaName()), new Property("Type", resource.getSchemaName()));

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

    public Response listResourceVersions(BuiltInResources resource, String name, UriInfo uri) {
        String schemaName = resource.getSchemaName();

        Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(
                new DomainPath("/desc/" + schemaName), name);
        
        if (!iter.hasNext()) throw ItemUtils.createWebAppException(schemaName + " not found", Response.Status.NOT_FOUND);

        try {
            ItemProxy item = Gateway.getProxyManager().getProxy(iter.next());
            return toJSON(getResourceVersions(item, VIEWPOINT + "/" + schemaName, name, uri));
        }
        catch (ObjectNotFoundException e) {
            throw ItemUtils.createWebAppException(schemaName + " has no versions", Response.Status.NOT_FOUND);
        }
    }

    public ArrayList<LinkedHashMap<String, Object>> getResourceVersions(ItemProxy item, String clusterPath, String name, UriInfo uri) {
        try {
            String[] children = item.getContents(clusterPath);
            ArrayList<LinkedHashMap<String, Object>> childrenData = new ArrayList<>();

            for (String childName: children) {
                // exclude 
                if (childName.equals("last")) continue;

                LinkedHashMap<String, Object> childData = new LinkedHashMap<>();

                childData.put("name", childName);
                childData.put("url", uri.getAbsolutePathBuilder().path(childName).build());

                childrenData.add(childData);
            }

            return childrenData;
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Database Error");
        }
    }


    public Response getResource(BuiltInResources resource, String name, Integer version, boolean json) {
        try {
            DescriptionObject obj;
            switch (resource) {
                case SCHEMA_RESOURCE:
                    obj = LocalObjectLoader.getSchema(name,version);
                    break;
                case STATE_MACHINE_RESOURCE:
                    obj = LocalObjectLoader.getStateMachine(name,version);
                    break;
                case SCRIPT_RESOURCE:
                    obj = LocalObjectLoader.getScript(name,version);
                    break;
                case QUERY_RESOURCE:
                    obj = LocalObjectLoader.getQuery(name,version);
                    break;
                case ELEM_ACT_DESC_RESOURCE:
                    obj = LocalObjectLoader.getElemActDef(name,version);
                    break;
                case COMP_ACT_DESC_RESOURCE:
                    obj = LocalObjectLoader.getCompActDef(name,version);
                    break;
                default:
                    throw ItemUtils.createWebAppException(resource.name()+" "+name+" v"+version+" not handled", Response.Status.NOT_IMPLEMENTED);
            }

            String result = Gateway.getMarshaller().marshall(obj);

            if(json) result = XML.toJSONObject(result).toString();

            return Response.ok(result).build();
        }
        catch (ObjectNotFoundException e) {
            throw ItemUtils.createWebAppException(resource.name()+" "+name+" v"+version+" does not exist", Response.Status.NOT_FOUND);
        }
        catch (InvalidDataException e) {
            throw ItemUtils.createWebAppException(resource.name()+" "+name+" v"+version+" doesn't point to any data", Response.Status.NOT_FOUND);
        }
        catch (MarshalException | ValidationException | IOException | MappingException e) {
            throw ItemUtils.createWebAppException(resource.name()+" "+name+" v"+version+" xml convert problem", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
