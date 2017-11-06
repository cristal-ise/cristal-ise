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

import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.json.XML;

public abstract class ItemUtils extends RestHandler {

    final DateFormat dateFormatter;

    public ItemUtils() {
        super();
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    protected static URI getItemURI(UriInfo uri, ItemProxy item, String...path) {
        return getItemURI(uri, item.getPath(), path);
    }

    protected static URI getItemURI(UriInfo uri, ItemPath item, String...path) {
        return getItemURI(uri, item.getUUID(), path);
    }

    protected static URI getItemURI(UriInfo uri, UUID item, String...path) {
        UriBuilder builder = uri.getBaseUriBuilder().path("item").path(item.toString());

        for (String name: path) builder.path(name);

        return builder.build();
    }

    protected static URI getItemURI(UriInfo uri, String...segments) {
        UriBuilder builder = uri.getBaseUriBuilder().path("item");

        for (String path: segments) builder.path(path);

        return builder.build();
    }

    protected static ArrayList<LinkedHashMap<String, String>> getPropertySummary(ItemProxy item) throws ObjectNotFoundException {
        ArrayList<LinkedHashMap<String, String>> props = new ArrayList<>();

        for (String propName : item.getContents(PROPERTY)) {
            if (!propName.equalsIgnoreCase("name")) {
                LinkedHashMap<String, String> prop = new LinkedHashMap<>();
                prop.put("name", propName);
                prop.put("value", item.getProperty(propName));
                props.add(prop);
            }
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
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.BAD_REQUEST); // Bad Request - the UUID wasn't valid
        } catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND); // UUID isn't used in this server
        }

        try {
            item = Gateway.getProxyManager().getProxy(itemPath);
        } catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND); // Not found - the path doesn't exist
        }
        return item;
    }

    public LinkedHashMap<String, URI> enumerate(ItemProxy item, ClusterType cluster, String uriPath, UriInfo uri) {
        return enumerate(item, cluster.getName(), uriPath, uri);
    }

    public LinkedHashMap<String, URI> enumerate(ItemProxy item, String dataPath, String uriPath, UriInfo uri) {
        String[] children;
        try {
            children = item.getContents(dataPath);
        } catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Database Error");
        }

        LinkedHashMap<String, URI> childrenWithLinks = new LinkedHashMap<>();

        for (String child : children) {
            childrenWithLinks.put(child, getItemURI(uri, item, uriPath, child));
        }

        return childrenWithLinks;
    }

    protected ArrayList<HashMap<String, Object>> getAllViewpoints(ItemProxy item, UriInfo uri) {
        ArrayList<HashMap<String, Object>> viewPoints = new ArrayList<>();

        try {
            for (String schema: item.getContents(VIEWPOINT)) {

                for (String view: item.getContents(VIEWPOINT+"/"+schema)) {
                    HashMap<String, Object> viewpoint = new HashMap<>();

                    viewpoint.put("schemaName", schema);
                    viewpoint.put("viewName", view);
                    viewpoint.put("url", getItemURI(uri, item, "viewpoint", schema, view));

                    viewPoints.add(viewpoint);
                }
            }
        }
        catch (ObjectNotFoundException e) {}

        return viewPoints;
    }

    protected Response getOutcomeResponse(Outcome oc, Event ev, boolean json) {
        Date eventDate;
        try {
            eventDate = dateFormatter.parse(ev.getTimeString());
        } catch (ParseException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Invalid timestamp in event "+ev.getID()+": "+ev.getTimeString());
        }

        String result;

        if(json) result = XML.toJSONObject(oc.getData()).toString();
        else     result = oc.getData();

        return Response.ok(result).lastModified(eventDate).build();
    }

    protected LinkedHashMap<String, Object> makeEventData(Event ev, UriInfo uri) {
        LinkedHashMap<String, Object> eventData = new LinkedHashMap<String, Object>();
        eventData.put("id", ev.getID());
        eventData.put("timestamp", ev.getTimeString());
        eventData.put("agent", ev.getAgentPath().getAgentName());
        eventData.put("role", ev.getAgentRole());

        if (ev.getSchemaName() != null && ev.getSchemaName().length()>0) { // add outcome info
            LinkedHashMap<String, Object> outcomeData = new LinkedHashMap<String, Object>();
            outcomeData.put("name",       ev.getViewName());
            outcomeData.put("schema",     ev.getSchemaName()+" v"+ev.getSchemaVersion());
            outcomeData.put("schemaData", uri.getBaseUriBuilder().build("schema", ev.getSchemaName(), ev.getSchemaVersion()));
            outcomeData.put("data",       getItemURI(uri, ev.getItemUUID(), "history", String.valueOf(ev.getID())));
            
            eventData.put("outcome", outcomeData);
        }

        // activity data
        LinkedHashMap<String, Object> activityData = new LinkedHashMap<String, Object>();
        activityData.put("name", ev.getStepName());
        activityData.put("path", ev.getStepPath());
        activityData.put("type", ev.getStepType());
        eventData.put("activity", activityData);

        // state data
        LinkedHashMap<String, Object> transData = new LinkedHashMap<String, Object>();
        try {
            StateMachine sm = LocalObjectLoader.getStateMachine(ev.getStateMachineName(), ev.getStateMachineVersion());
            transData.put("name", sm.getTransition(ev.getTransition()).getName());
            transData.put("origin", sm.getState(ev.getOriginState()).getName());
            transData.put("target", sm.getState(ev.getTargetState()).getName());
            transData.put("stateMachine", ev.getStateMachineName()+" v"+ev.getStateMachineVersion());
            transData.put("stateMachineData", uri.getBaseUriBuilder().path("stateMachine").path(ev.getStateMachineName()).path(String.valueOf(ev.getStateMachineVersion())).build());
            eventData.put("transition", transData);
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
        if (StringUtils.isNotBlank(agentName)) jobData.put("agent", agentName);
        jobData.put("role", job.getAgentRole());

        //item data
        LinkedHashMap<String, Object> itemData = new LinkedHashMap<String, Object>();
        itemData.put("name", itemName);
        itemData.put("location", getItemURI(uri, job.getItemUUID()));
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
                outcomeData.put("schema", job.getSchema().getName()+" v"+job.getSchema().getVersion());
                outcomeData.put("schemaData", uri.getBaseUriBuilder().path("schema").path(job.getSchema().getName()).path(String.valueOf(job.getSchema().getVersion())).build());
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
                thisMemberData.put("item", getItemURI(uri, member.getItemPath()));
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
        List<String> propList = null;
        if (classProps != null) propList = Arrays.asList(classProps.split(","));

        LinkedHashMap<String, Object> classPropData = new LinkedHashMap<String, Object>(), propData = new LinkedHashMap<String, Object>();

        for (KeyValuePair prop : props.getKeyValuePairs()) {
            if (propList != null && propList.contains(prop.getKey()))  // is classProp
                classPropData.put(prop.getKey(), prop.getValue());
            else
                propData.put(prop.getKey(), prop.getValue());
        }
        if (classPropData.size() > 0 && includeClassProps) collData.put("classIdentifiers", classPropData);
        if (propData.size() > 0) collData.put("properties", propData);
    }

    /**
     * Creates a WebApplicationException response from a simple text message. The status is set to INTERNAL_SERVER_ERROR
     *
     * @param msg text message
     * @return WebApplicationException response
     */
    public static WebApplicationException createWebAppException(String msg) {
        return createWebAppException(msg, Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * Creates a WebApplicationException response from a simple text message and status
     *
     * @param msg text message
     * @param status HTTP status of the response
     * @return WebApplicationException response
     */
    public static WebApplicationException createWebAppException(String msg, Response.Status status) {
        return createWebAppException(msg, null, status);
    }

    /**
     * Creates a WebApplicationException response from a simple text message, exception and status
     *
     * @param msg text message
     * @param ex exception
     * @param status HTTP status of the response
     * @return WebApplicationException response
     */
    public static WebApplicationException createWebAppException(String msg, Exception ex, Response.Status status) {
        Logger.debug(8, "ItemUtils.createWebAppException() - msg:"+ msg + "status:" + status);

        if (Gateway.getProperties().getBoolean("REST.Debug.errorsWithBody", false)) {
            StringBuffer sb = new StringBuffer(msg);

            if(ex != null) sb.append(" - Exception:" + ex.getMessage());

            return new WebApplicationException(sb.toString(), Response.status(status).entity(msg).build());
        }
        else {
            return new WebApplicationException(msg, status);
        }
    }
}
