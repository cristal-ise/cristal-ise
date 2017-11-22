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

import static org.cristalise.kernel.persistency.ClusterType.JOB;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
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
            @CookieParam(COOKIENAME)                Cookie  authCookie,
            @Context                                UriInfo uri)
    {
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        if (!(item instanceof AgentProxy))
            throw ItemUtils.createWebAppException("UUID does not belong to an Agent", Response.Status.BAD_REQUEST);

        if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Job.DefaultBatchSize",
                Gateway.getProperties().getInt("REST.DefaultBatchSize", 20));

        // fetch this batch of events from the RemoteMap
        LinkedHashMap<String, Object> batch = RemoteMapAccess.list(item, JOB, start, batchSize, uri);
        ArrayList<LinkedHashMap<String, Object>> jobs = new ArrayList<>();

        // replace Jobs with their JSON form. Leave any other object (like the nextBatch URI) as they are
        for (String key : batch.keySet()) {
            Object obj = batch.get(key);
            if (obj instanceof Job) {
                Job job = (Job) obj;
                try {
                    jobs.add(makeJobData(job, job.getItemProxy().getName(), uri));
                }
                catch (ObjectNotFoundException | InvalidItemPathException e) {
                    throw ItemUtils.createWebAppException("Item " + job.getItemUUID() + " in Job not found", Response.Status.NOT_FOUND);
                }
            }
        }
        return toJSON(jobs);
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
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        if (!(item instanceof AgentProxy))
            throw ItemUtils.createWebAppException("UUID does not belong to an Agent", Response.Status.BAD_REQUEST);

        Job job = (Job) RemoteMapAccess.get(item, JOB, jobId);

        try {
            return toJSON(makeJobData(job, job.getItemProxy().getName(), uri));
        }
        catch (ObjectNotFoundException e) {
            throw ItemUtils.createWebAppException("Item " + job.getItemUUID() + " in Job not found");
        }
        catch (InvalidItemPathException e) {
            throw ItemUtils.createWebAppException("Invalid Item UUID in Job " + job.getItemUUID() + " in Job not found");
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
        checkAuthCookie(authCookie);
        ItemProxy item = getProxy(uuid);

        if (!(item instanceof AgentProxy))
            throw ItemUtils.createWebAppException("UUID does not belong to an Agent", Response.Status.BAD_REQUEST);

        AgentProxy agent = (AgentProxy) item;
        RolePath[] roles = Gateway.getLookup().getRoles(agent.getPath());
        LinkedHashMap<String, URI> roleData = new LinkedHashMap<String, URI>();

        for (RolePath role : roles) {
            roleData.put(role.getName(), uri.getBaseUriBuilder().path("role").path(role.getName()).build());
        }

        return toJSON(roleData);
    }
}
