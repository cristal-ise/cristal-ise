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

import static org.cristalise.kernel.process.resource.BuiltInResources.SCRIPT_RESOURCE;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;

import com.google.common.collect.ImmutableMap;

@Path("/script")
public class ScriptAccess extends ResourceAccess {

    private ScriptUtils scriptUtils = new ScriptUtils();
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllScripts(
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @CookieParam(COOKIENAME)                Cookie  authCookie,
            @Context                                UriInfo uri)
    {
        checkAuthCookie(authCookie);

        if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.DefaultBatchSize", 75);

        return listAllResources(SCRIPT_RESOURCE, uri, start, batchSize);
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listScriptVersions(
            @PathParam("name")       String  name,
            @CookieParam(COOKIENAME) Cookie  authCookie, 
            @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        return listResourceVersions(SCRIPT_RESOURCE, name, uri);
    }

    @GET
    @Path("{name}/{version}")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getScript(
            @Context                 HttpHeaders headers,
            @PathParam("name")       String      name, 
            @PathParam("version")    Integer     version, 
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        checkAuthCookie(authCookie);
        return getResource(SCRIPT_RESOURCE, name, version, produceJSON(headers.getAcceptableMediaTypes()));
    }
    
    @GET
    @Path("scriptResult")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getScriptResult(
            @Context                 HttpHeaders headers,
            @QueryParam("script")    String      scriptName,
            @QueryParam("version")   Integer     scriptVersion,
            @QueryParam("inputs")    String      inputJson,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        checkAuthCookie(authCookie);
        
        try (DSLContext context = JooqHandler.connect()) {
            return scriptUtils.executeScript(headers, null, scriptName, scriptVersion, inputJson,
            		ImmutableMap.of("dsl", context));
        } catch (DataAccessException | PersistencyException e) {
            throw ItemUtils.createWebAppException("Error connecting to database, please contact support", e, Response.Status.NOT_FOUND);
		}
    }

}
