package org.cristalise.restapi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
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
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.utils.Logger;

@Path("/item/{uuid}")
public class ItemRoot extends ItemUtils {

	@GET
	@Path("name")
	@Produces(MediaType.TEXT_PLAIN)
	public String getName(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie) {
		checkAuth(authCookie);
		return getProxy(uuid).getName();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getItemSummary(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		ItemProxy item = getProxy(uuid);
		
		LinkedHashMap<String, Object> itemSummary = new LinkedHashMap<String, Object>();
		itemSummary.put("name", item.getName());
		try {
			itemSummary.put("properties", getPropertySummary(item));
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("No Properties found", 400);
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
			throw new WebApplicationException("Error loading joblist");
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
			throw new WebApplicationException("Error loading joblist");
		}
		
		// find the requested job by path and transition
		Job thisJob = null;
		for (Job job : jobList) {
			if (job.getStepPath().equals(actPath) && job.getTransition().getName().equalsIgnoreCase(transition)) {
				thisJob = job;
			}
		}
		if (thisJob == null)
			throw new WebApplicationException("Job not found for agent", 404);
		
		// set outcome if required
		if (thisJob.hasOutcome()) {
			thisJob.setOutcome(postData);
		}
		
		// execute the requested job
		try {
			return agent.execute(thisJob);
		} catch (InvalidDataException | ScriptErrorException | ObjectAlreadyExistsException | InvalidCollectionModification e) { // problem with submitted data
			Logger.error(e);
			throw new WebApplicationException(e.getMessage(), 400);
		} catch (AccessRightsException e) { // agent doesn't hold the right to execute
			throw new WebApplicationException(e.getMessage(), 401);
		} catch (ObjectNotFoundException e) { // workflow, schema, script etc not found.
			Logger.error(e);
			throw new WebApplicationException(e.getMessage(), 404);
		} catch (InvalidTransitionException e) { // activity has already changed state
			Logger.error(e);
			throw new WebApplicationException(e.getMessage(), 409);
		} catch (PersistencyException e) { // database failure
			Logger.error(e);
			throw new WebApplicationException(e.getMessage(), 500);
		}
	}
}
