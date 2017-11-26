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

import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/schema")
public class SchemaAccess extends ResourceAccess {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllSchemas(@CookieParam(COOKIENAME) Cookie authCookie, @Context UriInfo uri) {
        checkAuthCookie(authCookie);
        return listAllResources(SCHEMA_RESOURCE, uri);
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSchemaVersions(@PathParam("name") String name, @CookieParam(COOKIENAME) Cookie authCookie, @Context UriInfo uri) {
        checkAuthCookie(authCookie);
        return listResourceVersions(SCHEMA_RESOURCE, name, uri);
    }

    @GET
    @Path("{name}/{version}")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getSchema(
            @Context                 HttpHeaders headers,
            @PathParam("name")       String      name, 
            @PathParam("version")    Integer     version, 
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        checkAuthCookie(authCookie);
        return getResource(SCHEMA_RESOURCE, name, version, produceJSON(headers.getAcceptableMediaTypes()));
    }
}
