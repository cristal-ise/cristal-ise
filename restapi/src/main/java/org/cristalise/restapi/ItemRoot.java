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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

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
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilderException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONObject;

@Path("/item/{uuid}")
public class ItemRoot extends ItemUtils {

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(
            @PathParam("uuid")       String uuid,
            @CookieParam(COOKIENAME) Cookie authCookie)
    {
        checkAuthCookie(authCookie);
        return getProxy(uuid).getName();
    }

    @GET
    @Path("master")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMasterOutcome(
            @PathParam("uuid")           String uuid,
            @QueryParam("script")        String scriptName,
            @QueryParam("scriptVersion") Integer scriptVersion,
            @CookieParam(COOKIENAME) Cookie authCookie)
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

                return returnScriptResult(scriptName, item, schema, script);
            }
            else if ((script = getAggregateScript(type, scriptVersion)) != null) {
                final Schema schema = LocalObjectLoader.getSchema(type, schemaVersion);

                return returnScriptResult(scriptName, item, schema, script);
            }
            else if (item.checkViewpoint(type, view)) {
                return getViewpointOutcome(uuid, type, view, true);
            }
            else
                throw ItemUtils.createWebAppException("No method available to retrieve MasterOutcome", Response.Status.NOT_FOUND);
        }
        catch (ObjectNotFoundException | InvalidDataException | ScriptingEngineException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error retrieving MasterOutcome:" + e.getMessage() , Response.Status.NOT_FOUND);
        }
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

    private Response returnScriptResult(String scriptName, ItemProxy item, final Schema schema, final Script script)
            throws ScriptingEngineException, InvalidDataException
    {
        Object scriptResult = script.evaluate(item.getPath(), new CastorHashMap(), null, null);
        String xmlOutcome = null;

        if (scriptResult instanceof String) {
            xmlOutcome = (String)scriptResult;
        }
        else if (scriptResult instanceof Map) {
            //the map shall have one Key only
            String key = ((Map<?,?>) scriptResult).keySet().toArray(new String[0])[0];
            xmlOutcome = (String)((Map<?,?>) scriptResult).get(key);
        }
        else
            throw ItemUtils.createWebAppException("Cannot handle result of script:" + scriptName, Response.Status.NOT_FOUND);

        if (xmlOutcome != null)
            return getOutcomeResponse(new Outcome(xmlOutcome, schema), new Date(), true);
        else
            throw ItemUtils.createWebAppException("Cannot handle result of script:" + scriptName, Response.Status.NOT_FOUND);
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
        itemSummary.put("name", item.getName());
        itemSummary.put("uuid", uuid);
        itemSummary.put("hasMasterOutcome", false);
        
        PagedResult result = Gateway.getLookup().searchAliases(item.getPath(), 0, 50);
        ArrayList<Object> domainPathesData = new ArrayList<>();

        for (org.cristalise.kernel.lookup.Path p: result.rows) {
            domainPathesData.add(p.toString());
        }

        if (domainPathesData.size() != 0) itemSummary.put("domainPathes", domainPathesData);

        try {
            String type = item.getType();
            if (type != null) {
                itemSummary.put("type", type);
                itemSummary.put("hasMasterOutcome", (getAggregateScript(type, 0) != null || item.checkViewpoint(type, "last")));
            }

            itemSummary.put("properties", getPropertySummary(item));
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("No Properties found", Response.Status.BAD_REQUEST);
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

        // if transition isn't used explicitly, look for a valueless parameter
        if (transition == null) {
            for (String key : uri.getQueryParameters().keySet()) {
                List<String> vals = uri.getQueryParameters().get(key);
                if (vals.size() == 1 && vals.get(0).length() == 0) {
                    transition = key;
                    break;
                }
            }
        }

        if (transition == null) throw ItemUtils.createWebAppException("Must specify transition", Response.Status.BAD_REQUEST);

        // Find agent
        ItemProxy item = getProxy(uuid);

        // get all jobs for agent
        List<Job> jobList;
        try {
            jobList = item.getJobList(agent);
        }
        catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error loading joblist");
        }

        // find the requested job by path and transition
        Job thisJob = null;
        for (Job job : jobList) {
            if (job.getStepPath().equals(actPath) && job.getTransition().getName().equalsIgnoreCase(transition)) {
                thisJob = job;
            }
        }

        if (thisJob == null)
            throw ItemUtils.createWebAppException("Job not found for agent", Response.Status.NOT_FOUND);

        // execute the requested job
        try {
            // set outcome if required
            if (thisJob.hasOutcome()) {
                Logger.msg(5, "ItemRoot.requestTransition() postData:%s", postData);

                List<String> types = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);

                if (types.contains(MediaType.APPLICATION_XML) || types.contains(MediaType.TEXT_XML)) {
                    thisJob.setOutcome(postData);
                }
                else {
                    OutcomeBuilder builder = new OutcomeBuilder(thisJob.getSchema());
                    builder.addJsonInstance(new JSONObject(postData));
                    thisJob.setOutcome(builder.getOutcome());
                }
            }
            return agent.execute(thisJob);
        }
        catch (OutcomeBuilderException | InvalidDataException | ScriptErrorException | ObjectAlreadyExistsException | InvalidCollectionModification e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.BAD_REQUEST);
        }
        catch (AccessRightsException e) { // agent doesn't hold the right to execute
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.UNAUTHORIZED);
        }
        catch (ObjectNotFoundException e) { // workflow, schema, script etc not found.
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
        }
        catch (InvalidTransitionException e) { // activity has already changed state
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.CONFLICT);
        }
        catch (PersistencyException e) { // database failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
