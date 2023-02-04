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
package org.cristalise.kernel.entity.proxy;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.property.BuiltInItemProperties.AGGREGATE_SCRIPT_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.MASTER_SCHEMA_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.SCHEMA_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.SCRIPT_URN;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.BuiltInCollections;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.Item;
import org.cristalise.kernel.entity.ItemVerticle;
import org.cristalise.kernel.entity.ItemVertxEBProxy;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.WriteProperty;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.OutcomeAttachment;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.BuiltInItemProperties;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.security.SecurityManager;
import org.cristalise.kernel.utils.LocalObjectLoader;

import com.google.errorprone.annotations.Immutable;

import io.vertx.core.AsyncResult;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * It is a wrapper for the connection and communication with Item. It relies on the
 * ClusterStorage mechanism to retrieve and to cache data, i.e. it does not do any cashing itself.
 */
@Slf4j @Immutable @EqualsAndHashCode
public class ItemProxy {
    @EqualsAndHashCode.Exclude 
    protected Item mItem = null;

    protected ItemPath mItemPath;

    /**
     * Set Transaction key (a.k.a. transKey/locker) when ItemProxy is used in server side scripting
     */
    @Getter @EqualsAndHashCode.Exclude 
    protected TransactionKey transactionKey = null;

    /**
     *
     * @param itemPath
     */
    protected ItemProxy(ItemPath itemPath) {
        this(itemPath, null);
    }

    protected ItemProxy(ItemPath itemPath, TransactionKey transKey) {
        mItemPath  = itemPath;
        transactionKey = transKey;
    }

    public Item getItem() {
        if (mItem == null) mItem = new ItemVertxEBProxy(Gateway.getVertx(), ItemVerticle.ebAddress);

        return mItem;
    }

    /**
     * Return the ItemPath object of the Item this proxy is linked with
     * @return the ItemPath of the Item
     */
    public ItemPath getPath() {
        return mItemPath;
    }

    /**
     * Returns the UUID string of the Item this proxy is linked with
     * @return UUID string of the Item
     */
    public String getUuid() {
        return mItemPath.getName();
    }

    /**
     * Sets the value of the given Property
     *
     * @param agent the Agent who is setting the Property
     * @param name the name of the Property
     * @param value the value of the Property
     * @throws AccessRightsException Agent does not the rights to execute this operation
     * @throws PersistencyException there was a database problems during this operations
     * @throws InvalidDataException data was invalid
     */
    public void setProperty(AgentProxy agent, String name, String value)
            throws AccessRightsException, PersistencyException, InvalidDataException
    {
        try {
            String[] params = {name, value};
            agent.execute(this, WriteProperty.class.getSimpleName(), params);
        }
        catch (AccessRightsException | PersistencyException | InvalidDataException e) {
            throw (e);
        }
        catch (Exception e) {
            log.error("Could not store property {}", name, e);
            throw new PersistencyException("Could not store property:"+e.getMessage());
        }
    }

    /**
     * 
     * @param result
     * @param futureResult
     */
    private void asyncHandleRequestAction(AsyncResult<String> result, CompletableFuture<String> futureResult) {
        if (result.succeeded()) {
            String returnString = result.result();
            log.trace("handleRequestAction() - return:{}", returnString);
            futureResult.complete(returnString);
        }
        else {
            futureResult.completeExceptionally(result.cause());
        }
    }

    /**
     * 
     * @param itemUuid
     * @param agentUuid
     * @param stepPath
     * @param transitionID
     * @param requestData
     * @param fileName
     * @param attachment
     * @return
     * @throws CriseVertxException
     */
    public String requestAction(
            String     itemUuid,
            String     agentUuid,
            String     stepPath,
            int        transitionID,
            String     requestData,
            String     fileName,
            List<Byte> attachment
        ) throws CriseVertxException
    {
        log.debug("requestAction() - item:{} agent:{} stepPath:{}", this, agentUuid, stepPath);

        try {
            CompletableFuture<String> futureResult = new CompletableFuture<>();

            Thread thread = new Thread("requestAction-"+this) {
                public void run() {
                    getItem().requestAction(
                            itemUuid,
                            agentUuid,
                            stepPath,
                            transitionID,
                            requestData,
                            fileName,
                            attachment,
                            (result) -> { asyncHandleRequestAction(result, futureResult); }
                    );
                }
            };

            thread.start();

            return futureResult.get(ItemVerticle.requestTimeout, SECONDS);
        }
        catch (ExecutionException e) {
            log.error("requestAction() - item:{} agent:{}", this, agentUuid, e);
            throw CriseVertxException.convertFutureException(e);
        }
        catch (Exception e) {
            log.error("requestAction() - item:{} agent:{}", itemUuid, agentUuid, e);
            throw new CannotManageException("Error while waiting for the requestAction() return value item:"+itemUuid+" agent:"+agentUuid+"", e);
        }
    }

    /**
     * Executes the given Job
     *
     * @param thisJob the Job to be executed
     * @return the result of the execution
     * @throws AccessRightsException Agent does not the rights to execute this operation
     * @throws PersistencyException there was a database problems during this operations
     * @throws InvalidDataException data was invalid
     * @throws InvalidTransitionException the Transition cannot be executed
     * @throws ObjectNotFoundException Object not found
     * @throws ObjectAlreadyExistsException Object already exists
     * @throws InvalidCollectionModification Invalid collection
     */
    public String requestAction(Job thisJob) throws CriseVertxException {
        if (thisJob.getAgentPath() == null) throw new InvalidDataException("No Agent specified.");

        String outcome = thisJob.getOutcomeString();

        if (outcome == null) {
            if (thisJob.isOutcomeRequired()) throw new InvalidDataException("Outcome is required.");
            else                             outcome = "";
        }
        
        OutcomeAttachment attachment = thisJob.getAttachment();
        String attachmentFileName = "";
        byte[] attachmentBinary = new byte[0];

        if (attachment != null) {
            attachmentFileName = attachment.getFileName();
            attachmentBinary = attachment.getBinaryData();
        }

        log.debug("requestAction() - executing job:{}", thisJob);

        return requestAction(
                mItemPath.toString(),
                thisJob.getAgentPath().toString(),
                thisJob.getStepPath(),
                thisJob.getTransition().getId(),
                outcome,
                attachmentFileName,
                Arrays.asList(ArrayUtils.toObject(attachmentBinary)));
    }

    /**
     * Get the list of active Jobs of the Item that can be executed by the Agent
     *
     * @param agentPath the Agent requesting the job
     * @return list of active Jobs
     * @throws AccessRightsException Agent does not the rights to execute this operation
     * @throws PersistencyException there was a database problems during this operations
     * @throws ObjectNotFoundException data was invalid
     */
    public List<Job> getJobs(AgentPath agentPath) throws CriseVertxException {
        return getJobsForAgent(agentPath);
    }

    public boolean checkJobForAgent(Job job, AgentPath agentPath) throws CriseVertxException {
        String stepPath = job.getStepPath();
        Activity act = (Activity) getWorkflow().search(stepPath);
        SecurityManager secMan = Gateway.getSecurityManager();
        
        if (secMan.isShiroEnabled()) {
            if (secMan.checkPermissions(agentPath, act, getPath(), null)) {
                return true;
//                try {
//                    j.getTransition().checkPerformingRole(act, agentPath);
//                    return true;
//                }
//                catch (AccessRightsException e) {
//                    // AccessRightsException is thrown if Job requires specific Role that agent does not have
//                    log.debug("checkJobForAgent()", e);
//                }
            }
        }
        else {
            log.warn("checkJobForAgent() - ENABLE Shiro to work with permissions.");
            return true;
        }

        return false;
    }
    /**
     * Returns a set of Jobs for this Agent on this Item. Each Job represents a possible transition of a particular 
     * Activity/Step in the Item's lifecycle.
     *
     * @param agentPath the Agent requesting the jobs
     * @return list of Jobs
     * @throws AccessRightsException Agent does not the rights to execute this operation
     * @throws PersistencyException there was a database problems during this operations
     * @throws ObjectNotFoundException data was invalid
     */
    private List<Job> getJobsForAgent(AgentPath agentPath) throws CriseVertxException {
        List<Job> jobBag = new ArrayList<Job>();

        // Make sure that the latest Jobs and Workflow is used for this calculation
        Gateway.getStorage().clearCache(getPath(), ClusterType.JOB);
        Gateway.getStorage().clearCache(getPath(), ClusterType.LIFECYCLE);

        for (Job j: getJobs().values()) {
            if (checkJobForAgent(j, agentPath)) jobBag.add(j);
        }

        log.debug("getJobsForAgent() - {} returning #{} jobs for agent:{}", this, jobBag.size(), agentPath.getAgentName());
        return jobBag;
    }

    /**
     * Get the list of active Jobs of the Item that can be executed by the Agent
     *
     * @param agent requesting the job
     * @return list of Jobs
     */
    public List<Job> getJobs(AgentProxy agent) throws CriseVertxException {
        return getJobsForAgent(agent.getPath());
    }

    /**
     * Get the list of active Jobs of the Item for the given Activity that can be executed by the Agent.
     * 
     * @param agent requesting the job
     * @param stepPath of the Activity
     * @return list of active Jobs of the Item for the given Activity that can be executed by the Agent
     * @throws CriseVertxException
     */
    public List<Job> getJobs(AgentPath agent, String stepPath) throws CriseVertxException {
        List<Job> resultJobs = new ArrayList<>();

        for (Job job : getJobsForAgent(agent)) {
            if (job.getStepPath().equals(stepPath)) resultJobs.add(job);
        }
        return resultJobs;
    }

    /**
     *
     * @param actName
     * @param agent
     * @return
     * @throws AccessRightsException
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     */
    private Job getJobByName(String actName, AgentPath agent) throws CriseVertxException {
        C2KLocalObjectMap<Job> jobMap = getJobs();

        for (String key: jobMap.keySet()) {
            String stepName = key.split("/")[0]; // key = 'Update/Done'
            if (stepName.equals(actName)) {
                Job job = jobMap.get(key);
                if (job.getTransition().isFinishing() && checkJobForAgent(job, agent)) {
                    return job;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the given built-in Collection exists
     * 
     * @param collection the built-in Collection
     * @return true of Collection exists false otherwise
     * @throws ObjectNotFoundException if Item does not have any Collections at all
     */
    public boolean checkCollection(BuiltInCollections collection) throws ObjectNotFoundException {
        return checkCollection(collection, transactionKey);
    }

    /**
     * Checks if the given built-in Collection exists
     * 
     * @param collection the built-in Collection
     * @param transKey the transaction key
     * @return true of Collection exists false otherwise
     * @throws ObjectNotFoundException if Item does not have any Collections at all
     */
    public boolean checkCollection(BuiltInCollections collection, TransactionKey transKey) throws ObjectNotFoundException {
        return checkCollection(collection.getName(), transKey);
    }

    /**
     * Checks if the given Collection exists
     * 
     * @param collection the name Collection
     * @return true of Collection exists false otherwise
     * @throws ObjectNotFoundException if Item does not have any Collections at all
     */
    public boolean checkCollection(String collection) throws ObjectNotFoundException {
        return checkCollection(collection, transactionKey);
    }

    /**
     * Checks if the given Collection exists
     * 
     * @param collection the name Collection
     * @param transKey the transaction key
     * @return true of Collection exists false otherwise
     * @throws ObjectNotFoundException if Item does not have any Collections at all
     */
    public boolean checkCollection(String collection, TransactionKey transKey) throws ObjectNotFoundException {
        return checkContent(ClusterType.COLLECTION, collection, transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets the current version of the named Collection
     *
     * @param collection The built-in collection
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(BuiltInCollections collection) throws ObjectNotFoundException {
        return getCollection(collection, (Integer)null);
    }

    /**
     * Gets the current version of the named Collection. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     *
     * @param collection The built-in collection
     * @param transKey the transaction key
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(BuiltInCollections collection, TransactionKey transKey) throws ObjectNotFoundException {
        return getCollection(collection, (Integer)null, transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets a numbered version (snapshot) of a collection
     *
     * @param collection The built-in Collection
     * @param version The collection number. Use null to get the 'last' version.
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(BuiltInCollections collection, Integer version) throws ObjectNotFoundException {
        return getCollection(collection, version, transactionKey);
    }

    /**
     * Gets a numbered version (snapshot) of a collection
     *
     * @param collection The built-in Collection
     * @param version The collection number. Use null to get the 'last' version.
     * @param transKey the transaction key
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(BuiltInCollections collection, Integer version, TransactionKey transKey) throws ObjectNotFoundException {
        return getCollection(collection.getName(), version, transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets the last version of the named collection
     *
     * @param collName The collection name
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(String collName) throws ObjectNotFoundException {
        return getCollection(collName, (Integer)null, transactionKey);
    }

    /**
     * Gets the last version of the named collection. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     *
     * @param collName The collection name
     * @param transKey the transaction key
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(String collName, TransactionKey transKey) throws ObjectNotFoundException {
        return getCollection(collName, (Integer)null, transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets a numbered version (snapshot) of a collection
     *
     * @param collName The collection name
     * @param version The collection number. Use null to get the 'last' version.
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(String collName, Integer version) throws ObjectNotFoundException {
        return getCollection(collName, version, transactionKey);
    }

    /**
     * Gets a numbered version (snapshot) of a collection. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     *
     * @param collName The collection name
     * @param version The collection number. Use null to get the 'last' version.
     * @param transKey the transaction key
     * @return the Collection object
     * @throws ObjectNotFoundException objects were not found
     */
    public Collection<?> getCollection(String collName, Integer version, TransactionKey transKey) throws ObjectNotFoundException {
        String verStr = version == null ? "last" : String.valueOf(version);
        return (Collection<?>) getObject(ClusterType.COLLECTION+"/"+collName+"/"+verStr, transKey == null ? transactionKey : transKey);
    }

    /** 
     * Gets the Workflow object of this Item
     *
     * @return the Item's Workflow object
     * @throws ObjectNotFoundException objects were not found
     */
    public Workflow getWorkflow() throws ObjectNotFoundException {
        return getWorkflow(null);
    }

    /**
     * Gets the Workflow object of this Item. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     *
     * @param transKey the transaction key
     * @return the Item's Workflow object
     * @throws ObjectNotFoundException objects were not found
     */
    public Workflow getWorkflow(TransactionKey transKey) throws ObjectNotFoundException {
        return (Workflow)getObject(ClusterType.LIFECYCLE+"/workflow", transKey == null ? transactionKey : transKey);
    }

    /**
     * Check if the given Viewpoint exists
     *
     * @param schemaName the name of the Schema associated with the Viewpoint
     * @param viewName the name of the View
     * @return true if the ViewPoint exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkViewpoint(String schemaName, String viewName) throws ObjectNotFoundException {
        return checkContent(ClusterType.VIEWPOINT+"/"+schemaName, viewName);
    }

    /**
     * Check if the given Viewpoint exists. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     *
     * @param schemaName the name of the Schema associated with the Viewpoint
     * @param viewName the name of the View
     * @param transKey the transaction key
     * @return true if the ViewPoint exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkViewpoint(String schemaName, String viewName, TransactionKey transKey) throws ObjectNotFoundException {
        return checkContent(ClusterType.VIEWPOINT+"/"+schemaName, viewName, transKey == null ? transactionKey : transKey);
    }

    /**
     * Reads the list of existing Viewpoint names for the given schema 
     * 
     * @param schemaName the name of the schema
     * @return array of strings containing the Viewpoint names
     * @throws ObjectNotFoundException Object not found
     */
    public String[] getViewpoints(String schemaName) throws ObjectNotFoundException {
        return getContents(ClusterType.VIEWPOINT+"/"+schemaName);
    }

    /**
     * Reads the list of existing Viewpoint names for the given schema. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     * 
     * @param schemaName the name of the schema
     * @param transKey the transaction key
     * @return array of strings containing the Viewpoint names
     * @throws ObjectNotFoundException Object not found
     */
    public String[] getViewpoints(String schemaName, TransactionKey transKey) throws ObjectNotFoundException {
        return getContents(ClusterType.VIEWPOINT+"/"+schemaName, transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets the named Viewpoint
     *
     * @param schemaName the name of the Schema associated with the Viewpoint
     * @param viewName name if the View
     * @return a Viewpoint object
     * @throws ObjectNotFoundException objects were not found
     */
    public Viewpoint getViewpoint(String schemaName, String viewName) throws ObjectNotFoundException {
        return (Viewpoint)getObject(ClusterType.VIEWPOINT+"/"+schemaName+"/"+viewName);
    }

    /**
     * Gets the named Viewpoint. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     * 
     * @param schemaName the name of the Schema associated with the Viewpoint
     * @param viewName name if the View
     * @param transKey the transaction key
     * @return a Viewpoint object
     * @throws ObjectNotFoundException objects were not found
     */
    public Viewpoint getViewpoint(String schemaName, String viewName, TransactionKey transKey) throws ObjectNotFoundException {
        return (Viewpoint)getObject(ClusterType.VIEWPOINT+"/"+schemaName+"/"+viewName, transKey == null ? transactionKey : transKey);
    }

    /**
     * Check if the given Outcome exists
     *
     * @param schemaName the name of the Schema used to create the Outcome
     * @param schemaVersion the version of the Schema used to create the Outcome
     * @param eventId the id of the Event created when the Outcome was stored
     * @return true if the Outcome exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkOutcome(String schemaName, int schemaVersion, int eventId) throws ObjectNotFoundException {
        return checkOutcome(schemaName, schemaVersion, eventId, transactionKey);
    }

    /**
     * Check if the given Outcome exists. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param schemaName the name of the Schema used to create the Outcome
     * @param schemaVersion the version of the Schema used to create the Outcome
     * @param eventId the id of the Event created when the Outcome was stored
     * @param transKey the transaction key
     * @return true if the Outcome exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkOutcome(String schemaName, int schemaVersion, int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        try {
            TransactionKey tk = transKey == null ? transactionKey : transKey;
            return checkOutcome(LocalObjectLoader.getSchema(schemaName, schemaVersion, tk), eventId, tk);
        }
        catch (InvalidDataException e) {
            log.error("Schema was not found:{}", schemaName, e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    /**
     * Check if the given Outcome exists
     *
     * @param schema the Schema used to create the Outcome
     * @param eventId the id of the Event created when the Outcome was stored
     * @return true if the Outcome exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkOutcome(Schema schema, int eventId) throws ObjectNotFoundException {
        return checkOutcome(schema, eventId, transactionKey);
    }

    /**
     * Check if the given Outcome exists. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     * 
     * @param schema the Schema used to create the Outcome
     * @param eventId the id of the Event created when the Outcome was stored
     * @param transKey transaction key
     * @return true if the Outcome exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkOutcome(Schema schema, int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        return checkContent(ClusterType.OUTCOME+"/"+schema.getName()+"/"+schema.getVersion(), String.valueOf(eventId), transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets the selected Outcome. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param schemaName the name of the Schema of the Outcome
     * @param schemaVersion the version of the Schema of the Outcome
     * @param eventId the event id
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(String schemaName, int schemaVersion, int eventId) throws ObjectNotFoundException {
        return getOutcome(schemaName, schemaVersion, eventId, transactionKey);
    }

    /**
     * Gets the selected Outcome. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param schemaName the name of the Schema of the Outcome
     * @param schemaVersion the version of the Schema of the Outcome
     * @param eventId the event id
     * @param transKey the transaction key
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(String schemaName, int schemaVersion, int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        try {
            TransactionKey tk = transKey == null ? transactionKey : transKey;
            return getOutcome(LocalObjectLoader.getSchema(schemaName, schemaVersion, tk), eventId, tk);
        }
        catch (InvalidDataException e) {
            log.error("Schema was not found:{}", schemaName, e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    /**
     * Gets the selected Outcome,
     *
     * @param schema the Schema used to create the Outcome
     * @param eventId the id of the Event created when the Outcome was stored
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(Schema schema, int eventId) throws ObjectNotFoundException {
        return getOutcome(schema, eventId, transactionKey);
    }

    /**
     * Gets the selected Outcome. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param schema the Schema used to create the Outcome
     * @param eventId the id of the Event created when the Outcome was stored
     * @param transKey the transaction key
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(Schema schema, int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        return (Outcome)getObject(ClusterType.OUTCOME+"/"+schema.getName()+"/"+schema.getVersion()+"/"+eventId, transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets the Outcome selected by the Viewpoint
     *
     * @param view the Viewpoint to be used
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(Viewpoint view) throws ObjectNotFoundException {
        return getOutcome(view, transactionKey);
    }

    /**
     * Gets the Outcome selected by the Viewpoint. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param view the Viewpoint to be used
     * @param transKey the transaction key
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(Viewpoint view, TransactionKey transKey) throws ObjectNotFoundException {
        try {
            return view.getOutcome(transKey == null ? transactionKey : transKey);
        }
        catch (PersistencyException e) {
            log.error("Could not retrieve outcome for view:{}", view, e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    /**
     * Gets the Outcome associated with the Event.
     * 
     * @param event the Event to be used
     * @return the Outcome object
     * @throws ObjectNotFoundException
     */
    public Outcome getOutcome(Event event) throws ObjectNotFoundException {
        return getOutcome(event, transactionKey);
    }

    /**
     * Gets the Outcome associated with the Event. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     * 
     * @param event the Event to be used
     * @param transKey  the transaction key
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public Outcome getOutcome(Event event, TransactionKey transKey) throws ObjectNotFoundException {
        return getOutcome(event.getSchemaName(), event.getSchemaVersion(), event.getID(), transKey);
    }

    /**
     * Check if the given OutcomeAttachment exists
     *
     * @param schema the Schema used to create the Outcome and its OutcomeAttachment
     * @param eventId the id of the Event created when the Outcome and its OutcomeAttachment was stored
     * @return true if the OutcomeAttachment exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkOutcomeAttachment(Schema schema, int eventId) throws ObjectNotFoundException {
        return checkOutcomeAttachment(schema, eventId, transactionKey);
    }

    /**
     * Check if the given OutcomeAttachment exists. This method can be used in server side Script 
     * to find uncommitted changes during the active transaction.
     *
     * @param schema the Schema used to create the Outcome and its OutcomeAttachment
     * @param eventId the id of the Event created when the Outcome and its OutcomeAttachment was stored
     * @param transKey the transaction key
     * @return true if the OutcomeAttachment exist false otherwise
     * @throws ObjectNotFoundException Object not found
     */
    public boolean checkOutcomeAttachment(Schema schema, int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        return checkContent(ClusterType.ATTACHMENT+"/"+schema.getName()+"/"+schema.getVersion(), String.valueOf(eventId), transKey == null ? transactionKey : transKey);
    }

    /**
     * Gets the selected OutcomeAttachment
     *
     * @param schemaName the name of the Schema used to create the Outcome and its OutcomeAttachment
     * @param schemaVersion the version of the Schema of the Outcome
     * @param eventId the event id
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public OutcomeAttachment getOutcomeAttachment(String schemaName, int schemaVersion, int eventId) throws ObjectNotFoundException {
        return getOutcomeAttachment(schemaName, schemaVersion, eventId, transactionKey);
    }

    /**
     * Gets the selected OutcomeAttachment. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param schemaName the name of the Schema used to create the Outcome and its OutcomeAttachment
     * @param schemaVersion the version of the Schema of the Outcome
     * @param eventId the event id
     * @param transKey the transaction key
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public OutcomeAttachment getOutcomeAttachment(String schemaName, int schemaVersion, int eventId, TransactionKey transKey)
            throws ObjectNotFoundException
    {
        try {
            TransactionKey tk = transKey == null ? transactionKey : transKey;
            return getOutcomeAttachment(LocalObjectLoader.getSchema(schemaName, schemaVersion, tk), eventId, tk);
        }
        catch (InvalidDataException e) {
            log.error("Could not retrieve attachment for schema:{}", schemaName, e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    /**
     * Gets the selected OutcomeAttachment
     *
     * @param schema the Schema used to create the Outcome and its OutcomeAttachment
     * @param eventId the id of the Event created when the Outcome and the OutcomeAttachment was stored
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public OutcomeAttachment getOutcomeAttachment(Schema schema, int eventId) throws ObjectNotFoundException {
        return getOutcomeAttachment(schema, eventId, transactionKey);
    }

    /**
     * Gets the selected OutcomeAttachment. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param schema the Schema used to create the Outcome and its OutcomeAttachment
     * @param eventId the id of the Event created when the Outcome and the OutcomeAttachment was stored
     * @param transKey the transaction key
     * @return the Outcome object
     * @throws ObjectNotFoundException object was not found
     */
    public OutcomeAttachment getOutcomeAttachment(Schema schema, int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        return (OutcomeAttachment)getObject(ClusterType.ATTACHMENT+"/"+schema.getName()+"/"+schema.getVersion()+"/"+eventId, transKey == null ? transactionKey : transKey);
    }

    /**
     * Finds the first finishing job with the given name for the given Agent in the workflow.
     *
     * @param actName the name of the Activity to look for
     * @param agent The agent to fetch jobs for
     * @return the JOB object or null if nothing was found
     * @throws AccessRightsException Agent has not rights
     * @throws ObjectNotFoundException objects were not found
     * @throws PersistencyException Error loading the relevant objects
     */
    public Job getJobByName(String actName, AgentProxy agent) throws CriseVertxException {
        return getJobByName(actName, agent.getPath());
    }

    /**
     * Finds the Job with the given Activity and Transition name for the Agent in the Items Workflow
     *
     * @param actName the name of the Activity to look for
     * @param transName the name of the Transition to look for
     * @param agent The AgentProxy to fetch jobs for
     * @return the JOB object or null if nothing was found
     * @throws AccessRightsException Agent has not rights
     * @throws ObjectNotFoundException objects were not found
     * @throws PersistencyException Error loading the relevant objects
     */
    public Job getJobByTransitionName(String actName, String transName, AgentProxy agent) throws CriseVertxException {
        return getJobByTransitionName(actName, transName, agent.getPath());
    }

    /**
     * Finds the Job with the given Activity and Transition name for the Agent in the Items Workflow
     *
     * @param actName the name of the Activity to look for
     * @param transName the name of the Transition to look for
     * @param agentPath The agent to fetch jobs for
     * @return the JOB object or null if nothing was found
     * @throws AccessRightsException Agent has not rights
     * @throws ObjectNotFoundException objects were not found
     * @throws PersistencyException Error loading the relevant objects
     */
    public Job getJobByTransitionName(String actName, String transName, AgentPath agentPath) throws CriseVertxException {
        for (Job job : getJobsForAgent(agentPath)) {
            if (job.getTransition().getName().equals(transName)) {
                if ((actName.contains("/") && job.getStepPath().equals(actName)) || job.getStepName().equals(actName))
                    return job;
            }
        }
        return null;
    }

    /**
     * Query data of the Item located by the ClusterStorage path
     *
     * @param path the ClusterStorage path
     * @return the data in XML form
     * @throws ObjectNotFoundException path was not correct
     */
    public String queryData(String path) throws ObjectNotFoundException {
        return queryData(path, transactionKey);
    }

    /**
     * Query data of the Item located by the ClusterStorage path
     *
     * @param path the ClusterStorage path
     * @param transKey the transaction key
     * @return the data in XML form
     * @throws ObjectNotFoundException path was not correct
     */
    public String queryData(String path, TransactionKey transKey) throws ObjectNotFoundException {
        log.debug("queryData() - {}/{}", mItemPath, path);

        try {
            if (path.endsWith("all")) {
                log.trace("queryData() - listing contents");

                String[] result = Gateway.getStorage().getClusterContents(mItemPath, path.substring(0, path.length()-3), transKey == null ? transactionKey : transKey);
                StringBuffer retString = new StringBuffer();

                for (int i = 0; i < result.length; i++) {
                    retString.append(result[i]);

                    if (i < result.length-1) retString.append(",");
                }
                log.trace("queryData() - retString:{}", retString);
                return retString.toString();
            }
            else {
                C2KLocalObject target = Gateway.getStorage().get(mItemPath, path, transKey == null ? transactionKey : transKey);
                return Gateway.getMarshaller().marshall(target);
            }
        }
        catch (ObjectNotFoundException e) {
            throw e;
        }
        catch (Throwable e) {
            log.error("queryData() - could not read data for path:{}/{}", mItemPath, path, e);
            return "<ERROR>"+e.getMessage()+"</ERROR>";
        }
    }

    /**
     * Check if the data of the Item located by the ClusterStorage path is exist
     *
     * @param path the ClusterStorage path
     * @param name the name of the content to be checked
     * @return true if there is content false otherwise
     * @throws ObjectNotFoundException path was not correct
     */
    public boolean checkContent(String path, String name) throws ObjectNotFoundException {
        return checkContent(path, name, transactionKey);
    }

    /**
     * Check the root content of the given ClusterType
     *
     * @param cluster the type of the cluster
     * @param name the name of the content to be checked
     * @return true if there is content false otherwise
     * @throws ObjectNotFoundException path was not correct
     */
    public boolean checkContent(ClusterType cluster, String name) throws ObjectNotFoundException {
        return checkContent(cluster.getName(), name, transactionKey);
    }

    /**
     * Check the root content of the given ClusterType
     *
     * @param cluster the type of the cluster
     * @param name the name of the content to be checked
     * @param transKey the transaction key
     * @return true if there is content false otherwise
     * @throws ObjectNotFoundException path was not correct
     */
    public boolean checkContent(ClusterType cluster, String name, TransactionKey transKey) throws ObjectNotFoundException {
        return checkContent(cluster.getName(), name, transKey);
    }

    /**
     * Check if the data of the Item located by the ClusterStorage path is exist. This method can be used
     * in server side Script to find uncommitted changes during the active transaction.
     *
     * @param cluster the type of the cluster
     * @param name the name of the content to be checked
     * @param transKey the transaction key
     * @return true if there is content false otherwise
     * @throws ObjectNotFoundException path was not correct
     */
    public boolean checkContent(String path, String name, TransactionKey transKey) throws ObjectNotFoundException {
        String[] contents = getContents(path, transKey == null ? transactionKey : transKey);

        for (String key : contents) {
            if (key.equals(name)) return true;
        }
        return false;
    }

    /**
     * List the root content of the given ClusterType.
     *
     * @param type the type of the cluster
     * @return list of String of the cluster content
     * @throws ObjectNotFoundException Object nt found
     */
    public String[] getContents(ClusterType type) throws ObjectNotFoundException {
        return getContents(type.getName());
    }

    /**
     * List the root content of the given ClusterType. This method can be used in server side Script 
     * to find uncommitted changes during the active transaction.
     *
     * @param type the type of the cluster
     * @param transKey the transaction key
     * @return list of String of the cluster content
     * @throws ObjectNotFoundException Object nt found
     */
    public String[] getContents(ClusterType type, TransactionKey transKey) throws ObjectNotFoundException {
        return getContents(type.getName(), transKey == null ? transactionKey : transKey);
    }

    /**
     * List the content of the cluster located by the cluster path
     *
     * @param path the ClusterStorage path
     * @return list of String of the cluster content
     * @throws ObjectNotFoundException Object not found
     */
    public String[] getContents(String path) throws ObjectNotFoundException {
        return getContents(path, transactionKey);
    }

    /**
     * List the content of the cluster located by the cluster path. This method can be used in server side Script 
     * to find uncommitted changes during the active transaction.
     *
     * @param path the ClusterStorage path
     * @param transKey the transaction key
     * @return list of String of the cluster content
     * @throws ObjectNotFoundException Object not found
     */
    public String[] getContents(String path, TransactionKey transKey) throws ObjectNotFoundException {
        try {
            return Gateway.getStorage().getClusterContents(mItemPath, path, transKey == null ? transactionKey : transKey);
        }
        catch (PersistencyException e) {
            throw new ObjectNotFoundException(e.toString());
        }
    }

    /**
     * Executes the Query in the target database. The query can be any of these type: SQL/OQL/XQuery/XPath/etc.
     *
     * @param query the query to be executed
     * @return the xml result of the query
     * @throws PersistencyException there was a fundamental DB issue
     */
    public String executeQuery(Query query) throws PersistencyException {
        return Gateway.getStorage().executeQuery(query);
    }

    /**
     * Retrieve the C2KLocalObject for the ClusterType
     *
     * @param type the ClusterTyoe
     * @return the C2KLocalObject
     * @throws ObjectNotFoundException the type did not result in a C2KLocalObject
     */
    public C2KLocalObject getObject(ClusterType type) throws ObjectNotFoundException {
        return getObject(type.getName(), transactionKey);
    }

    /**
     * Retrieve the C2KLocalObject for the ClusterType. Actually it returns an instance of C2KLocalObjectMap
     *
     * @param type the ClusterTyoe
     * @return the C2KLocalObjectMap representing all the Object in the ClusterType
     * @throws ObjectNotFoundException the type did not result in a C2KLocalObject
     */
    public C2KLocalObject getObject(ClusterType type, TransactionKey transKey) throws ObjectNotFoundException {
        C2KLocalObjectMap<?> c2kObjMap = (C2KLocalObjectMap<?>) getObject(type.getName(), transKey);
        c2kObjMap.setTransactionKey(transKey);
        return c2kObjMap;
    }

    /**
     * Retrieve the C2KLocalObject for the Cluster path
     *
     * @param path the path to the cluster content
     * @return the C2KLocalObject
     * @throws ObjectNotFoundException the path did not result in a C2KLocalObject
     */
    public C2KLocalObject getObject(String path) throws ObjectNotFoundException {
        return getObject(path, transactionKey);
    }

    /**
     * Retrieve the C2KLocalObject for the Cluster path. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     * 
     * @param path the path to the cluster object
     * @param transKey the transaction key
     * @return the C2KLocalObject
     * @throws ObjectNotFoundException the path did not result in a C2KLocalObject
     */
    public C2KLocalObject getObject(String path, TransactionKey transKey) throws ObjectNotFoundException {
        try {
            return Gateway.getStorage().get(mItemPath, path , transKey == null ? transactionKey : transKey);
        }
        catch( PersistencyException ex ) {
            log.error("getObject() - Exception loading object:{}/{}", mItemPath, path, ex);
            throw new ObjectNotFoundException( ex.toString() );
        }
    }

    /**
     * Retrieves the values of a BuiltInItemProperty
     *
     * @param prop one of the Built-In Item Property
     * @return the value of the property
     * @throws ObjectNotFoundException property was not found
     */
    public String getProperty( BuiltInItemProperties prop ) throws ObjectNotFoundException {
        return getProperty(prop.getName());
    }

    /**
     * Retrieves the values of a BuiltInItemProperty or returns the defaulValue if no Property was found
     * 
     * @param prop one of the Built-In Item Property
     * @param defaultValue the value to be used if no Property was found
     * @return the value or the defaultValue
     */
    public String getProperty(BuiltInItemProperties prop, String defaultValue) {
        return getProperty(prop, defaultValue, transactionKey);
    }

    /**
     * Retrieves the values of a BuiltInItemProperty or returns the defaulValue if no Property was found.
     * This method can be used in server side Script to find uncommitted changes during the active transaction.
     * 
     * @param prop one of the Built-In Item Property
     * @param defaultValue the value to be used if no Property was found
     * @param transKey the transaction key
     * @return the value or the defaultValue
     */
    public String getProperty(BuiltInItemProperties prop, String defaultValue, TransactionKey transKey) {
        return getProperty(prop.getName(), defaultValue, transKey);
    }

    /**
     * Retrieves the values of a named property or returns the defaulValue if no Property was found
     * 
     * @param name of the Item Property
     * @param defaultValue the value to be used if no Property was found
     * @return the value or the defaultValue
     */
    public String getProperty(String name, String defaultValue) {
        return getProperty(name, defaultValue, transactionKey);
    }

    /**
     * Retrieves the value of a named property. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     *
     * @param name of the Item Property
     * @param defaultValue the value to be used if no Property was found
     * @param transKey the transaction key
     * @return the value of the property
     */
    public String getProperty(String name, String defaultValue, TransactionKey transKey) {
        try {
            if (checkProperty(name, transKey)) {
                return getProperty(name, transKey);
            }
        }
        catch(ObjectNotFoundException e) {
            //This line should never happen because of the use of checkProperty()
        }

        return defaultValue;
    }

    /**
     * Retrieves the value of a named property
     *
     * @param name of the Item Property
     * @return the value of the property
     * @throws ObjectNotFoundException property was not found
     */
    public String getProperty( String name ) throws ObjectNotFoundException {
        return getProperty(name, (TransactionKey)null);
    }

    /**
     * 
     * @param name
     * @param transKey
     * @return
     * @throws ObjectNotFoundException
     */
    public String getProperty(String name, TransactionKey transKey) throws ObjectNotFoundException {
        Property prop = (Property)getObject(ClusterType.PROPERTY+"/"+name, transKey == null ? transactionKey : transKey);

        if(prop != null) return prop.getValue();
        else             throw new ObjectNotFoundException("COULD not find property "+name+" from item "+mItemPath);
    }

    /**
     * Check if the given built-in Property exists
     * 
     * @param prop the built-in Property
     * @return true if the Property exist false otherwise
     * @throws ObjectNotFoundException Item does not have any properties at all
     */
    public boolean checkProperty(BuiltInItemProperties prop) throws ObjectNotFoundException {
        return checkProperty(prop, transactionKey);
    }

    /**
     * Check if the given built-in Property exists. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     * 
     * @param prop the built-in Property
     * @param transKey the transaction key
     * @return true if the Property exist false otherwise
     * @throws ObjectNotFoundException Item does not have any properties at all
     */
    public boolean checkProperty(BuiltInItemProperties prop, TransactionKey transKey) throws ObjectNotFoundException {
        return checkProperty(prop.getName(), transKey == null ? transactionKey : transKey);
    }

    /**
     * Check if the given Property exists
     * 
     * @param name of the Property
     * @return true if the Property exist false otherwise
     * @throws ObjectNotFoundException Item does not have any properties at all
     */
    public boolean checkProperty(String name) throws ObjectNotFoundException {
        return checkContent(ClusterType.PROPERTY.getName(), name);
    }

    /**
     * Check if the given Property exists. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     * 
     * @param name of the Property
     * @param transKey the transaction key
     * @return true if the Property exist false otherwise
     * @throws ObjectNotFoundException Item does not have any properties at all
     */
    public boolean checkProperty(String name, TransactionKey transKey) throws ObjectNotFoundException {
        return checkContent(ClusterType.PROPERTY.getName(), name, transKey == null ? transactionKey : transKey);
    }

    /**
     * Get the name of the Item from its Property called Name
     *
     * @return the name of the Item or null if no Name Property exists
     */
    public String getName() {
        return getName(transactionKey);
    }

    /**
     * Get the name of the Item from its Property called Name. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     * 
     * @param transKey the transaction key
     * @return the name of the Item or null if no Name Property exists
     */
    public String getName(TransactionKey transKey) {
        return getProperty(NAME, (String)null, transKey == null ? transactionKey : transKey);
    }

    /**
     * Get the type of the Item from its Property called Type
     *
     * @return the type of the Item or null if no Type Property exists
     */
    public String getType() {
        return getType(transactionKey);
    }

    /**
     * Get the type of the Item from its Property called Type. This method can be used in server 
     * side Script to find uncommitted changes during the active transaction.
     *
     * @param transKey the transaction key
     * @return the type of the Item or null if no Type Property exists
     */
    public String getType(TransactionKey transKey) {
        return getProperty(TYPE, (String)null, transKey);
    }

    /**
     * Retrieves the Event of the given id.
     * 
     * @param eventId the id of the Event
     * @return the Event object
     * @throws ObjectNotFoundException there is no event for the given id
     */
    public Event getEvent(int eventId) throws ObjectNotFoundException {
        return getEvent(eventId, transactionKey);
    }

    /**
     * Retrieves the Event of the given id. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     * 
     * @param eventId the id of the Event
     * @param transKey the transaction key
     * @return the Event object
     * @throws ObjectNotFoundException there is no event for the given id
     */
    public Event getEvent(int eventId, TransactionKey transKey) throws ObjectNotFoundException {
        return (Event) getObject(HISTORY + "/" + eventId, transKey == null ? transactionKey : transKey);
    }

    /**
     * Retrieves the History of the item.
     * 
     * @return the History object
     * @throws ObjectNotFoundException there is no event for the given id
     */
    public History getHistory() throws ObjectNotFoundException {
        return getHistory(null);
    }

    /**
     * Retrieves the History of the item. This method can be used in server side Script to find uncommitted changes
     * during the active transaction.
     * 
     * @param transKey the transaction key
     * @return the History object
     * @throws ObjectNotFoundException there is no event for the given id
     */
    public History getHistory(TransactionKey transKey) throws ObjectNotFoundException {
        return (History) getObject(HISTORY, transKey == null ? transactionKey : transKey);
    }

    /**
     * Retrieves single persistent Job.
     * 
     * @param id of the persistent Job
     * @return persistent Job of the Item
     * @throws ObjectNotFoundException there is no persistent Job for the given id
     */
    public Job getJob(String id) throws ObjectNotFoundException {
        return getJob(id, null);
    }

    /**
     * Retrieves single persistent Job. This method can be used in server side Script 
     * to find uncommitted changes during the active transaction.
     * 
     * @param id of the Job
     * @param transKey the transaction key
     * @return persistent Job of the Item
     * @throws ObjectNotFoundException there is no Job for the given id
     */
    public Job getJob(String id, TransactionKey transKey) throws ObjectNotFoundException {
        return (Job) getObject(JOB+"/"+id, transKey == null ? transactionKey : transKey);
    }

    /**
     * Retrieves the complete list of Jobs of the Item.
     * 
     * @return C2KLocalObjectMap of Jobs
     */
    public C2KLocalObjectMap<Job> getJobs() {
        return getJobs((TransactionKey)null);
    }

    /**
     * Retrieves the complete list of Jobs of the Item. This method can be used in server side Script 
     * to find uncommitted changes during the active transaction.
     * 
     * @param transKey the transaction key
     * @return C2KLocalObjectMap of Jobs
     */
    @SuppressWarnings("unchecked")
    public C2KLocalObjectMap<Job> getJobs(TransactionKey transKey) {
        try {
            return (C2KLocalObjectMap<Job>) getObject(JOB, transKey == null ? transactionKey : transKey);
        }
        catch (ObjectNotFoundException e) {
            //This case should never happen
            return new C2KLocalObjectMap<Job>(mItemPath, JOB, transactionKey);
        }
    }

    /**
     * Returns the so called Master Schema which can be used to construct master outcome.
     * 
     * @return the actual Schema
     * @throws InvalidDataException the Schema could not be constructed
     * @throws ObjectNotFoundException no Schema was found for the name and version
     */
    public Schema getMasterSchema() throws InvalidDataException, ObjectNotFoundException {
        return getMasterSchema(null, null);
    }

    /**
     * Returns the so called Master Schema which can be used to construct master outcome.
     * 
     * @param schemaName the name or UUID of the Schema or can be blank. It overwrites the master schema settings in the Properties
     * @param schemaVersion the version of the schema or can be null. It overwrites the master schema settings in the Properties
     * @return the Schema
     * @throws InvalidDataException the Schema could not be constructed
     * @throws ObjectNotFoundException no Schema was found for the name and version
     */
    public Schema getMasterSchema(String schemaName, Integer schemaVersion) throws InvalidDataException, ObjectNotFoundException {
        String masterSchemaUrn = getProperty(MASTER_SCHEMA_URN, null);
        if (StringUtils.isBlank(masterSchemaUrn)) masterSchemaUrn = getProperty(SCHEMA_URN, null);

        if (StringUtils.isBlank(schemaName)) {
            if (StringUtils.isNotBlank(masterSchemaUrn)) schemaName = masterSchemaUrn.split(":")[0];
            else                                         schemaName = getType();
        }

        if (schemaVersion == null) {
            if (StringUtils.isBlank(masterSchemaUrn)) {
                if (Gateway.getProperties().getBoolean("Module.Versioning.strict", false)) {
                    throw new InvalidDataException("Version for Schema '" + schemaName + "' cannot be null");
                }
                else {
                    log.warn("getMasterSchema() - Version for Schema '{}' was null, using version 0 as default", schemaName);
                    schemaVersion = 0;
                }
            }
            else {
                schemaVersion = Integer.valueOf(masterSchemaUrn.split(":")[1]);
            }
        }

        return LocalObjectLoader.getSchema(schemaName, schemaVersion, transactionKey);
    }

    /**
     * Returns the so called Aggregate Script which can be used to construct master outcome.
     * 
     * @return the script or null
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     */
    public Script getAggregateScript() throws InvalidDataException, ObjectNotFoundException {
        return getAggregateScript(null, null);
    }

    /**
     * Returns the so called Aggregate Script which can be used to construct master outcome.
     * 
     * @param scriptName the name of the script received in the rest call (can be null)
     * @param scriptVersion the version of the script received in the rest call (can be null)
     * @return the script or null
     * @throws InvalidDataException 
     * @throws ObjectNotFoundException 
     */
    public Script getAggregateScript(String scriptName, Integer scriptVersion) throws InvalidDataException, ObjectNotFoundException {
        String aggregateScriptUrn = getProperty(AGGREGATE_SCRIPT_URN, null);
        if (StringUtils.isBlank(aggregateScriptUrn)) aggregateScriptUrn = getProperty(SCRIPT_URN, null);

        if (StringUtils.isBlank(scriptName)) {
            if (StringUtils.isBlank(aggregateScriptUrn)) scriptName = getType() + "_Aggregate";
            else                                         scriptName = aggregateScriptUrn.split(":")[0];
        }

        if (scriptVersion == null) {
            if (StringUtils.isBlank(aggregateScriptUrn)) {
                if (Gateway.getProperties().getBoolean("Module.Versioning.strict", false)) {
                    throw new InvalidDataException("Version for Script '" + scriptName + "' cannot be null");
                }
                else {
                    log.warn("getAggregateScript() - Version for Script '{}' was null, using version 0 as default", scriptName);
                    scriptVersion = 0;
                }
            }
            else {
                scriptVersion = Integer.valueOf(aggregateScriptUrn.split(":")[1]);
            }
        }

        return LocalObjectLoader.getScript(scriptName, scriptVersion, transactionKey);
    }

    public String marshall(Object obj) throws Exception {
        return Gateway.getMarshaller().marshall(obj);
    }

    public Object unmarshall(String obj) throws Exception {
        return Gateway.getMarshaller().unmarshall(obj);
    }

    public void clearCache() {
        Gateway.getStorage().clearCache(mItemPath);
    }

    @Override
    public String toString() {
        if (log.isTraceEnabled()) {
            return this.getName()+"("+this.getPath().getUUID()+"/"+getType()+")";
        }
        else {
            return this.getName();
        }
    }
}
