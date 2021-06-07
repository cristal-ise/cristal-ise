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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.agent.JobList;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;

@Path("/agent/{uuid}")
public class AgentJobList extends ItemUtils {

    @GET
    @Path("job")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJobs(
            @PathParam("uuid")                      String  uuid,
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @QueryParam("descending")               Boolean descending,
            @CookieParam(COOKIENAME)                Cookie  authCookie,
            @Context                                UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        AgentProxy agent = getAgentProxy(uuid, cookie);

        descending = descending != null;

        if (batchSize == null) {
            batchSize = Gateway.getProperties().getInt("REST.Job.DefaultBatchSize",
                    Gateway.getProperties().getInt("REST.DefaultBatchSize", 20));
        }

        // fetch this batch of events from the RemoteMap
        JobList jobList;
        try {
            jobList = agent.getJobList();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }

        ArrayList<LinkedHashMap<String, Object>> jobs = new ArrayList<>();

        // replace Jobs with their JSON form. Leave any other object (like the nextBatch URI) as they are
        for (String key : jobList.keySet()) {
            Job job = jobList.get(key);
            try {
                jobs.add(makeJobData(job, job.getItemProxy().getName(), uri));
            }
            catch (ObjectNotFoundException | InvalidItemPathException e) {
                throw new WebAppExceptionBuilder()
                    .message( "Item " + job.getItemUUID() + " in Job not found" )
                    .status( Response.Status.NOT_FOUND )
                    .newCookie(cookie).build();
            }
        }

        return toJSON(jobs, cookie).build();
    }

    @GET
    @Path("job/{jobId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJob(
            @PathParam("uuid")       String  uuid,
            @PathParam("jobId")      String  jobId,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        AgentProxy agent = getAgentProxy(uuid, cookie);

        try {
            Job job = (Job) agent.getJobList().get(jobId);
            return toJSON(makeJobData(job, agent.getName(), uri), cookie).build();
        }
        catch ( ObjectNotFoundException | ClassCastException e ) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
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
