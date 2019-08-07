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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

@Path("/")
public class RootAccess extends RestHandler {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listResourceRoots(
            @CookieParam(COOKIENAME)  Cookie authCookie,
            @Context                  UriInfo uri)
    {
        AuthData authData = checkAuthCookie( authCookie );
        ArrayList<Map<String, Object>> resourceRoots = new ArrayList<>();

        resourceRoots.add(getRootData(uri, "domain"));
        resourceRoots.add(getRootData(uri, "role"));
        resourceRoots.add(getRootData(uri, "schema"));
        resourceRoots.add(getRootData(uri, "query"));
        resourceRoots.add(getRootData(uri, "stateMachine"));
        resourceRoots.add(getRootData(uri, "compositeActDesc"));
        resourceRoots.add(getRootData(uri, "elementaryActDesc"));
        resourceRoots.add(getRootData(uri, "script"));
//        resourceRoots.add(getRootData(uri, "item"));
//        resourceRoots.add(getRootData(uri, "agent"));

        try {
            return toJSON(resourceRoots).cookie(checkAndCreateNewCookie( authData )).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authData )).build();
        }
    }

    private Map<String, Object> getRootData(UriInfo uri, String name) {
        Map<String, Object> resourceData = new HashMap<>();

        resourceData.put("resource", name);
        resourceData.put("url", uri.getBaseUriBuilder().path(name).build());

        return resourceData;
    }
}
