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
import javax.ws.rs.core.*;

import org.cristalise.kernel.entity.proxy.ItemProxy;

@Path("/item/{uuid}/outcome")
public class ItemOutcome extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOutcomeSchemas(@PathParam("uuid")       String uuid,
                                      @CookieParam(COOKIENAME) Cookie authCookie,
                                      @Context                 UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);

        try {
            ItemProxy item = getProxy(uuid);
            return toJSON(enumerate(item, OUTCOME, "outcome", uri))
                    .cookie(checkAndCreateNewCookie( authData )).build();
        } catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e)
                    .newCookie(checkAndCreateNewCookie( authData )).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}")
    public Response getOutcomeVersions(@PathParam("uuid")       String  uuid,
                                       @PathParam("schema")     String  schema,
                                       @CookieParam(COOKIENAME) Cookie  authCookie,
                                       @Context                 UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);

        try {
            ItemProxy item = getProxy(uuid);
            return toJSON(enumerate(item, OUTCOME + "/" + schema, "outcome/" + schema, uri))
                    .cookie(checkAndCreateNewCookie( authData )).build();
        } catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e)
                    .newCookie(checkAndCreateNewCookie( authData )).build();
        }
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
        AuthData authData = checkAuthCookie(authCookie);

        try {
            ItemProxy item = getProxy(uuid);
            return toJSON(
                    enumerate(item, OUTCOME+"/"+schema+"/"+version, "outcome/"+schema+"/"+version, uri)
            ).cookie(checkAndCreateNewCookie( authData )).build();
        } catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e)
                    .newCookie(checkAndCreateNewCookie( authData )).build();
        }
    }

    @GET
    @Produces( {MediaType.TEXT_PLAIN, MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Path("{schema}/{version}/{event}")
    public Response queryXMLData(@PathParam("uuid")       String  uuid,
                                 @PathParam("schema")     String  schema,
                                 @PathParam("version")    Integer version,
                                 @PathParam("event")      Integer event,
                                 @CookieParam(COOKIENAME) Cookie  authCookie,
                                 @Context                 UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);

        try {
            return getOutcome(uuid, schema, version, event, false)
                    .cookie( checkAndCreateNewCookie( authData ) ).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e)
                    .newCookie( checkAndCreateNewCookie( authData ) ).build();
        }
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
        AuthData authData = checkAuthCookie(authCookie);

        try {
            return getOutcome(uuid, schema, version, event, true)
                    .cookie( checkAndCreateNewCookie( authData ) ).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e)
                    .newCookie( checkAndCreateNewCookie( authData ) ).build();
        }
    }
}
