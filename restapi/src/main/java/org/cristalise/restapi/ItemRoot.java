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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
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
import org.json.JSONObject;
import org.json.XML;

@Path("/item/{uuid}")
public class ItemRoot extends ItemUtils {

    static Semaphore mutex = new Semaphore(1);

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(
            @PathParam("uuid")       String uuid,
            @CookieParam(COOKIENAME) Cookie authCookie)
    {
        checkAuthCookie(authCookie);
        String name = getItemName(new ItemPath(UUID.fromString(uuid)));

        if (StringUtils.isBlank(name)) throw ItemUtils.createWebAppException("Cannot resolve UUID", Response.Status.NOT_FOUND);
        return name;
    }

    @GET
    @Path("aliases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAliases(
            @PathParam("uuid")       String uuid,
            @CookieParam(COOKIENAME) Cookie authCookie)
    {
        checkAuthCookie(authCookie);
        LinkedHashMap<String, Object> itemAliases = new LinkedHashMap<String, Object>();

        //Add name, and domainPathes
        makeItemDomainPathsData(new ItemPath(UUID.fromString(uuid)), itemAliases);

        if (StringUtils.isBlank((String)itemAliases.get("name")))
            throw ItemUtils.createWebAppException("Cannot resolve UUID", Response.Status.NOT_FOUND);

        return toJSON(itemAliases);
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
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        String type = item.getType();

        if (type == null) throw ItemUtils.createWebAppException("Type is null, cannot get MasterOutcome ", Response.Status.NOT_FOUND);

        //FIXME: version should be retrieved from the current item or the Module
        String view = "last";
        int schemaVersion = 0;
        if (scriptVersion == null) scriptVersion = 0;

        Script script = null;

        try {
            if (scriptName != null) {
                final Schema schema = LocalObjectLoader.getSchema(type, schemaVersion);
                script = LocalObjectLoader.getScript(scriptName, scriptVersion);

                return returnScriptResult(scriptName, item, schema, script, new CastorHashMap(), produceJSON(headers.getAcceptableMediaTypes()));
            }
            else if ((script = getAggregateScript(type, scriptVersion)) != null) {
                final Schema schema = LocalObjectLoader.getSchema(type, schemaVersion);

                return returnScriptResult(scriptName, item, schema, script, new CastorHashMap(), produceJSON(headers.getAcceptableMediaTypes()));
            }
            else if (item.checkViewpoint(type, view)) {
                return getViewpointOutcome(uuid, type, view, true);
            }
            else
                throw ItemUtils.createWebAppException("No method available to retrieve MasterOutcome", Response.Status.NOT_FOUND);
        }
        catch (ObjectNotFoundException | InvalidDataException | ScriptingEngineException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error retrieving MasterOutcome:" + e.getMessage() , e, Response.Status.NOT_FOUND);
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
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        // FIXME: version should be retrieved from the current item or the Module
        // String view = "last";
        if (scriptVersion == null) scriptVersion = 0;

        Script script = null;
        if (scriptName != null) {
            try {
                script = LocalObjectLoader.getScript(scriptName, scriptVersion);
                if (inputJson == null) inputJson = "{}";

                JSONObject json = new JSONObject(inputJson);
                CastorHashMap inputs = new CastorHashMap();
                for (String key: json.keySet()) inputs.put(key, json.get(key));

                return returnScriptResult(scriptName, item, null, script, inputs, produceJSON(headers.getAcceptableMediaTypes()));
            }
            catch (Exception e) {
                Logger.error(e);
                throw ItemUtils.createWebAppException("Error executing Script:" + e.getMessage(), e, Response.Status.NOT_FOUND);
            }
        }
        else
            throw ItemUtils.createWebAppException("Name or UUID of Script was missing", Response.Status.NOT_FOUND);
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
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        //FIXME: version should be retrieved from the current item or the Module
        //String view = "last";
        if (queryVersion == null) queryVersion = 0;

        Query query = null;

        try {
            if (queryName != null) {
                query = LocalObjectLoader.getQuery(queryName, queryVersion);
                return returnQueryResult(queryName, item, null, query, produceJSON(headers.getAcceptableMediaTypes()));
            }
            else
                throw ItemUtils.createWebAppException("Name or UUID of Query was missing", Response.Status.NOT_FOUND);
        }
        catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error executing Query:" + e.getMessage() , e, Response.Status.NOT_FOUND);
        }
    }

    private Response returnQueryResult(String queryName, ItemProxy item, Object object, Query query, boolean jsonFlag) throws PersistencyException {
        String xmlResult = item.executeQuery(query);

        if (jsonFlag) return Response.ok(XML.toJSONObject(xmlResult).toString()).build();
        else          return Response.ok(xmlResult).build();
    }

    /**
     * Returns the so called Aggregate Script which can be used to construct master outcome.
     * The method is created to retrieve the script in the if statement
     * 
     * @param the value of Type Property of the Item
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

    /**
     * @see org.cristalise.restapi.ItemUtils#returnScriptResult(java.lang.String, org.cristalise.kernel.entity.proxy.ItemProxy, org.cristalise.kernel.persistency.outcome.Schema, org.cristalise.kernel.scripting.Script)
     */
    @Override
    protected Response returnScriptResult(String scriptName, ItemProxy item, final Schema schema, final Script script, CastorHashMap inputs, boolean jsonFlag)
            throws ScriptingEngineException, InvalidDataException
    {
        try {
            mutex.acquire();
            return super.returnScriptResult(scriptName, item, schema, script, inputs, jsonFlag);
        }
        catch (ScriptingEngineException e) {
            throw e;
        }
        catch (Exception e) {
            throw new InvalidDataException(e.getMessage());
        }
        finally {
            mutex.release();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemSummary(
            @PathParam("uuid")       String  uuid,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        LinkedHashMap<String, Object> itemSummary = new LinkedHashMap<String, Object>();

        //Add name, and domainPaths
        makeItemDomainPathsData(item.getPath(), itemSummary);

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
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("No Properties found", e, Response.Status.BAD_REQUEST);
        }

        itemSummary.put("viewpoints",  getAllViewpoints(item, uri));
        itemSummary.put("collections", enumerate(item, COLLECTION, "collection", uri));
        itemSummary.put("workflow",    getItemURI(uri, item, "workflow"));
        itemSummary.put("history",     getItemURI(uri, item, "history"));
        itemSummary.put("outcome",     getItemURI(uri, item, "outcome"));

        return toJSON(itemSummary);
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
        checkAuthCookie(authCookie);

        ItemProxy item = getProxy(uuid);
        AgentProxy agent = getAgent(agentName, authCookie);

        List<Job> jobList = null;
        Job job = null;
        try {
            if (StringUtils.isNotBlank(activityName)) {
                if (StringUtils.isNotBlank(transitionName)) job = item.getJobByTransitionName(activityName, transitionName, agent);
                else                                        job = item.getJobByName(activityName, agent);
            }
            else
                jobList = item.getJobList(agent);
        }
        catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error loading joblist");
        }

        String itemName = item.getName();
        if (jobList != null) {
            ArrayList<Object> jobListData = new ArrayList<Object>();

            for (Job j : jobList) jobListData.add(makeJobData(j, itemName, uri));

            return toJSON(jobListData);
        }
        else if (job != null) {
            return toJSON(makeJobData(job, itemName, uri));
        }
        else {
            throw ItemUtils.createWebAppException("No job found for actName:" + activityName + " transName:" + transitionName, Response.Status.NOT_FOUND);
        }
    }

    @POST
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
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
        AgentProxy agent = null;
        try {
            agent = (AgentProxy)Gateway.getProxyManager().getProxy( checkAuthCookie(authCookie) );
        }
        catch (ObjectNotFoundException e1) {
            throw ItemUtils.createWebAppException(e1.getMessage(), Response.Status.UNAUTHORIZED);
        }

        if (actPath == null) throw ItemUtils.createWebAppException("Must specify activity path", Response.Status.BAD_REQUEST);

        // Find agent
        ItemProxy item = getProxy(uuid);

        try {
            List<String> types = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);

            Logger.msg(5, "ItemRoot.requestTransition() postData:%s", postData);

            if (actPath.startsWith(PREDEFINED_PATH)) {
                return executePredefinedStep(item, postData, types, actPath, agent);
            }
            else {
                transition = extractAndCcheckTransitionName(transition, uri);
                String execJob = executeJob(item, postData, types, actPath, transition, agent);
                if (types.contains(MediaType.APPLICATION_XML) || types.contains(MediaType.TEXT_XML)) {
                	return execJob;
                } else {
                	return XML.toJSONObject(execJob).toString();
                }
                
            }
        }
        catch (OutcomeBuilderException | InvalidDataException | ScriptErrorException | ObjectAlreadyExistsException | InvalidCollectionModification e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.BAD_REQUEST);
        }
        catch (AccessRightsException e) { // agent doesn't hold the right to execute
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.UNAUTHORIZED);
        }
        catch (ObjectNotFoundException e) { // workflow, schema, script etc not found.
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.NOT_FOUND);
        }
        catch (InvalidTransitionException e) { // activity has already changed state
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.CONFLICT);
        }
        catch (PersistencyException e) { // database failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) { // any other failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
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
        AgentProxy agent = null;
        try {
            agent = (AgentProxy)Gateway.getProxyManager().getProxy( checkAuthCookie(authCookie) );
        }
        catch (ObjectNotFoundException e1) {
            throw ItemUtils.createWebAppException(e1.getMessage(), Response.Status.UNAUTHORIZED);
        }

        if (actPath == null) throw ItemUtils.createWebAppException("Must specify activity path", Response.Status.BAD_REQUEST);
        
        if (file == null) throw ItemUtils.createWebAppException("Must provide a file to upload", Response.Status.BAD_REQUEST);

        // Find agent
        ItemProxy item = getProxy(uuid);

        try {
            List<String> types = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);

            Logger.msg(5, "ItemRoot.requestTransition() postData:%s", postData);

            if (actPath.startsWith(PREDEFINED_PATH)) {
                return executePredefinedStep(item, postData, types, actPath, agent);
            }
            else {
                transition = extractAndCcheckTransitionName(transition, uri);

                return executeUploadJob(item, file, postData, types, actPath, transition, agent);
            }
        }
        catch (OutcomeBuilderException | InvalidDataException | ScriptErrorException | ObjectAlreadyExistsException | InvalidCollectionModification e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.BAD_REQUEST);
        }
        catch (AccessRightsException e) { // agent doesn't hold the right to execute
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.UNAUTHORIZED);
        }
        catch (ObjectNotFoundException e) { // workflow, schema, script etc not found.
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.NOT_FOUND);
        }
        catch (InvalidTransitionException e) { // activity has already changed state
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.CONFLICT);
        }
        catch (PersistencyException e) { // database failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) { // any other failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
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
    private String executeUploadJob(ItemProxy item, InputStream file, String postData, List<String> types, String actPath, String transition, AgentProxy agent)
            throws AccessRightsException, ObjectNotFoundException, PersistencyException, InvalidDataException, OutcomeBuilderException,
                   InvalidTransitionException, ObjectAlreadyExistsException, InvalidCollectionModification, ScriptErrorException, IOException
    {
        Job thisJob = item.getJobByTransitionName(actPath, transition, agent);
        
        byte[] binaryData = IOUtils.toByteArray(file);

        if (thisJob == null)
            throw ItemUtils.createWebAppException("Job not found for actPath:"+actPath+" transition:"+transition, Response.Status.NOT_FOUND);

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
        AgentProxy agent = null;
        try {
            agent = (AgentProxy)Gateway.getProxyManager().getProxy( checkAuthCookie(authCookie) );
        }
        catch (ObjectNotFoundException e1) {
            throw ItemUtils.createWebAppException(e1.getMessage(), Response.Status.UNAUTHORIZED);
        }

        if (actPath == null) throw ItemUtils.createWebAppException("Must specify activity path", Response.Status.BAD_REQUEST);

        // Find agent
        ItemProxy item = getProxy(uuid);

        try {
            if (actPath.startsWith("workflow/predefined")) {
                throw ItemUtils.createWebAppException("Unimplemented", Response.Status.BAD_REQUEST);
            }
            else {
                transition = extractAndCcheckTransitionName(transition, uri);

                Job thisJob = item.getJobByTransitionName(actPath, transition, agent);

                if (thisJob == null)
                    throw ItemUtils.createWebAppException("Job not found for actPath:"+actPath+" transition:"+transition, Response.Status.NOT_FOUND);

                // set outcome if required
                if (thisJob.hasOutcome()) {
                    return Response.ok(new OutcomeBuilder(thisJob.getSchema(), false).generateNgDynamicForms(thisJob.getActProps())).build();
                }
                else {
                    Logger.msg(5, "ItemRoot.getJobFormTemplate() - no outcome needed for job:%s", thisJob);
                    return Response.noContent().build();
                }
            }
        }
        catch (OutcomeBuilderException | InvalidDataException  e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.BAD_REQUEST);
        }
        catch (AccessRightsException e) { // agent doesn't hold the right to execute
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.UNAUTHORIZED);
        }
        catch (ObjectNotFoundException e) { // workflow, schema, script etc not found.
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.NOT_FOUND);
        }
        catch (PersistencyException e) { // database failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        catch (Exception e) { // any other failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * If transition isn't specified explicitly, look for a valueless query parameter
     * 
     * @param transName the name of the transition, can be null
     * @param uri the uri of the request
     * @return the transName if it was not blank or the name of first valueless query parameter
     * @throws WebApplicationException if no transition name can be extracted
     */
    private String extractAndCcheckTransitionName(String transName, UriInfo uri) {
        if (StringUtils.isNotBlank(transName)) return transName;

        for (String key: uri.getQueryParameters().keySet()) {
            List<String> qparams = uri.getQueryParameters().get(key);

            if (qparams.size() == 1 && qparams.get(0).length() == 0) return key;
        }

        throw ItemUtils.createWebAppException("Must specify transition name", Response.Status.BAD_REQUEST);
    }
}
