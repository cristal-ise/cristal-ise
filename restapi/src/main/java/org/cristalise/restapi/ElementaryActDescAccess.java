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

import static org.cristalise.kernel.process.resource.BuiltInResources.ELEM_ACT_DESC_RESOURCE;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;

import org.cristalise.kernel.process.Gateway;

@Path("/elementaryActDesc")
public class ElementaryActDescAccess extends ResourceAccess {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllElementaryActDescs(
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @CookieParam(COOKIENAME)                Cookie  authCookie,
            @Context                                UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);

        if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.DefaultBatchSize", 75);

        try {
            return listAllResources(ELEM_ACT_DESC_RESOURCE, uri, start ,batchSize).cookie(checkAndCreateNewCookie( authData )).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authData )).build();
        }
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listElementaryActDescVersions(
            @PathParam("name")        String  name,
            @CookieParam(COOKIENAME)  Cookie  authCookie,
            @Context                  UriInfo uri)
    {
        try {
            return listResourceVersions(ELEM_ACT_DESC_RESOURCE, name, uri).cookie(checkAndCreateNewCookie( authCookie )).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authCookie )).build();
        }
    }

    @GET
    @Path("{name}/{version}")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getElementaryActDesc(
            @Context                 HttpHeaders headers,
            @PathParam("name")       String      name, 
            @PathParam("version")    Integer     version, 
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        try {
            return getResource(ELEM_ACT_DESC_RESOURCE, name, version, produceJSON(headers.getAcceptableMediaTypes()))
                    .cookie(checkAndCreateNewCookie( authCookie )).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(checkAndCreateNewCookie( authCookie )).build();
        }
    }
}
