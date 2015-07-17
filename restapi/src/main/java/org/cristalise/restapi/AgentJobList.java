package org.cristalise.restapi;

import java.net.URI;
import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;

@Path("/agent/{uuid}")
public class AgentJobList extends RemoteMapAccess {

	@GET
	@Path("job")
	@Produces(MediaType.APPLICATION_JSON)
	public Response list(@PathParam("uuid") String uuid, 
			@DefaultValue("0") @QueryParam("start") Integer start, 
			@QueryParam("batch") Integer batchSize, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		if (!(item instanceof AgentProxy))
			throw new WebApplicationException("UUID does not belong to an Agent", 400);
		if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.Job.DefaultBatchSize", 
				Gateway.getProperties().getInt("REST.DefaultBatchSize", 20));
		
		// fetch this batch of events from the RemoteMap
		LinkedHashMap<String, Object> jobs = super.list(item, ClusterStorage.JOB, start, batchSize, uri);
		
		// replace Jobs with their JSON form. Leave any other object (like the nextBatch URI) as they are
		for (String key : jobs.keySet()) {
			Object obj = jobs.get(key);
			if (obj instanceof Job) {
				Job job = (Job)obj;
				try {
					jobs.put(key, makeJobData(job, job.getItemProxy().getName(), uri));
				} catch (ObjectNotFoundException e) {
					jobs.put(key, "ERROR: Item "+job.getItemUUID()+" not found");
				} catch (InvalidItemPathException e) {
					jobs.put(key, "ERROR: Invalid Item UUID in Job:"+job.getItemUUID());
				}
			}
		}
		return toJSON(jobs);
	}
	
	@GET
	@Path("job/{jobId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getEvent(@PathParam("uuid") String uuid, @PathParam("jobId") String jobId, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		if (!(item instanceof AgentProxy))
			throw new WebApplicationException("UUID does not belong to an Agent", 400);
		Job job = (Job)get(item, ClusterStorage.JOB, jobId);
		try {
			return toJSON(makeJobData(job, job.getItemProxy().getName(), uri));
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException("Item "+job.getItemUUID()+" in Job not found");
		} catch (InvalidItemPathException e) {
			throw new WebApplicationException("Invalid Item UUID in Job "+job.getItemUUID()+" in Job not found");
		}
	}
	
	@GET
	@Path("roles")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRoles(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		if (!(item instanceof AgentProxy))
			throw new WebApplicationException("UUID does not belong to an Agent", 400);
		AgentProxy agent = (AgentProxy)item;
		RolePath[] roles = Gateway.getLookup().getRoles(agent.getPath());
		LinkedHashMap<String, URI> roleData = new LinkedHashMap<String, URI>();
		for (RolePath role : roles) {
			roleData.put(role.getName(), uri.getBaseUriBuilder().path("role").path(role.getName()).build());
		}
		return toJSON(roleData);
	}
}
