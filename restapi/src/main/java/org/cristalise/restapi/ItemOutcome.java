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

import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.entity.proxy.ItemProxy;

@Path("/item/{uuid}/outcome")
public class ItemOutcome extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOutcomeSchemas(@PathParam("uuid")       String uuid,
                                      @CookieParam(COOKIENAME) Cookie authCookie,
                                      @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        ItemProxy item = ItemRoot.getProxy(uuid);
        return toJSON(enumerate(item, OUTCOME, "outcome", uri));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}")
    public Response getOutcomeVersions(@PathParam("uuid")       String  uuid,
                                       @PathParam("schema")     String  schema,
                                       @CookieParam(COOKIENAME) Cookie  authCookie,
                                       @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        ItemProxy item = ItemRoot.getProxy(uuid);
        return toJSON(enumerate(item, OUTCOME + "/" + schema, "outcome/" + schema, uri));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{version}")
    public Response getOutcomeEvents(@PathParam("uuid")       String  uuid,
                                     @PathParam("schema")     String  schema,
                                     @PathParam("version")    Integer version,
                                     @CookieParam(COOKIENAME) Cookie  authCookie,
                                     @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        ItemProxy item = ItemRoot.getProxy(uuid);
        return toJSON(enumerate(item, OUTCOME+"/"+schema+"/"+version, "outcome/"+schema+"/"+version, uri));
    }

    @GET
    @Produces(MediaType.TEXT_XML)
    @Path("{schema}/{version}/{event}")
    public Response queryXMLData(@PathParam("uuid")       String  uuid,
                                 @PathParam("schema")     String  schema,
                                 @PathParam("version")    Integer version,
                                 @PathParam("event")      Integer event,
                                 @CookieParam(COOKIENAME) Cookie  authCookie,
                                 @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        return getOutcome(uuid, schema, version, event, false);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{version}/{event}")
    public Response queryJSONData(@PathParam("uuid")       String  uuid,
                                  @PathParam("schema")     String  schema,
                                  @PathParam("version")    Integer version,
                                  @PathParam("event")      Integer event,
                                  @CookieParam(COOKIENAME) Cookie  authCookie,
                                  @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        return getOutcome(uuid, schema, version, event, true);
    }
}
