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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML_TYPE;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
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
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
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

    protected static URI getItemURI(UriInfo uri, ItemProxy item, Object...path) {
        return getItemURI(uri, item.getPath(), path);
    }

    protected static URI getItemURI(UriInfo uri, ItemPath item, Object...path) {
        return getItemURI(uri, item.getUUID(), path);
    }

    protected static URI getItemURI(UriInfo uri, UUID item, Object...path) {
        UriBuilder builder = uri.getBaseUriBuilder().path("item").path(item.toString());

        for (Object name: path) builder.path(name.toString());

        return builder.build();
    }

    protected static URI getItemURI(UriInfo uri, Object...segments) {
        UriBuilder builder = uri.getBaseUriBuilder().path("item");

        for (Object path: segments) builder.path(path.toString());

        return builder.build();
    }

    protected static ArrayList<LinkedHashMap<String, String>> getPropertySummary(ItemProxy item) throws ObjectNotFoundException {
        ArrayList<LinkedHashMap<String, String>> props = new ArrayList<>();

        for (String propName : item.getContents(PROPERTY)) {
            LinkedHashMap<String, String> prop = new LinkedHashMap<>();

            prop.put("name", propName);
            prop.put("value", item.getProperty(propName));

            props.add(prop);
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

    public Response getViewpointOutcome(String uuid, String schema, String viewName, boolean json) {
        ItemProxy item = ItemRoot.getProxy(uuid);
        try {
            Viewpoint view = item.getViewpoint(schema, viewName);
            return getOutcomeResponse(view.getOutcome(), json);
        }
        catch (PersistencyException | ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), e, Response.Status.NOT_FOUND);
        }
    }

    public Response getOutcome(String uuid, String schema, int version, int eventId, boolean json) {
        ItemProxy item = ItemRoot.getProxy(uuid);
        try {
            Outcome outcome = item.getOutcome(schema, version, eventId);
            return getOutcomeResponse(outcome, (Event)RemoteMapAccess.get(item, HISTORY, Integer.toString(eventId)), json);
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    public ArrayList<LinkedHashMap<String, Object>> enumerate(ItemProxy item, ClusterType cluster, String uriPath, UriInfo uri) {
        return enumerate(item, cluster.getName(), uriPath, uri);
    }

    public ArrayList<LinkedHashMap<String, Object>> enumerate(ItemProxy item, String dataPath, String uriPath, UriInfo uri) {
        try {
            String[] children = item.getContents(dataPath);
            ArrayList<LinkedHashMap<String, Object>> childrenData = new ArrayList<>();

            for (String childName: children) {
                LinkedHashMap<String, Object> childData = new LinkedHashMap<>();

                childData.put("name", childName);
                childData.put("url", getItemURI(uri, item, uriPath, childName));

                childrenData.add(childData);
            }

            return childrenData;
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Database Error");
        }
    }

    protected ArrayList<LinkedHashMap<String, Object>> getAllViewpoints(ItemProxy item, UriInfo uri) {
        ArrayList<LinkedHashMap<String, Object>> viewPoints = enumerate(item, VIEWPOINT, "viewpoint", uri);

        for(LinkedHashMap<String, Object> vp: viewPoints) {
            String schema = vp.get("name").toString();
            vp.put("views", enumerate(item, VIEWPOINT+"/"+schema, "viewpoint"+"/"+schema, uri));
        }

        return viewPoints;
    }

    /**
     * Creates Response with without any specific header.
     * 
     * @param oc the Outcome to convert
     * @param json produce json or xml
     * @return the ws Response
     */
    protected Response getOutcomeResponse(Outcome oc, boolean json) {
        String result;

        if(json) result = XML.toJSONObject(oc.getData()).toString();
        else     result = oc.getData();
        
        //Perhaps header 'Cache-Control: no-cache' should be used.
//        CacheControl cc = new CacheControl();
//        cc.setMaxAge(300);
//        cc.setPrivate(true);
//        cc.setNoStore(true);

        return Response.ok(result)./*cacheControl(cc).*/build();
    }

    /**
     * Creates Response with header 'Last-Modified: <evnt date>'
     * 
     * @param oc the Outcome to convert
     * @param json produce json or xml
     * @return the ws Response
     */
    protected Response getOutcomeResponse(Outcome oc, Date eventDate, boolean json) {
        String result;

        if(json) result = XML.toJSONObject(oc.getData()).toString();
        else     result = oc.getData();

        return Response.ok(result).lastModified(eventDate).build();
    }

    protected Response getOutcomeResponse(Outcome oc, Event ev, boolean json) {
        try {
            Date eventDate = dateFormatter.parse(ev.getTimeString());
            return getOutcomeResponse(oc, eventDate, json);
        }
        catch (ParseException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Invalid timestamp in event "+ev.getID()+": "+ev.getTimeString());
        }
    }

    protected LinkedHashMap<String, Object> makeEventData(Event ev, UriInfo uri) {
        LinkedHashMap<String, Object> eventData = new LinkedHashMap<String, Object>();
        eventData.put("id", ev.getID());
        eventData.put("timestamp", ev.getTimeString());
        eventData.put("agent", ev.getAgentPath().getAgentName());
        eventData.put("role", ev.getAgentRole());

        if (ev.getSchemaName() != null && ev.getSchemaName().length()>0) { // add outcome info
            LinkedHashMap<String, Object> outcomeData = new LinkedHashMap<String, Object>();
            outcomeData.put("name",          ev.getViewName());
            outcomeData.put("schema",        ev.getSchemaName());
            outcomeData.put("schemaVersion", ev.getSchemaVersion());
            //outcomeData.put("schemaData",    uri.getBaseUriBuilder().build("schema", ev.getSchemaName(), ev.getSchemaVersion()));
            outcomeData.put("data",          getItemURI(uri, ev.getItemUUID(), "outcome", ev.getSchemaName(), ev.getSchemaVersion(), ev.getID()));

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
            transData.put("stateMachine", ev.getStateMachineName());
            transData.put("stateMachineVersion", ev.getStateMachineVersion());
            //transData.put("stateMachineData", uri.getBaseUriBuilder().path("stateMachine").path(ev.getStateMachineName()).path(String.valueOf(ev.getStateMachineVersion())).build());
            eventData.put("transition", transData);
        }
        catch (ObjectNotFoundException e) {
            eventData.put("transition", "ERROR: State Machine "+ev.getStateMachineName()+" v"+ev.getStateMachineVersion()+" not found!");
        }
        catch (InvalidDataException e) {
            eventData.put("transition", "ERROR: State Machine definition "+ev.getStateMachineName()+" v"+ev.getStateMachineVersion()+" not valid!");
        }

        return eventData;
    }

    protected LinkedHashMap<String, Object> makeJobData(Job job, String itemName, UriInfo uri) {
        LinkedHashMap<String, Object> jobData = new LinkedHashMap<String, Object>();

        String agentName = job.getAgentName();
        if (StringUtils.isNotBlank(agentName)) jobData.put("agent", agentName);
        jobData.put("role", job.getAgentRole());

        jobData.put("item",       getJobItemData(job, itemName, uri));
        jobData.put("activity",   getJobActivityData(job, itemName, uri));
        jobData.put("transition", getJobTransitionData(job, itemName, uri));

        if (job.hasOutcome()) jobData.put("outcome", getJobOutcomeData(job, itemName, uri));

        return jobData;
    }

    protected LinkedHashMap<String, Object> getJobItemData(Job job, String itemName, UriInfo uri) {
        LinkedHashMap<String, Object> itemData = new LinkedHashMap<String, Object>();
        itemData.put("uuid", job.getItemUUID());
        itemData.put("name", itemName);
        try {
            String type = job.getItem().getType();
            if (StringUtils.isNotBlank(type)) itemData.put("type", type);
        }
        catch (InvalidDataException e1) {}
        itemData.put("url", getItemURI(uri, job.getItemUUID()));

        return itemData;
    }

    protected LinkedHashMap<String, Object> getJobActivityData(Job job, String itemName, UriInfo uri) {
        LinkedHashMap<String, Object> activityData = new LinkedHashMap<String, Object>();

        activityData.put("name", job.getStepName());
        activityData.put("path", job.getStepPath());
        activityData.put("type", job.getStepType());
        //activityData.put("version", job.getStepTypeVersion); //version is unavailable in Job

        LinkedHashMap<String, Object> activityPropData = new LinkedHashMap<String, Object>();

        for (KeyValuePair actProp : job.getKeyValuePairs()) {
            String key = actProp.getKey();
            String value = job.getActPropString(key);

            if (StringUtils.isNotBlank(value)) activityPropData.put(key, value);
        }
        activityData.put("properties", activityPropData);

        return activityData;
    }

    protected LinkedHashMap<String, Object> getJobTransitionData(Job job, String itemName, UriInfo uri) {
        LinkedHashMap<String, Object> transitionData = new LinkedHashMap<String, Object>();

        Object url = uri.getBaseUriBuilder()
                .path("stateMachine")
                .path(job.getActPropString("StateMachineName"))
                .path(job.getActPropString("StateMachineVersion"))
                .build();

        transitionData.put("name",                job.getTransition().getName());
        transitionData.put("id",                  Integer.valueOf(job.getTransition().getId()));
        transitionData.put("origin",              job.getOriginStateName());
        transitionData.put("target",              job.getTargetStateName());
        transitionData.put("stateMachine",        job.getActPropString("StateMachineName"));
        transitionData.put("stateMachineVersion", job.getActPropString("StateMachineVersion"));
        transitionData.put("stateMachineUrl",     url);

        return transitionData;
    }

    protected LinkedHashMap<String, Object> getJobOutcomeData(Job job, String itemName, UriInfo uri) {
        LinkedHashMap<String, Object> outcomeData = new LinkedHashMap<String, Object>();

        try {
            outcomeData.put("required",      job.isOutcomeRequired());
            outcomeData.put("schema",        job.getSchema().getName());
            outcomeData.put("schemaVersion", job.getSchema().getVersion());
            outcomeData.put("schemaUrl",     uri.getBaseUriBuilder().path("schema").path(job.getSchema().getName()).path(String.valueOf(job.getSchema().getVersion())).build());
        }
        catch (InvalidDataException | ObjectNotFoundException e) {
            Logger.error(e);
            outcomeData.put("schema", "Schema not found");
        }

        return outcomeData;
    }

    protected LinkedHashMap<String, Object> makeCollectionData(Collection<?> coll, UriInfo uri) {
        LinkedHashMap<String, Object> collData = new LinkedHashMap<String, Object>();

        collData.put("name", coll.getName());
        collData.put("version", coll.getVersionName());
        String collType = "Collection";

        if      (coll instanceof Aggregation) collType = "Aggregation";
        else if (coll instanceof Dependency)  collType = "Dependency";

        collData.put("type", collType);
        collData.put("isDescription", coll instanceof CollectionDescription);

        // include class props for dependencies here, not in member
        if (coll instanceof Dependency) {
            Dependency dep = (Dependency)coll;
            addCollectionProps(collData, dep.getProperties(), dep.getClassProps(), true);
        }

        ArrayList<LinkedHashMap<String, Object>> members = new ArrayList<>();

        for (CollectionMember member : coll.getMembers().list) {
            LinkedHashMap<String, Object> thisMemberData = new LinkedHashMap<String, Object>();

            thisMemberData.put("id", member.getID());

            if (member.getItemPath() != null) {
                thisMemberData.put("uuid", member.getItemPath().getUUID());
                thisMemberData.put("url", getItemURI(uri, member.getItemPath()));
            }

            // omit class props for dependencies
            addCollectionProps(thisMemberData, member.getProperties(), member.getClassProps(), coll instanceof Aggregation);

            if (member instanceof AggregationMember) thisMemberData.put("geometry", makeGeoData((AggregationMember)member));

            members.add(thisMemberData);
        }

        collData.put("members", members);
        return collData;
    }

    private LinkedHashMap<String, Integer> makeGeoData(AggregationMember aggMem) {
        LinkedHashMap<String, Integer> geo = new LinkedHashMap<String, Integer>();

        geo.put("x",      aggMem.getCentrePoint().x);
        geo.put("y",      aggMem.getCentrePoint().y);
        geo.put("width",  aggMem.getWidth());
        geo.put("heigth", aggMem.getHeight());

        return geo;
    }

    private void addCollectionProps(LinkedHashMap<String, Object> collData, CastorHashMap props, String classProps, boolean includeClassProps) {
        List<String> classPropList = null;
        if (classProps != null) classPropList = Arrays.asList(classProps.split(","));

        ArrayList<LinkedHashMap<String, Object>> classPropData = new ArrayList<>(), propData = new ArrayList<>();

        for (KeyValuePair prop : props.getKeyValuePairs()) {
            LinkedHashMap<String, Object> propMap = new LinkedHashMap<>();

            propMap.put("name",  prop.getKey());
            propMap.put("value", prop.getValue());

            if (classPropList != null && classPropList.contains(prop.getKey()))  classPropData.add(propMap);
            else                                                                 propData.add(propMap);
        }

        if (classPropData.size() > 0 && includeClassProps) collData.put("classIdentifiers", classPropData);
        if (propData.size() > 0)                           collData.put("properties", propData);
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
        Logger.debug(8, "ItemUtils.createWebAppException() - msg:"+ msg + " status:" + status);

        if (Gateway.getProperties().getBoolean("REST.Debug.errorsWithBody", false)) {
            StringBuffer sb = new StringBuffer(msg);

            if(ex != null) sb.append(" - Exception:" + ex.getMessage());

            return new WebApplicationException(sb.toString(), Response.status(status).entity(msg).build());
        }
        else {
            return new WebApplicationException(msg, status);
        }
    }

    /**
     * Check if the requested media type should be a JSON or XML
     * 
     * @param types the media types requested by the client
     * @return true if the type is JSON, false if it is XML
     */
    public boolean produceJSON(List<MediaType> types) {
        if (types.isEmpty()) return false;

        for (MediaType t: types) {
            if      (t.isCompatible(APPLICATION_XML_TYPE) || t.isCompatible(TEXT_XML_TYPE)) return false;
            else if (t.isCompatible(APPLICATION_JSON_TYPE))                                 return true;
        }
        
        throw ItemUtils.createWebAppException(
                    "Supported media types: TEXT_XML, APPLICATION_XML, APPLICATION_JSON", 
                    Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * 
     * @param type
     * @return
     */
    public static List<String> getItemNames(Property ...props) {
        PagedResult result = Gateway.getLookup().search(new DomainPath(""), Arrays.asList(props), 0, 1000);

        ArrayList<String> names = new ArrayList<>();

        for (org.cristalise.kernel.lookup.Path path: result.rows) names.add(path.getName());

        Collections.sort(names);

        return names;
    }
}
