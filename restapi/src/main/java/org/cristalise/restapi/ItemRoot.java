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

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.OutcomeAttachment;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilderException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.XML;

import com.google.common.collect.ImmutableMap;

@Path("/item/{uuid}")
public class ItemRoot extends ItemUtils {

    private ScriptUtils scriptUtils = new ScriptUtils();

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
            @QueryParam("script")        String      scriptName,
            @QueryParam("scriptVersion") Integer     scriptVersion,
            @CookieParam(COOKIENAME)     Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        String type = item.getType();
        if (type == null) {
            throw new WebAppExceptionBuilder()
                    .message("Type is null, cannot get MasterOutcome")
                    .status(Response.Status.NOT_FOUND)
                    .newCookie(cookie).build();
        }

        //FIXME: version should be retrieved from the current item or the Module
        String view = "last";
        int schemaVersion = 0;
        if (scriptVersion == null) scriptVersion = 0;

        Script script = null;
        try {
            boolean jsonFlag = produceJSON(headers.getAcceptableMediaTypes());

            if (scriptName != null) {
                final Schema schema = LocalObjectLoader.getSchema(type, schemaVersion);
                script = LocalObjectLoader.getScript(scriptName, scriptVersion);

                return scriptUtils.returnScriptResult(scriptName, item, schema, script, new CastorHashMap(), jsonFlag).cookie(cookie).build();
            }
            else if ((script = getAggregateScript(type, scriptVersion)) != null) {
                final Schema schema = LocalObjectLoader.getSchema(type, schemaVersion);

                return scriptUtils.returnScriptResult(scriptName, item, schema, script, new CastorHashMap(), jsonFlag).cookie(cookie).build();
            }
            else if (item.checkViewpoint(type, view)) {
                return getViewpointOutcome(uuid, type, view, true, cookie).build();
            }
            else {
                throw new WebAppExceptionBuilder()
                        .message("No method available to retrieve MasterOutcome")
                        .status(Response.Status.NOT_FOUND)
                        .newCookie(cookie).build();
            }
        } catch (ObjectNotFoundException | InvalidDataException | ScriptingEngineException e) {
            throw new WebAppExceptionBuilder()
                .message("Error retrieving MasterOutcome")
                .status(Response.Status.NOT_FOUND)
                .newCookie(cookie).build();
        } 
        catch ( UnsupportedOperationException e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Path("scriptResult")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getScriptResult(
            @Context                 HttpHeaders headers,
            @PathParam("uuid")       String      uuid,
            @QueryParam("script")    String      scriptName,
            @QueryParam("version")   Integer     scriptVersion,
            @QueryParam("inputs")    String      inputJson,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return scriptUtils.executeScript(headers, item, scriptName, scriptVersion, inputJson, ImmutableMap.of())
                    .cookie(cookie).build();
        } catch (ObjectNotFoundException | UnsupportedOperationException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @POST
    @Path("scriptResult")
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN } )
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response getScriptResultPost(
            String postData,
            @Context                 HttpHeaders headers,
            @PathParam("uuid")       String      uuid,
            @QueryParam("script")    String      scriptName,
            @QueryParam("version")   Integer     scriptVersion,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return scriptUtils.executeScript(headers, item, scriptName, scriptVersion, postData, ImmutableMap.of())
                    .cookie(cookie).build();
        } catch (ObjectNotFoundException | UnsupportedOperationException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
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
                return returnQueryResult(queryName, item, null, query, jsonFlag).cookie(cookie).build();
            } else {
                throw new WebAppExceptionBuilder()
                        .message("Name or UUID of Query was missing")
                        .status(Response.Status.NOT_FOUND)
                        .newCookie(cookie).build();
            }
        } catch (UnsupportedOperationException | ObjectNotFoundException | InvalidDataException | PersistencyException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    private Response.ResponseBuilder returnQueryResult(String queryName, ItemProxy item,
                                       Object object, Query query, boolean jsonFlag) throws PersistencyException {
        String xmlResult = item.executeQuery(query);

        if (jsonFlag) return Response.ok(XML.toJSONObject(xmlResult, true).toString());
        else          return Response.ok(xmlResult);
    }

    /**
     * Returns the so called Aggregate Script which can be used to construct master outcome.
     * The method is created to retrieve the script in the if statement
     * 
     * @param type value of Type Property of the Item
     * @param scriptVersion the version of the script
     * @return the script or null
     */
    private Script getAggregateScript(String type, Integer scriptVersion) {
        String scriptName = type + Gateway.getProperties().getString("REST.MasterOutcome.postfix", "_Aggregate");

        try {
            return LocalObjectLoader.getScript(scriptName, scriptVersion);
        }
        catch (ObjectNotFoundException | InvalidDataException e1) {
            Logger.msg(5, "ItemRoot.getAggregateScript() - Could not find Script name:%s", scriptName);
        }
        return null;
    }

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

        try {
            String type = item.getType();
            if (type != null) {
                itemSummary.put("type", type);

                if (getAggregateScript(type, 0) != null || item.checkViewpoint(type, "last")) {
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

            return toJSON(itemSummary, cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().message("No Properties found")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        } catch ( Exception e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }


    @GET
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobs(
            @PathParam("uuid")            String  uuid,
            @QueryParam("agent")          String  agentName,
            @QueryParam("activityName")   String  activityName,
            @QueryParam("transitionName") String  transitionName,
            @CookieParam(COOKIENAME)      Cookie  authCookie,
            @Context                      UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        List<Job> jobList = null;
        Job job = null;
        try {
            AgentProxy agent = getAgent(agentName, authCookie);
            if (StringUtils.isNotBlank(activityName)) {
                if (StringUtils.isNotBlank(transitionName)) job = item.getJobByTransitionName(activityName, transitionName, agent);
                else                                        job = item.getJobByName(activityName, agent);
            }
            else
                jobList = item.getJobList(agent);
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder("Error loading joblist", e, null, cookie).build();
        }

        String itemName = item.getName();

        if (jobList != null) {
            ArrayList<Object> jobListData = new ArrayList<Object>();

            for (Job j : jobList) jobListData.add(makeJobData(j, itemName, uri));

            return toJSON(jobListData, cookie).build();
        }
        else if (job != null) {
            return toJSON(makeJobData(job, itemName, uri), cookie).build();
        }
        else {
            throw new WebAppExceptionBuilder().message("No job found for actName:" + activityName + " transName:" + transitionName)
                        .status(Response.Status.NOT_FOUND).newCookie(cookie).build();
        }
    }

    @POST
    @Consumes( {MediaType.TEXT_PLAIN, MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    @Path("{activityPath: .*}")
    public String requestTransition(    String      postData,
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

        try {
            String contentType = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).get(0);

            Logger.msg(5, "ItemRoot.requestTransition() postData:'%s' contentType:'%s'", postData, contentType);

            AgentProxy agent = Gateway.getProxyManager().getAgentProxy(getAgentPath(authCookie));
            String executeResult;

            if (actPath.startsWith(PREDEFINED_PATH)) {
                executeResult = executePredefinedStep(item, postData, contentType, actPath, agent);
            }
            else {
                transition = extractAndCheckTransitionName(transition, uri);
                executeResult = executeJob(item, postData, contentType, actPath, transition, agent);
            }

            if (produceJSON(headers.getAcceptableMediaTypes())) return XML.toJSONObject(executeResult, true).toString();
            else                                                return executeResult;
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }


    /**
     * Method for handling binary uplaod POST methods
     * 
     * @param postData
     * @param headers
     * @param uuid
     * @param actPath
     * @param transition
     * @param authCookie
     * @param uri
     * @return
     */
    @POST
    @Consumes( MediaType.MULTIPART_FORM_DATA )
    @Produces( MediaType.MULTIPART_FORM_DATA)
    @Path("{binaryUploadPath: .*}")
    public String requestBinaryTransition(    String      postData,
            @FormDataParam ("file") InputStream file,
            @Context                    HttpHeaders headers,
            @PathParam("uuid")          String      uuid,
            @PathParam("binaryUploadPath")  String      actPath,
            @QueryParam("transition")   String      transition,
            @CookieParam(COOKIENAME)    Cookie      authCookie,
            @Context                    UriInfo     uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        if (actPath == null) {
            throw new WebAppExceptionBuilder().message("Must specify activity path")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }
        
        if (file == null) {
            throw new WebAppExceptionBuilder().message("Must provide a file to upload")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }

        try {
            String contentType = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE).get(0);

            Logger.msg(5, "ItemRoot.requestTransition() postData:%s", postData);

            AgentProxy agent = Gateway.getProxyManager().getAgentProxy(getAgentPath(authCookie));
            String executeResult;

            if (actPath.startsWith(PREDEFINED_PATH)) {
                executeResult = executePredefinedStep(item, postData, contentType, actPath, agent);
            }
            else {
                transition = extractAndCheckTransitionName(transition, uri);
                executeResult = executeUploadJob(item, file, postData, contentType, actPath, transition, agent);
            }

            if (produceJSON(headers.getAcceptableMediaTypes())) return XML.toJSONObject(executeResult, true).toString();
            else                                                return executeResult;
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }
    
    /**
     * 
     * @param item
     * @param file
     * @param postData
     * @param types
     * @param actPath
     * @param transition
     * @param agent
     * @return
     * @throws AccessRightsException
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws InvalidDataException
     * @throws OutcomeBuilderException
     * @throws InvalidTransitionException
     * @throws ObjectAlreadyExistsException
     * @throws InvalidCollectionModification
     * @throws ScriptErrorException
     * @throws IOException
     */
    private String executeUploadJob(ItemProxy item, InputStream file, String postData, String contentType, String actPath, String transition, AgentProxy agent)
            throws AccessRightsException, ObjectNotFoundException, PersistencyException, InvalidDataException, OutcomeBuilderException,
                   InvalidTransitionException, ObjectAlreadyExistsException, InvalidCollectionModification, ScriptErrorException, IOException
    {
        Job thisJob = item.getJobByTransitionName(actPath, transition, agent);
        
        byte[] binaryData = IOUtils.toByteArray(file);

        if (thisJob == null) {
            throw new ObjectNotFoundException("Job not found for actPath:" + actPath+ " transition:" + transition);
        }

        // set outcome if required
        if (thisJob.hasOutcome()) {
            OutcomeAttachment outcomeAttachment =
                    new OutcomeAttachment(item.getPath(), thisJob.getSchema().getName(), thisJob.getSchema().getVersion(), -1, MediaType.APPLICATION_OCTET_STREAM, binaryData); 

            thisJob.setAttachment(outcomeAttachment);
        }
        return agent.execute(thisJob);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("job/formTemplate/{activityPath: .*}")
    public Response getJobFormTemplate(
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
            throw new WebAppExceptionBuilder().message("Must specify activity path")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }

        try {
            if (actPath.startsWith(PREDEFINED_PATH)) {
                throw new WebAppExceptionBuilder().message("Unimplemented")
                        .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
            }
            else {
                transition = extractAndCheckTransitionName(transition, uri);
                AgentProxy agent = Gateway.getProxyManager().getAgentProxy(getAgentPath(authCookie));

                Job thisJob = item.getJobByTransitionName(actPath, transition, agent);

                if (thisJob == null) {
                    throw new WebAppExceptionBuilder().message("Job not found for actPath:"+actPath+" transition:"+transition)
                            .status(Response.Status.NOT_FOUND).newCookie(cookie).build();
                }
                
                CastorHashMap inputs = (CastorHashMap) thisJob.getActProps().clone();

                inputs.put(Script.PARAMETER_AGENT, agent);
                inputs.put(Script.PARAMETER_ITEM, item);
                inputs.put(Script.PARAMETER_JOB, thisJob);

                // set outcome if required
                if (thisJob.hasOutcome()) {
                    return Response.ok(new OutcomeBuilder(thisJob.getSchema(), false).generateNgDynamicForms(inputs))
                            .cookie(cookie).build();
                } else {
                    Logger.msg(5, "ItemRoot.getJobFormTemplate() - no outcome needed for job:%s", thisJob);
                    return Response.noContent().cookie(cookie).build();
                }
            }
        } catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    /**
     * If transition isn't specified explicitly, look for a valueless query parameter
     * 
     * @param transName the name of the transition, can be null
     * @param uri the uri of the request
     * @return the transName if it was not blank or the name of first valueless query parameter
     * @throws InvalidDataException if no transition name can be extracted
     */
    private String extractAndCheckTransitionName(String transName, UriInfo uri) throws InvalidDataException {
        if (StringUtils.isNotBlank(transName)) return transName;

        for (String key: uri.getQueryParameters().keySet()) {
            List<String> qparams = uri.getQueryParameters().get(key);

            if (qparams.size() == 1 && qparams.get(0).length() == 0) return key;
        }

        throw new InvalidDataException("Must specify transition name");
    }
}
