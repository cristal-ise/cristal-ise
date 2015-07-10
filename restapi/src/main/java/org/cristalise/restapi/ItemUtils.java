package org.cristalise.restapi;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
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
import org.cristalise.kernel.utils.CastorHashMap;
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
	
	protected static ItemProxy getProxy(String uuid) {
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

	protected LinkedHashMap<String, Object> makeEventData(Event ev, UriInfo uri) {
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
			outcomeData.put("data", uri.getBaseUriBuilder().path("item").path(ev.getItemUUID()).path("history").path(String.valueOf(ev.getID())).path("data").build());
			eventData.put("outcome", outcomeData);
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
	
	protected LinkedHashMap<String, Object> makeJobData(Job job, String itemName, UriInfo uri) {
		LinkedHashMap<String, Object> jobData = new LinkedHashMap<String, Object>();
		String agentName = job.getAgentName();
		if (agentName != null && agentName.length() > 0)
			jobData.put("agent", agentName);
		jobData.put("role", job.getAgentRole());
		
		//item data
		LinkedHashMap<String, Object> itemData = new LinkedHashMap<String, Object>();
		itemData.put("name", itemName);
		itemData.put("location", uri.getBaseUriBuilder().path("item").path(job.getItemUUID()).build());
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
	
	protected LinkedHashMap<String, Object> makeCollectionData(Collection<?> coll, UriInfo uri) {
		LinkedHashMap<String, Object> collData = new LinkedHashMap<String, Object>();
		collData.put("name", coll.getName());
		collData.put("version", coll.getVersionName());
		String collType = "Collection";
		if (coll instanceof Aggregation) {
			collType = "Aggregation";
		}
		else if (coll instanceof Dependency) {
			collType = "Dependency";
		}
		collData.put("type", collType);
		collData.put("isDescription", coll instanceof CollectionDescription);
		if (coll instanceof Dependency) {
			Dependency dep = (Dependency)coll;
			addProps(collData, dep.getProperties(), dep.getClassProps(), true); // include class props for dependencies here, not in member
		}
		
		LinkedHashMap<String, Object> memberData = new LinkedHashMap<String, Object>();
		for (CollectionMember member : coll.getMembers().list) {
			LinkedHashMap<String, Object> thisMemberData = new LinkedHashMap<String, Object>();
			if (member.getItemPath() != null)
				thisMemberData.put("item", uri.getBaseUriBuilder().path("item").path(member.getItemPath().getUUID().toString()).build());
			else
				thisMemberData.put("item", "");
			addProps(thisMemberData, member.getProperties(), member.getClassProps(), coll instanceof Aggregation); // omit class props for dependencies
			if (member instanceof AggregationMember) {
				AggregationMember aggMem = (AggregationMember)member;
				LinkedHashMap<String, Integer> geo = new LinkedHashMap<String, Integer>();
				geo.put("x", aggMem.getCentrePoint().x);
				geo.put("y", aggMem.getCentrePoint().y);
				geo.put("w", aggMem.getWidth());
				geo.put("h", aggMem.getHeight());
				thisMemberData.put("geometry", geo);
			}
			memberData.put(String.valueOf(member.getID()), thisMemberData);
		}
		collData.put("members", memberData);
		return collData;
	}
	
	private void addProps(LinkedHashMap<String, Object> collData, CastorHashMap props, String classProps, boolean includeClassProps) {
		List<String> propList = Arrays.asList(classProps.split(","));
		LinkedHashMap<String, Object> classPropData = new LinkedHashMap<String, Object>(), 
				propData = new LinkedHashMap<String, Object>();
		for (KeyValuePair prop : props.getKeyValuePairs()) {
			if (propList.contains(prop.getKey()))  // is classProp
				classPropData.put(prop.getKey(), prop.getValue());
			else
				propData.put(prop.getKey(), prop.getValue());
		}
		if (classPropData.size() > 0 && includeClassProps) collData.put("classIdentifiers", classPropData);
		if (propData.size() > 0) collData.put("properties", propData);
	}
}
