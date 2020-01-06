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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.process.Gateway;
import org.json.JSONArray;

import lombok.extern.slf4j.Slf4j;

@Path("/domain") @Slf4j
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
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        DomainPath domPath = new DomainPath(path);

        if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Path.DefaultBatchSize",
                Gateway.getProperties().getInt("REST.DefaultBatchSize", 75));

        if (path.equals("aliases") && search != null && search.startsWith("[") && search.endsWith("]")) {
            return getAliases(search, cookie).build();
        }
        else {
            // Return 404 if the domain path doesn't exist
            if (!domPath.exists()) {
                throw new WebAppExceptionBuilder().message("Domain path does not exist")
                        .status(Response.Status.NOT_FOUND).newCookie(cookie).build();
            }

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

            return toJSON(getPagedResult(uri, start, batchSize, childSearch.maxRows, pathDataArray), cookie).build();
        }
    }

    private Response.ResponseBuilder getAliases(String uuids, NewCookie cookie) {
        log.info("getAliases() - uuids:{}", uuids);

        JSONArray uuidsArray = new JSONArray(uuids);
        ArrayList<Object> returnVal = new ArrayList<>();

        for (int i = 0; i < uuidsArray.length(); i++) {
            String uuid = uuidsArray.getString(i);
            try {
                Map<String, Object> itemAliases = makeItemDomainPathsData(Gateway.getLookup().getItemPath(uuid));
                returnVal.add(itemAliases);
            }
            catch (InvalidItemPathException | ObjectNotFoundException e) {
                //Logger.error(e);

                Map<String, Object> error = new LinkedHashMap<String, Object>();
                error.put("uuid", uuid);
                error.put("error", e.getMessage());
                returnVal.add(error);
            }
        }

        return toJSON(returnVal, cookie);
    }
}
