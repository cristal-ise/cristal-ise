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

import org.cristalise.kernel.common.*;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.utils.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Path("/item/{uuid}")
public class ItemRoot extends ItemUtils {

    @GET
    @Path("name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie) {
        checkAuthCookie(authCookie);
        return getProxy(uuid).getName();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getItemSummary(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie,
            @Context UriInfo uri) {
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        LinkedHashMap<String, Object> itemSummary = new LinkedHashMap<String, Object>();
        itemSummary.put("name", item.getName());
        try {
            itemSummary.put("properties", getPropertySummary(item));
        } catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("No Properties found", Response.Status.BAD_REQUEST);
        }

        itemSummary.put("data", enumerate(item, ClusterStorage.VIEWPOINT, "data", uri));

        itemSummary.put("collections", enumerate(item, ClusterStorage.COLLECTION, "collection", uri));
        return toJSON(itemSummary);
    }

    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobs(@PathParam("uuid") String uuid, 
            @QueryParam("agent") String agentName, @CookieParam(COOKIENAME) Cookie authCookie,
            @Context UriInfo uri) {
        ItemProxy item = getProxy(uuid);
        AgentProxy agent = getAgent(agentName, authCookie);

        List<Job> jobList;
        try {
            jobList = item.getJobList(agent);
        } catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error loading joblist");
        }

        ArrayList<Object> jobListData = new ArrayList<Object>();
        String itemName = item.getName();
        for (Job job : jobList) {
            jobListData.add(makeJobData(job, itemName, uri));
        }

        return toJSON(jobListData);

    }

    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.TEXT_XML)
    @Path("{activityPath: .*}")
    public String requestTransition(String postData, @PathParam("uuid") String uuid,
            @PathParam("activityPath") String actPath,
            @QueryParam("transition") String transition,
            @QueryParam("agent") String agentName,
            @CookieParam(COOKIENAME) Cookie authCookie,
            @Context UriInfo uri) {

        // if transition isn't used explicitly, look for a valueless parameter
        if (transition == null) {
            for(String key: uri.getQueryParameters().keySet()) {
                List<String> vals = uri.getQueryParameters().get(key);
                if (vals.size()==1 && vals.get(0).length() == 0) {
                    transition = key;
                    break;
                }
            }
            if (transition == null) // default to Done
                transition = "Done";
        }	

        //Find agent
        ItemProxy item = getProxy(uuid);
        AgentProxy agent = getAgent(agentName, authCookie);

        // get all jobs for agent
        List<Job> jobList;
        try {
            jobList = item.getJobList(agent);
        } catch (Exception e) {
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
                thisJob.setOutcome(postData);
            }
            return agent.execute(thisJob);
        } catch (InvalidDataException | ScriptErrorException | ObjectAlreadyExistsException | InvalidCollectionModification e) { // problem with submitted data
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.BAD_REQUEST);
        } catch (AccessRightsException e) { // agent doesn't hold the right to execute
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.UNAUTHORIZED);
        } catch (ObjectNotFoundException e) { // workflow, schema, script etc not found.
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
        } catch (InvalidTransitionException e) { // activity has already changed state
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.CONFLICT);
        } catch (PersistencyException e) { // database failure
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
