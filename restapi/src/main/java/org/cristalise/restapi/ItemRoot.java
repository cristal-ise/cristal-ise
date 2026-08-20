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

import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.json.XML;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

@Path("/item/{uuid}") @Slf4j
public class ItemRoot extends ItemUtils {

    private final ScriptUtils scriptUtils = new ScriptUtils();
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemSummary(
            @PathParam("uuid")       String  uuid,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri) throws Exception
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        //Add name, and domainPaths
        Map<String, Object> itemSummary = makeItemDomainPathsData(item.getPath());

        itemSummary.put("uuid", uuid);
        itemSummary.put("hasMasterOutcome", false);
        itemSummary.put("isAgent", item.getPath() instanceof AgentPath);

        try {
            String type = item.getType();
            if (type != null) {
                itemSummary.put("type", type);

                if (getAggregateScript(item) != null || item.checkViewpoint(type, "last")) {
                    itemSummary.put("hasMasterOutcome", true);
                    itemSummary.put("master", getItemURI(uri, item, "master"));
                }
            }

            itemSummary.put("properties", getPropertySummary(item));

            itemSummary.put("viewpoints",  getAllViewpoints(item, uri, cookie));
            itemSummary.put("collections", enumerate(item, COLLECTION, "collection", uri, cookie));
            itemSummary.put("workflow",    getItemURI(uri, item, "workflow"));
            itemSummary.put("history",     getItemURI(uri, item, "history"));
            itemSummary.put("outcome",     getItemURI(uri, item, "outcome"));
            itemSummary.put("attachment",  getItemURI(uri, item, "attachment"));
            itemSummary.put("job",         getItemURI(uri, item, "job"));
            if (item.getPath() instanceof AgentPath) {
                itemSummary.put("roles", getItemURI(uri, item, "roles"));
            }

            return toJSON(itemSummary, cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().message("No Properties found")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @POST
    @Consumes( {MediaType.TEXT_PLAIN, MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    @Path("{activityPath: .*}")
    public String requestTransition(    String      outcome, //body of the post
                                        @Context                    HttpHeaders headers,
                                        @PathParam("uuid")          String      uuid,
                                        @PathParam("activityPath")  String      actPath,
                                        @QueryParam("transition")   String      transition,
                                        @CookieParam(COOKIENAME)    Cookie      authCookie,
                                        @Context                    UriInfo     uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        if (actPath == null) {
            throw new WebAppExceptionBuilder().message("Must specify activity path").status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }

        log.info("requestTransition() - {}://{}:{}", item, actPath, transition);

        try {
            String contentType = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).getFirst();

            log.debug("requestTransition() outcome:'{}' contentType:'{}'", outcome, contentType);

            AgentProxy agent = Gateway.getAgentProxy(getAgentPath(authCookie));
            String executeResult;

            if (actPath.startsWith(PREDEFINED_PATH)) {
                executeResult = executePredefinedStep(item, outcome, contentType, actPath, agent);
            }
            else {
                transition = extractAndCheckTransitionName(transition, uri);
                executeResult = executeJob(item, outcome, contentType, actPath, transition, agent);
            }

            if (produceJSON(headers.getAcceptableMediaTypes())) return XML.toJSONObject(executeResult, true).toString();
            else                                                return executeResult;
        }
        catch (Exception e) {
            log.error("requestTransition() - could not execute {}://{}:{}'", item, actPath, transition, e);
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
        catch (Throwable t) {
            log.error("requestTransition() - could not execute {}://{}:{}'", item, actPath, transition, t);
            throw new WebAppExceptionBuilder().exception(new CriseVertxException(t)).newCookie(cookie).build();
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    @Path("{binaryUploadPath: .*}")
    public String requestBinaryTransition( FormDataMultiPart  body,
                                           @Context                       HttpHeaders        headers,
                                           @PathParam("uuid")             String             uuid,
                                           @PathParam("binaryUploadPath") String             actPath,
                                           @QueryParam("transition")      String             transition,
                                           @CookieParam(COOKIENAME)       Cookie             authCookie,
                                           @Context                       UriInfo            uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        log.info("requestBinaryTransition() - {}://{}:{}", item, actPath, transition);

        if (actPath == null) {
            throw new WebAppExceptionBuilder().message("Must specify activity path")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }

        String outcome = null;

        try {
            AgentProxy agent = Gateway.getAgentProxy(getAgentPath(authCookie));
            String executeResult;

            FormDataBodyPart outcomeBodyPart = body.getField("outcome");
            String outcomeType = MediaType.APPLICATION_JSON;

            if (outcomeBodyPart != null) {
                outcome = outcomeBodyPart.getValue();

                // Multipart only works with text/plain so outcome string needs to be inspected for media type
                if (outcome.startsWith("<")) outcomeType = MediaType.APPLICATION_XML;

                log.debug("requestBinaryTransition() - outcome:'{}' contentType:'{}'", outcome, outcomeType);
            }

            FormDataBodyPart fileBodyPart = body.getField("file");
            InputStream file = null;
            String fileName = null;

            if (fileBodyPart != null) {
                file = fileBodyPart.getValueAs(InputStream.class);
                fileName = fileBodyPart.getContentDisposition().getFileName();

                log.debug("requestBinaryTransition() - attachment fileName:'{}'", fileName);
            }

            if (actPath.startsWith(PREDEFINED_PATH)) {
                if (file != null) {
                    throw new WebAppExceptionBuilder().message("PredefinedStep '"+actPath+"' cannot have attahcment")
                            .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
                }

                //This will execute 
                executeResult = executePredefinedStep(item, outcome, outcomeType, actPath, agent);
            }
            else {
                transition = extractAndCheckTransitionName(transition, uri);
                executeResult = executeJob(item, outcome, outcomeType, file, fileName, actPath, transition, agent);
            }

            if (produceJSON(headers.getAcceptableMediaTypes())) return XML.toJSONObject(executeResult, true).toString();
            else                                                return executeResult;
        }
        catch (Exception e) {
            log.error("requestBinaryTransition() - could not execute {}://{}:{}'", item, actPath, transition, e);
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
        catch (Throwable t) {
            log.error("requestTransition() - could not execute {}://{}:{}'", item, actPath, transition, t);
            throw new WebAppExceptionBuilder().exception(new CriseVertxException(t)).newCookie(cookie).build();
        }
    }

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(
            @PathParam("uuid")       String uuid,
            @CookieParam(COOKIENAME) Cookie authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        String name = getItemName(new ItemPath(UUID.fromString(uuid)));

        if (StringUtils.isBlank(name)) {
            throw new WebAppExceptionBuilder()
                    .message("Cannot resolve UUID")
                    .status(Response.Status.NOT_FOUND)
                    .newCookie(cookie)
                    .build();
        }

        return name;
    }

    @GET
    @Path("aliases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAliases(
            @PathParam("uuid")       String uuid,
            @CookieParam(COOKIENAME) Cookie authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));

        //Add name, and domainPathes
        Map<String, Object> itemAliases = makeItemDomainPathsData(new ItemPath(UUID.fromString(uuid)));

        if (StringUtils.isBlank((String)itemAliases.get("name"))) {
            throw new WebAppExceptionBuilder()
                    .message("Cannot resolve UUID")
                    .status(Response.Status.NOT_FOUND)
                    .newCookie(cookie).build();
        }

        return toJSON(itemAliases, cookie).build();
    }

    @GET
    @Path("master")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getMasterOutcome(
            @Context                     HttpHeaders headers,
            @PathParam("uuid")           String      uuid,
            @QueryParam("schema")        String      schemaName,
            @QueryParam("schemaVersion") Integer     schemaVersion,
            @QueryParam("script")        String      scriptName,
            @QueryParam("scriptVersion") Integer     scriptVersion,
            @CookieParam(COOKIENAME)     Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            Schema masterSchema = item.getMasterSchema(schemaName, schemaVersion);
            Script aggrScript   = getAggregateScript(item, scriptName, scriptVersion);

            boolean jsonFlag = produceJSON(headers.getAcceptableMediaTypes());

            if (aggrScript != null) {
                return scriptUtils.returnScriptResult(item, masterSchema, aggrScript, new CastorHashMap(), jsonFlag)
                        .cookie(cookie).build();
            }
            else if (item.checkViewpoint(item.getType(), "last")) {
                return getViewpointOutcome(uuid, item.getType(), "last", true, cookie).build();
            }
            else {
                throw new WebAppExceptionBuilder()
                        .message("No method available to retrieve MasterOutcome")
                        .status(Response.Status.NOT_FOUND)
                        .newCookie(cookie).build();
            }
        }
        catch (ObjectNotFoundException | InvalidDataException | ScriptingEngineException e) {
            throw new WebAppExceptionBuilder()
                .message("Error retrieving MasterOutcome")
                .status(Response.Status.NOT_FOUND)
                .newCookie(cookie).build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    /**
     * Retrieve the default Aggregate Script of the Item if exists.
     * 
     * @param item to be checked
     * @return returns the Script or null
     */
    private Script getAggregateScript(ItemProxy item) {
        return getAggregateScript(item, null, null);
    }

    /**
     * Retrieve the the named version of Aggregate Script of the Item if exists.
     * 
     * @param item to be checked
     * @param name the name or UUID of he Script
     * @param version version of the Script
     * @return returns the Script or null
     */
    private Script getAggregateScript(ItemProxy item, String name, Integer version) {
        if ("Factory".equals(item.getType())) {
            log.debug("getAggregateScript() - Return null for Factory item:{}", item);
            return null;
        }

        try {
            return item.getAggregateScript(name, version);
        }
        catch (InvalidDataException | ObjectNotFoundException e) {
            log.trace("getAggregateScript() - Return null for item:{}", item, e);
            return null;
        }
    }

    @GET
    @Path("scriptResult")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getScriptResult(
            @Context                    HttpHeaders headers,
            @PathParam("uuid")          String      uuid,
            @QueryParam("script")       String      scriptName,
            @QueryParam("version")      Integer     scriptVersion,
            @QueryParam("activityPath") String      actPath,
            @QueryParam("inputs")       String      inputJson,
            @CookieParam(COOKIENAME)    Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return scriptUtils
                    .executeScript(headers, item, scriptName, scriptVersion, actPath, inputJson, ImmutableMap.of())
                    .cookie(cookie).build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
        catch (Throwable t) {
            log.error("getScriptResult() - could not execute {}://{}'", item, scriptName, t);
            throw new WebAppExceptionBuilder().exception(new CriseVertxException(t)).newCookie(cookie).build();
        }
    }

    @POST
    @Path("scriptResult")
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN } )
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getScriptResultPost(
            String postData,
            @Context                    HttpHeaders headers,
            @PathParam("uuid")          String      uuid,
            @QueryParam("script")       String      scriptName,
            @QueryParam("version")      Integer     scriptVersion,
            @QueryParam("activityPath") String      actPath,
            @CookieParam(COOKIENAME)    Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return scriptUtils
                    .executeScript(headers, item, scriptName, scriptVersion, actPath, postData,
                            ImmutableMap.of(Script.PARAMETER_AGENT, getAgentProxy(authCookie)))
                    .cookie(cookie).build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
        catch (Throwable t) {
            log.error("getScriptResultPost() - could not execute {}://{}'", item, scriptName, t);
            throw new WebAppExceptionBuilder().exception(new CriseVertxException(t)).newCookie(cookie).build();
        }
    }

    @GET
    @Path("queryResult")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getQueryResult(
            @Context                 HttpHeaders headers,
            @PathParam("uuid")       String      uuid,
            @QueryParam("query")     String      queryName,
            @QueryParam("version")   Integer     queryVersion,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        //FIXME: version should be retrieved from the current item or the Module
        //String view = "last";
        if (queryVersion == null) queryVersion = 0;

        Query query = null;

        try {
            boolean jsonFlag = produceJSON(headers.getAcceptableMediaTypes());

            if (queryName != null) {
                query = LocalObjectLoader.getQuery(queryName, queryVersion);
                return returnQueryResult(item, query, jsonFlag).cookie(cookie).build();
            } else {
                throw new WebAppExceptionBuilder()
                        .message("Name or UUID of Query was missing")
                        .status(Response.Status.NOT_FOUND)
                        .newCookie(cookie).build();
            }
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    private Response.ResponseBuilder returnQueryResult(ItemProxy item, Query query, boolean jsonFlag) throws PersistencyException {
        String xmlResult = item.executeQuery(query);

        if (jsonFlag) return Response.ok(XML.toJSONObject(xmlResult, true).toString());
        else          return Response.ok(xmlResult);
    }

    @GET
    @Path("roles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoles(
            @PathParam("uuid")       String  uuid,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        AgentProxy agent = getAgentProxy(uuid, cookie);

        RolePath[] roles = Gateway.getLookup().getRoles(agent.getPath());
        LinkedHashMap<String, URI> roleData = new LinkedHashMap<String, URI>();

        for (RolePath role : roles) {
            roleData.put(role.getName(), uri.getBaseUriBuilder().path("role").path(role.getName()).build());
        }

        return toJSON(roleData, cookie).build();
    }
}
