/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.entity;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.AGENT_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.OUTCOME_INIT;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.PropertyUtility.getPropertyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeAttachment;
import org.cristalise.kernel.persistency.outcome.OutcomeInitiator;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.KeyValuePair;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 */
@Getter @Setter @Slf4j
public class Job implements C2KLocalObject {
    // Persistent fields
    private ItemPath       itemPath;
    private String         stepName;
    private String         transitionName;
    private String         stepPath;
    private String         stepType;
    private String         agentRole;
    private AgentPath      agentPath;
    private CastorHashMap  actProps = new CastorHashMap();
    private GTimeStamp     creationDate;

    // Non-persistent fields
    private ErrorInfo  error;

    private Outcome           outcome = null;
    private OutcomeAttachment attachment = null;

    /**
     * OutcomeInitiator cache
     */
    static private HashMap<String, OutcomeInitiator> ocInitCache = new HashMap<String, OutcomeInitiator>();

    /**
     * Empty constructor required for Castor
     */
    public Job() {
//        id = -1;
        setCreationDate(DateUtility.getNow());
        setActProps(new CastorHashMap());
    }

    /**
     * Main constructor to create Job during workflow enactment
     */
    public Job(Activity act, ItemPath itemPath, String transition, AgentPath agent, String role)
            throws InvalidDataException, ObjectNotFoundException
    {
        this();
        setItemPath(itemPath);
        setStepPath(act.getPath());
        setTransitionName(transition);
        setStepName(act.getName());
        setStepType(act.getType());
        setAgentPath(agent);
        setAgentRole(role);

        setActPropsAndEvaluateValues(act);

        getItem();
    }

    /**
     * Constructor for recreating Job from backend
     */
    public Job(ItemPath itemPath, String stepName, String stepPath, String stepType, String transition,
            String agentRole, CastorHashMap actProps, GTimeStamp creationDate)
    {
        this();
        setItemPath(itemPath);
        setStepName(stepName);
        setStepPath(stepPath);
        setStepType(stepType);
        setTransitionName(transition);
        setAgentRole(agentRole);
        setActProps(actProps);
        setCreationDate(creationDate);
    }

    public void setItemPath(ItemPath path) {
        itemPath = path;
    }

    public void setItemUUID( String uuid ) throws InvalidItemPathException {
        setItemPath(new ItemPath(uuid));
    }

    public String getItemUUID() {
        return getItemPath().getUUID().toString();
    }

    public ItemProxy getItem() throws InvalidDataException {
        try {
            return getItemProxy();
        }
        catch (InvalidItemPathException | ObjectNotFoundException e) {
            throw new InvalidDataException(e);
        }
    }

    public Transition getTransition() {
        try {
            StateMachine sm = LocalObjectLoader.getStateMachine(actProps);
            return sm.getTransition(transitionName);
        }
        catch (Exception e) {
            log.error("Cannot retrieve state machine for actProps:{}", actProps, e);
            return null;
        }
    }

    /**
     * Used by castor to unmarshall from XML
     * 
     * @param uuid the string representation of UUID
     * @throws InvalidItemPathException Cannot set UUID of agent
     * @throws InvalidAgentPathException 
     */
    public void setAgentUUID(String uuid) throws InvalidItemPathException {
        if (StringUtils.isBlank(uuid)) agentPath = null; 
        else                           setAgentPath(new AgentPath(uuid));
    }

    /**
    * Used by castor to marshall to XML
     * @return The stringified UUID of Agent
     */
    public String getAgentUUID() {
        if (agentPath != null) return getAgentPath().getUUID().toString();
        else                   return null;
    }

    public String getAgentName() {
        String agentName = null;

        if (agentPath != null) agentName = agentPath.getAgentName();
        if (agentName == null) agentName = (String) actProps.getBuiltInProperty(AGENT_NAME);

        return agentName;
    }

    public Schema getSchema() throws InvalidDataException, ObjectNotFoundException {
        if (getTransition().hasOutcome(actProps)) {
            return getTransition().getSchema(actProps);
        }
        return null;
    }

    public String getSchemaName() throws InvalidDataException, ObjectNotFoundException {
        try {
            return getSchema().getName();
        }
        catch (Exception e) {
            return null;
        }
    }

    public int getSchemaVersion() throws InvalidDataException, ObjectNotFoundException {
        try {
            return getSchema().getVersion();
        }
        catch (Exception e) {
            return -1;
        }
    }
    public boolean isOutcomeRequired() {
        return getTransition().hasOutcome(actProps) && getTransition().getOutcome().isRequired();
    }

    public Script getScript() throws ObjectNotFoundException, InvalidDataException {
        if (getTransition().hasScript(actProps)) {
            return getTransition().getScript(actProps);
        }
        return null;
    }

    public Query getQuery() throws ObjectNotFoundException, InvalidDataException {
        if (hasQuery()) {
            Query query = getTransition().getQuery(actProps);
            query.setParemeterValues(itemPath.getUUID().toString(), getSchemaName(), actProps);
            return query;
        }
        return null;
    }

    @Deprecated
    public String getScriptName() {
        try {
            return getScript().getName();
        }
        catch (Exception e) {
            return null;
        }
    }

    @Deprecated
    public int getScriptVersion() throws InvalidDataException {
        try {
            return getScript().getVersion();
        }
        catch (Exception e) {
            return -1;
        }
    }

    public KeyValuePair[] getKeyValuePairs() {
        return actProps.getKeyValuePairs();
    }

    public void setKeyValuePairs(KeyValuePair[] pairs) {
        actProps.setKeyValuePairs(pairs);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setName(String name) {
        //do nothing
    }

    public ItemProxy getItemProxy() throws ObjectNotFoundException, InvalidItemPathException {
        return Gateway.getProxy(itemPath, null);
    }

    public String getDescription() {
        String desc = (String) actProps.get("Description");
        if (desc == null) desc = "No Description";
        return desc;
    }

    public void setOutcome(String outcomeData) throws InvalidDataException, ObjectNotFoundException {
        setOutcome(new Outcome(-1, outcomeData, getTransition().getSchema(actProps)));
    }

    public void setOutcome(Outcome o) {
        outcome = o;
    }

    public void setError(ErrorInfo errors) {
        error = errors;
        try {
            setOutcome(Gateway.getMarshaller().marshall(error));
        }
        catch (Exception e) {
            log.error("Error marshalling ErrorInfo in job", e);
        } 
    }

    /**
     * Checks the value of the 'Viewpoint' ActivityProperty and return 'last' if value is blank 
     * or starts with 'xpath:'. In all other cases it returns its value.
     * 
     * @return the 'calculated' Viewpoint name
     */
    public String getValidViewpointName() {
        String viewName = getActPropString("Viewpoint");

        if(StringUtils.isBlank(viewName) || viewName.startsWith("xpath:")) {
            viewName = "last";
        }

        log.debug("getValidViewpointName() - returning Viewpoint:'{}'", viewName);

        return viewName;
    }

    /**
     * Returns the Outcome instance associated with the 'last' Viewpoint
     * 
     * @return Outcome instance
     * @throws InvalidDataException inconsistent data or persistency issue
     * @throws ObjectNotFoundException Schema or Outcome was not found
     */
    public Outcome getLastOutcome() throws InvalidDataException, ObjectNotFoundException {
        try {
            return getItemProxy().getViewpoint(getSchema().getName(), getValidViewpointName()).getOutcome();
        }
        catch (PersistencyException | InvalidItemPathException e) {
            log.error("Error loading viewpoint", e);
            throw new InvalidDataException("Error loading viewpoint:"+e.getMessage(), e); 
        }
    }

    /**
     * Returns the Outcome string associated with the 'last' Viewpoint
     * 
     * @return XML data of the last version of Outcome
     * @throws InvalidDataException inconsistent data or persistency issue
     * @throws ObjectNotFoundException Schema or Outcome was not found
     */
    public String getLastView() throws InvalidDataException, ObjectNotFoundException {
        return getLastOutcome().getData();
    }

    /**
     * Retrieve the OutcomeInitiator associated with this Job.
     * 
     * @see BuiltInVertexProperties#OUTCOME_INIT
     * 
     * @return the configured OutcomeInitiator
     * @throws InvalidDataException OutcomeInitiator could not be created
     */
    public OutcomeInitiator getOutcomeInitiator() throws InvalidDataException {
        String ocInitName = getActPropString(OUTCOME_INIT);

        if (StringUtils.isNotBlank(ocInitName)) {
            String ocConfigPropName = OUTCOME_INIT.getName()+"."+ocInitName;
            OutcomeInitiator ocInit;

            synchronized (ocInitCache) {
                log.debug("Job.getOutcomeInitiator() - ocConfigPropName:{}", ocConfigPropName);
                ocInit = ocInitCache.get(ocConfigPropName);

                if (ocInit == null) {
                    if (!Gateway.getProperties().containsKey(ocConfigPropName)) {
                        throw new InvalidDataException("Property OutcomeInstantiator "+ocConfigPropName+" isn't defined. Check module.xml");
                    }

                    try {
                        ocInit = (OutcomeInitiator)Gateway.getProperties().getInstance(ocConfigPropName);
                        ocInitCache.put(ocConfigPropName, ocInit);
                    }
                    catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                        log.error("OutcomeInstantiator {} couldn't be instantiated", ocConfigPropName, e);
                        throw new InvalidDataException("OutcomeInstantiator "+ocConfigPropName+" couldn't be instantiated:"+e.getMessage());
                    }
                }
            }
            return ocInit;
        }
        else
            return null;
    }

    /**
     * Returns the Outcome string. It is based on {@link Job#getOutcome()}
     * 
     * @return the Outcome xml or null
     * @throws InvalidDataException inconsistent data
     */
    public String getOutcomeString() throws InvalidDataException, ObjectNotFoundException {
        if(outcome != null) { 
            return outcome.getData();
        }
        else {
            getOutcome();

            //getOutcome() could return a null object
            if(outcome != null) return outcome.getData();
        }
        return null;
    }

    /**
     * Returns the Outcome if exists otherwise tries to read and duplicate the Outcome of 'last' ViewPoint. 
     * If that does not exists it tries to use an OutcomeInitiator.
     * 
     * @return the Outcome object or null
     * @throws InvalidDataException inconsistent data
     * @throws ObjectNotFoundException Schema was not found
     */
    public Outcome getOutcome() throws InvalidDataException, ObjectNotFoundException {
        if (outcome == null && hasOutcome()) {
            boolean useViewpoint = Gateway.getProperties().getBoolean("OutcomeInit.jobUseViewpoint", false);

            // check viewpoint first if exists
            if (useViewpoint) {
                if (getItem().checkViewpoint(getSchema().getName(), getValidViewpointName())) {
                    Outcome tempOutcome = getLastOutcome();
                    outcome = new Outcome(tempOutcome.getData(), tempOutcome.getSchema());
                }
            }

            if (outcome == null) {
                // try outcome initiator
                OutcomeInitiator ocInit = getOutcomeInitiator();

                if (ocInit != null) outcome = ocInit.initOutcomeInstance(this);
            }

            if (outcome == null) log.warn("getOutcome() - Could not initilase Outcome for Job:{}", this);
        }
        else if (!hasOutcome()) {
            log.debug("getOutcome() - No Outcome description for Job:{}", this);
        }

        return outcome;
    }

    public boolean hasOutcome() {
        return getTransition().hasOutcome(actProps);
    }

    public boolean hasScript() {
        return getTransition().hasScript(actProps);
    }

    public boolean hasQuery() {
        return getTransition().hasQuery(actProps);
    }

    public boolean isOutcomeSet() {
        return outcome != null;
    }

    @Override
    public ClusterType getClusterType() {
        return ClusterType.JOB;
    }

    @Override
    public String getClusterPath() {
        return getClusterType()+"/"+getStepName()+"/"+getTransition().getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((itemPath == null)       ? 0 : itemPath.hashCode());
        result = prime * result + ((stepPath == null)       ? 0 : stepPath.hashCode());
        result = prime * result + ((transitionName == null) ? 0 : transitionName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)                  return true;
        if (other == null)                  return false;
        if (getClass() != other.getClass()) return false;

        Job otherJob = (Job) other;

        if (itemPath == null) if (otherJob.itemPath != null) return false;
        else if (!itemPath.equals(otherJob.itemPath))        return false;

        if (stepPath == null) if (otherJob.stepPath != null) return false;
        else if (!stepPath.equals(otherJob.stepPath))        return false;

        if (transitionName == null) if (otherJob.transitionName != null) return false;
        else if (!transitionName.equals(otherJob.transitionName))        return false;

        return true;
    }

    /**
     * 
     * @param act
     * @throws InvalidDataException 
     */
    private void setActPropsAndEvaluateValues(Activity act) throws InvalidDataException {
        setActProps(act.getProperties());
        
        List<String> errors = new ArrayList<String>();

        for(Map.Entry<String, Object> entry: act.getProperties().entrySet()) {
            try {
                Object newVal = act.evaluatePropertyValue(null, entry.getValue(), null);
                if(newVal != null) actProps.put(entry.getKey(), newVal);
            }
            catch (InvalidDataException | PersistencyException | ObjectNotFoundException e) {
                log.error("setActPropsAndEvaluateValues() - error evaluating act property '{}:{}'", entry.getKey(), entry.getValue(), e);
                errors.add(e.getMessage());
            }
        }

        if(errors.size() != 0) {
            StringBuffer buffer = new StringBuffer();
            for(String msg: errors) buffer.append(msg);
            throw new InvalidDataException(buffer.toString());
        }
    }

    private void setActProps(CastorHashMap actProps) {
        this.actProps = actProps;
    }

    public Object getActProp(String name) {
        return actProps.get(name);
    }

    public Object getActProp(String name, Object defaultValue) {
        Object value = getActProp(name);
        return (value == null) ? defaultValue : value;
    }

    public Object getActProp(BuiltInVertexProperties name) {
        return getActProp(name.getName());
    }

    public Object getActProp(BuiltInVertexProperties name, Object defaultValue) {
        return getActProp(name.getName(), defaultValue);
    }

    public String getActPropString(String name) {
        Object obj = getActProp(name);
        return obj == null ? null : String.valueOf(obj);
    }

    public void setActProp(BuiltInVertexProperties prop, Object value) {
        actProps.setBuiltInProperty(prop, value);
    }

    public void setActProp(String name, Object value) {
        actProps.put(name, value);
    }

    public String getActPropString(BuiltInVertexProperties name) {
        return getActPropString(name.getName());
    }

    /**
     * Searches Activity property names using {@link String#startsWith(String)} method
     * 
     * @param pattern the pattern to be matched
     * @return Map of property name and value
     */
    public Map<String, Object> matchActPropNames(String pattern) {
        Map<String, Object> result = new HashMap<String, Object>();

        for(String propName : actProps.keySet()) {
            if(propName.startsWith(pattern)) result.put(propName, actProps.get(propName));
            //if(propName.matches(pattern)) result.put(propName, actProps.get(propName));
        }

        if(result.size() == 0) {
            log.debug("matchActPropNames() - NO properties were found for propName.startsWith(pattern:'{}')", pattern);
            log.trace("matchActPropNames() - actProps:", actProps);
        }

        return result;
    }

    @Override
    public String toString() {
        //enable to use toString even if Lookup is not configured in Gateway
        String agent = agentPath.toString();
        if (Gateway.getLookup() != null) agent = agentPath.getAgentName();

        //enable to use toString even if ClusterStorage is not configured in Gateway
        String item = itemPath.toString();
        if (Gateway.getStorage() != null) item = getPropertyValue(itemPath, NAME, item, null);

        return "[item:"+item+" step:"+stepName+" trans:"+getTransition()+" agent:"+agent+"]";
    }
}