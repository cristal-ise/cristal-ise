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

import static org.cristalise.kernel.process.resource.BuiltInResources.QUERY_RESOURCE;
import static org.cristalise.restapi.SystemProperties.REST_DefaultBatchSize;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;

import com.google.common.collect.ImmutableMap;

@Path("/query")
public class QueryAccess extends ResourceAccess {

    private QueryUtils queryUtils = new QueryUtils();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllQueries(
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @CookieParam(COOKIENAME)                Cookie  authCookie,
            @Context                                UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);
        NewCookie cookie = checkAndCreateNewCookie(authData);

        if (batchSize == null) batchSize = REST_DefaultBatchSize.getInteger();

        return listAllResources(QUERY_RESOURCE, uri, start, batchSize, cookie).build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listQueryVersions(
            @PathParam("name")        String  name, 
            @CookieParam(COOKIENAME)  Cookie  authCookie, 
            @Context                  UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);
        NewCookie cookie = checkAndCreateNewCookie(authData);

        return listResourceVersions(QUERY_RESOURCE, name, uri, cookie).build();
    }

    @GET
    @Path("{name}/{version}")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getQuery(
            @Context                 HttpHeaders headers,
            @PathParam("name")       String      name, 
            @PathParam("version")    Integer     version, 
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        AuthData authData = checkAuthCookie(authCookie);
        NewCookie cookie = checkAndCreateNewCookie(authData);

        return getResource(QUERY_RESOURCE, name, version, produceJSON(headers.getAcceptableMediaTypes()), cookie).build();
    }

    @GET
    @Path("queryResult")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getQueryResult(
            @Context                 HttpHeaders headers,
            @QueryParam("name" )     String      queryName,
            @QueryParam("version")   Integer     queryVersion,
            @QueryParam("inputs")    String      inputJson,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        AuthData authData = checkAuthCookie(authCookie);
        NewCookie cookie = checkAndCreateNewCookie(authData);

        return handleQueryExecution(headers, queryName, queryVersion, inputJson, cookie, authData.agent);
    }

    @POST
    @Path("queryResult")
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getQueryResultPost(
                                     String      postData,
            @Context                 HttpHeaders headers,
            @QueryParam("name")      String      queryName,
            @QueryParam("version")   Integer     queryVersion,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        AuthData authData = checkAuthCookie(authCookie);
        NewCookie cookie = checkAndCreateNewCookie(authData);

        return handleQueryExecution(headers, queryName, queryVersion, postData, cookie, authData.agent);
    }

    private Response handleQueryExecution(HttpHeaders headers, String queryName, Integer queryVersion, String inputJson, NewCookie cookie, AgentPath agentPath)  {
        try {
            AgentProxy agent = (AgentProxy) Gateway.getProxy(agentPath);
            Map<String, Object> additionalInputs = ImmutableMap.of(Script.PARAMETER_AGENT, agent);
            return queryUtils.executeQuery(headers, queryName, queryVersion, inputJson, additionalInputs).cookie(cookie).build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
        catch (Throwable t) {
            throw new WebAppExceptionBuilder().exception(new CriseVertxException(t)).newCookie(cookie).build();
        }
    }
}
