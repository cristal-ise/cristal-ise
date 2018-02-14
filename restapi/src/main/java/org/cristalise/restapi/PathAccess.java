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

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;

@Path("/domain")
public class PathAccess extends PathUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryPath(
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @QueryParam("search")                   String search,
            @CookieParam(COOKIENAME)                Cookie authCookie,
            @Context                                UriInfo uri)
    {
        return queryPath("/", start, batchSize, search, authCookie, uri);
    }

    @GET
    @Path("{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryPath(
            @PathParam("path")                      String path,
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @QueryParam("search")                   String search,
            @CookieParam(COOKIENAME)                Cookie authCookie,
            @Context                                UriInfo uri)
    {
        checkAuthCookie(authCookie);
        DomainPath domPath = new DomainPath(path);
        if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Path.DefaultBatchSize",
                Gateway.getProperties().getInt("REST.DefaultBatchSize", 75));

        // Return 404 if the domain path doesn't exist
        if (!domPath.exists())
            throw ItemUtils.createWebAppException("Domain path doesn't exis", Response.Status.NOT_FOUND);

        // If the domain path represents an item, redirect to it
        try {
            ItemPath item = domPath.getItemPath();
            return Response.seeOther(ItemUtils.getItemURI(uri, item)).build();
        }
        catch (ObjectNotFoundException ex) {} // not an item

        PagedResult childSearch;

        if (search == null) childSearch = Gateway.getLookup().getChildren(domPath, start, batchSize);
        else                childSearch = Gateway.getLookup().search(domPath, getPropertiesFromQParams(search), start, batchSize);

        ArrayList<Map<String, Object>> pathDataArray = new ArrayList<>();
        
        for (org.cristalise.kernel.lookup.Path p: childSearch.rows) {
            pathDataArray.add(makeLookupData(path, p, uri));
        }

        LinkedHashMap<String, Object> pagedReturnData = new LinkedHashMap<>();

        pagedReturnData.put("start", start);
        pagedReturnData.put("pageSize", batchSize);
        pagedReturnData.put("totalRows", childSearch.maxRows);

        if (start - batchSize >= 0) {
            pagedReturnData.put("prevPage", 
                    uri.getAbsolutePathBuilder()
                    .replaceQueryParam("start", start - batchSize)
                    .replaceQueryParam("batch", batchSize)
                    .build());
        }

        if (start + batchSize < childSearch.maxRows) {
            pagedReturnData.put("nextPage", 
                    uri.getAbsolutePathBuilder()
                    .replaceQueryParam("start", start + batchSize)
                    .replaceQueryParam("batch", batchSize)
                    .build());
        }

        pagedReturnData.put("rows", pathDataArray);

        return toJSON(pagedReturnData);
    }

    /**
     * Converts QueryParems to Item Properties
     * 
     * @param search the string to decompose in the format: name,prop:val,prop:val
     * @return the decoded list of Item Properties
     */
    private List<Property> getPropertiesFromQParams(String search) {
        String[] terms = search.split(",");

        List<Property> props = new ArrayList<>();

        for (int i = 0; i < terms.length; i++) {
            if (terms[i].contains(":")) { // assemble property if we have name:val
                String[] nameval = terms[i].split(":");

                if (nameval.length != 2)
                    throw ItemUtils.createWebAppException("Invalid search term: " + terms[i], Response.Status.BAD_REQUEST);

                props.add(new Property(nameval[0], nameval[1]));
            }
            else if (i == 0) { // first search term can imply Name if no propname given
                props.add(new Property(NAME, terms[i]));
            }
            else {
                throw ItemUtils.createWebAppException("Only the first search term may omit property name", Response.Status.BAD_REQUEST);
            }
        }
        return props;
    }
}
