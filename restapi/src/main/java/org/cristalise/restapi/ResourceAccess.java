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
import static org.cristalise.kernel.process.resource.BuiltInResources.COMP_ACT_DESC_RESOURCE;
import static org.cristalise.kernel.process.resource.BuiltInResources.ELEM_ACT_DESC_RESOURCE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.NotImplementedException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.json.XML;

public class ResourceAccess extends ItemUtils {

    public Response.ResponseBuilder listAllResources(BuiltInResources resource, UriInfo uri, int start, int batchSize)
        throws Exception
    {
        DomainPath searchRoot = new DomainPath(resource.getTypeRoot());
        ArrayList<Property> props = new ArrayList<>();

        if (resource == ELEM_ACT_DESC_RESOURCE) {
            props.add(new Property("Type", "ActivityDesc"));
            props.add(new Property("Complexity", "Elementary"));
        }
        else if (resource == COMP_ACT_DESC_RESOURCE) {
            props.add(new Property("Type", "ActivityDesc"));
            props.add(new Property("Complexity", "Composite"));
        }
        else {
            props.add(new Property("Type", resource.getSchemaName()));
        }

        PagedResult pr = Gateway.getLookup().search(searchRoot, props, start, batchSize);

        ArrayList<Map<String, String>> resourceArray = new ArrayList<>();

        for (Path p: pr.rows) {
            LinkedHashMap<String, String> resourceNameData = new LinkedHashMap<>();
            try {
                ItemProxy proxy = Gateway.getProxyManager().getProxy(p.getItemPath());
                String name = proxy.getName();
                resourceNameData.put("name", name );
                resourceNameData.put("url", uri.getAbsolutePathBuilder().path(name).build().toString());

                resourceArray.add(resourceNameData);
            }
            catch (ObjectNotFoundException e) {
                resourceNameData.put("name", "Path not found for name:"+p.getName());
            }
        }

        return toJSON(getPagedResult(uri, start, batchSize, pr.maxRows, resourceArray));
    }

    public Response.ResponseBuilder listResourceVersions(BuiltInResources resource, String name,
                                                         UriInfo uri) throws ObjectNotFoundException, Exception {
        String resourceTypeName = resource.getSchemaName();

        if (resource == ELEM_ACT_DESC_RESOURCE || resource == COMP_ACT_DESC_RESOURCE) resourceTypeName = "ActivityDesc";

        Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(new DomainPath("/desc/" + resourceTypeName), name);

        if (!iter.hasNext()) {
            throw new ObjectNotFoundException(resourceTypeName + " not found");
        }

        try {
            ItemProxy item = Gateway.getProxyManager().getProxy(iter.next());
            return toJSON(getResourceVersions(item, VIEWPOINT + "/" + resource.getSchemaName(), name, uri));
        }
        catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException(resource.getSchemaName() + " has no versions");
        } catch ( Exception e ) {
            throw e;
        }
    }

    public ArrayList<LinkedHashMap<String, Object>> getResourceVersions(ItemProxy item, String clusterPath,
                                                                        String name, UriInfo uri) throws Exception {
        try {
            String[] children = item.getContents(clusterPath);
            ArrayList<LinkedHashMap<String, Object>> childrenData = new ArrayList<>();

            for (String childName: children) {
                // exclude 'last' from result to contain versions only
                if (childName.equals("last")) continue;

                LinkedHashMap<String, Object> childData = new LinkedHashMap<>();

                childData.put("version", childName);
                childData.put("url", uri.getAbsolutePathBuilder().path(childName).build());

                childrenData.add(childData);
            }

            return childrenData;
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw new Exception("Database Error");
        }
    }

    public Response.ResponseBuilder getResource(BuiltInResources resource, String name, Integer version,
                                boolean json) throws ObjectNotFoundException, NotImplementedException, Exception {
        try {
            String result;
            switch (resource) {
                case SCHEMA_RESOURCE:
                    result = LocalObjectLoader.getSchema(name,version).getSchemaData(); 
                    break;
                case STATE_MACHINE_RESOURCE:
                    result = Gateway.getMarshaller().marshall(LocalObjectLoader.getStateMachine(name,version));
                    break;
                case SCRIPT_RESOURCE:
                    result = LocalObjectLoader.getScript(name,version).getScriptData();
                    break;
                case QUERY_RESOURCE:
                    result = LocalObjectLoader.getQuery(name,version).getQueryXML();
                    break;
                case ELEM_ACT_DESC_RESOURCE:
                    result = Gateway.getMarshaller().marshall(LocalObjectLoader.getElemActDef(name,version));
                    break;
                case COMP_ACT_DESC_RESOURCE:
                    result = Gateway.getMarshaller().marshall(LocalObjectLoader.getCompActDef(name,version));
                    break;
                default:
                    throw new NotImplementedException(resource.name()+" "+name+" v"+version+" not handled");
            }

            if(json) result = XML.toJSONObject(result, true).toString();

            return Response.ok(result);
        }
        catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException(resource.name()+" "+name+" v"+version+" does not exist");
        }
        catch (InvalidDataException e) {
            throw new ObjectNotFoundException(resource.name()+" "+name+" v"+version+" doesn't point to any data");
        }
        catch (MarshalException | ValidationException | IOException | MappingException e) {
            throw new Exception(resource.name()+" "+name+" v"+version+" xml convert problem");
        }
    }
}
