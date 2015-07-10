package org.cristalise.restapi;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

public abstract class ItemUtils extends RestHandler {
	
	DateFormat dateFormatter;
	
	public ItemUtils() {
		super();
		dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	
	protected static LinkedHashMap<String, String> getPropertySummary(ItemProxy item) throws ObjectNotFoundException {
		LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
		for (String propName : item.getContents(ClusterStorage.PROPERTY)) {
			if (!propName.equalsIgnoreCase("name"))
				props.put(propName, item.getProperty(propName));
		}
		return props;
	}	
	
	protected static ItemProxy getProxy(String uuid) throws WebApplicationException {
		ItemProxy item;
		ItemPath itemPath;
		try {
			itemPath = Gateway.getLookup().getItemPath(uuid);
		} catch (InvalidItemPathException e) {
			Logger.error(e);
			throw new WebApplicationException(400); // Bad Request - the UUID wasn't valid
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException(404); // UUID isn't used in this server
		}
		
		try {
			item = Gateway.getProxyManager().getProxy(itemPath);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException(404); // Not found - the path doesn't exist
		}
		return item;
	}
	
	public LinkedHashMap<String, URI> enumerate(ItemProxy item, String dataPath, String uriPath, UriInfo uri) {
		String[] children;
		try {
			children = Gateway.getStorage().getClusterContents(item.getPath(), dataPath);
		} catch (PersistencyException e) {
			Logger.error(e);
			throw new WebApplicationException("Database Error");
		}
		
		LinkedHashMap<String, URI> childrenWithLinks = new LinkedHashMap<>();
		for (String child : children) {
			childrenWithLinks.put(child, uri.getBaseUriBuilder().path("item").path(item.getPath().getUUID().toString()).
					path(uriPath).path(child).build());
		}
		
		return childrenWithLinks;
	}
	
	protected Response getOutcomeResponse(Outcome oc, Event ev) {
		Date eventDate;
		try {
			eventDate = dateFormatter.parse(ev.getTimeString());
		} catch (ParseException e) {
			Logger.error(e);
			throw new WebApplicationException("Invalid timestamp in event "+ev.getID()+": "+ev.getTimeString());
		}
		return Response.ok(oc.getData()).lastModified(eventDate).build();
	}

	protected LinkedHashMap<String, Object> jsonEvent(Event ev, UriInfo uri) {
		LinkedHashMap<String, Object> eventData = new LinkedHashMap<String, Object>();
		eventData.put("id", ev.getID());
		eventData.put("timestamp", ev.getTimeString());
		eventData.put("agent", ev.getAgentPath().getAgentName());
		eventData.put("role", ev.getAgentRole());
		
		if (ev.getSchemaName() != null && ev.getSchemaName().length()>0) { // add outcome info
			LinkedHashMap<String, Object> outcomeData = new LinkedHashMap<String, Object>();
			outcomeData.put("name", ev.getViewName());
			outcomeData.put("schema", ev.getSchemaName()+" v"+ev.getSchemaVersion());
			outcomeData.put("schemaData", uri.getBaseUriBuilder().path("schema").path(ev.getSchemaName()).path(String.valueOf(ev.getSchemaVersion())).build());
			eventData.put("data", outcomeData);
		}
		
		// activity data
		LinkedHashMap<String, Object> activityData = new LinkedHashMap<String, Object>();
		activityData.put("name", ev.getStepName());
		activityData.put("path", ev.getStepPath());
		activityData.put("type", ev.getStepType());
		eventData.put("activity", activityData);
		
		// state data
		LinkedHashMap<String, Object> stateData = new LinkedHashMap<String, Object>();
		try {
			StateMachine sm = LocalObjectLoader.getStateMachine(ev.getStateMachineName(), ev.getStateMachineVersion());
			stateData.put("name", sm.getState(ev.getTransition()).getName());
			stateData.put("origin", sm.getState(ev.getOriginState()).getName());
			stateData.put("target", sm.getState(ev.getTargetState()).getName());
			stateData.put("stateMachine", ev.getStateMachineName()+" v"+ev.getStateMachineVersion());
			stateData.put("stateMachineData", uri.getBaseUriBuilder().path("stateMachine").path(ev.getStateMachineName()).path(String.valueOf(ev.getStateMachineVersion())).build());
			eventData.put("transition", stateData);
		} catch (ObjectNotFoundException e) {
			eventData.put("transition", "ERROR: State Machine "+ev.getStateMachineName()+" v"+ev.getStateMachineVersion()+" not found!");
		} catch (InvalidDataException e) {
			eventData.put("transition", "ERROR: State Machine definition "+ev.getStateMachineName()+" v"+ev.getStateMachineVersion()+" not valid!");
		}
		
		return eventData;
	}
	
	protected LinkedHashMap<String, Object> jsonJob(ItemProxy item, Job job, UriInfo uri) {
		LinkedHashMap<String, Object> jobData = new LinkedHashMap<String, Object>();
		String agentName = job.getAgentName();
		if (agentName != null && agentName.length() > 0)
			jobData.put("agent", agentName);
		jobData.put("role", job.getAgentRole());
		
		//item data
		LinkedHashMap<String, Object> itemData = new LinkedHashMap<String, Object>();
		itemData.put("name", item.getName());
		itemData.put("location", uri.getBaseUriBuilder().path("item").path(item.getPath().getUUID().toString()).build());
		jobData.put("item", itemData);
		
		// activity data
		LinkedHashMap<String, Object> activityData = new LinkedHashMap<String, Object>();
		activityData.put("name", job.getStepName());
		activityData.put("path", job.getStepPath());
		activityData.put("type", job.getStepType());
		LinkedHashMap<String, Object> activityPropData = new LinkedHashMap<String, Object>();
		for (KeyValuePair actProp : job.getKeyValuePairs()) {
			String key = actProp.getKey();
			String value = job.getActPropString(key);
			if (value!=null && value.length()>0)
				activityPropData.put(key, job.getActPropString(key));
		}
		activityData.put("properties", activityPropData);
		jobData.put("activity", activityData);
		
		LinkedHashMap<String, Object> stateData = new LinkedHashMap<String, Object>();
		stateData.put("name", job.getTransition().getName());
		stateData.put("origin", job.getOriginStateName());
		stateData.put("target", job.getTargetStateName());
		stateData.put("stateMachine", job.getActPropString("StateMachineName")+" v"+job.getActPropString("StateMachineVersion"));
		stateData.put("stateMachineData", uri.getBaseUriBuilder().path("stateMachine").path(job.getActPropString("StateMachineName")).path(job.getActPropString("StateMachineVersion")).build());
		jobData.put("transition", stateData);
		
		if (job.hasOutcome()) { // add outcome info
			LinkedHashMap<String, Object> outcomeData = new LinkedHashMap<String, Object>();
			try {
				outcomeData.put("required", job.isOutcomeRequired());
				outcomeData.put("schema", job.getSchemaName()+" v"+job.getSchemaVersion());
				outcomeData.put("schemaData", uri.getBaseUriBuilder().path("schema").path(job.getSchemaName()).path(String.valueOf(job.getSchemaVersion())).build());
				jobData.put("data", outcomeData);
			} catch (InvalidDataException | ObjectNotFoundException e) {
				Logger.error(e);
				jobData.put("data", "Schema not found");
			}
		}
		
		return jobData;
	}
}
