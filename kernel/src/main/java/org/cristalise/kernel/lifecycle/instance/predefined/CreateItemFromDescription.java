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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.apache.commons.lang3.StringUtils.equalsAny;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE;
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;
import static org.cristalise.kernel.property.BuiltInItemProperties.CREATOR;
import static org.cristalise.kernel.property.BuiltInItemProperties.ID_PREFIX;
import static org.cristalise.kernel.property.BuiltInItemProperties.LAST_COUNT;
import static org.cristalise.kernel.property.BuiltInItemProperties.LEFT_PAD_SIZE;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.PropertyUtility.getPropertyDescriptionOutcome;
import static org.cristalise.kernel.property.PropertyUtility.getPropertyValue;
import static org.cristalise.kernel.property.PropertyUtility.writeProperty;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateItemFromDescription extends PredefinedStep {

    /**
     * Use this constant to enforce the factory to generate the name
     */
    public static final String FACTORY_GENERATED_NAME = "FACTORY_GENERATED";

    public CreateItemFromDescription() {
        super();
    }

    /**
     * Params:
     * <ol>
     * <li>Item name</li>
     * <li>Domain context</li>
     * <li>Description version to use(optional)</li>
     * <li>Initial properties to set in the new Agent (optional)</li>
     * </ol>
     * @throws ObjectNotFoundException
     * @throws InvalidDataException The input parameters were incorrect
     * @throws ObjectAlreadyExistsException The Agent already exists
     * @throws CannotManageException The Agent could not be created
     * @throws ObjectCannotBeUpdated The addition of the new entries into the LookupManager failed
     * @throws PersistencyException
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath descItemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException,
                   ObjectNotFoundException,
                   ObjectAlreadyExistsException,
                   CannotManageException,
                   ObjectCannotBeUpdated,
                   PersistencyException
    {
        String[] inputs = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), descItemPath, (Object)inputs);

        String            newName   = getItemName(descItemPath, inputs[0], transactionKey);
        String            domPath   = inputs[1];
        String            descVer   = inputs.length > 2 && isNotBlank(inputs[2]) ? inputs[2] : "last";
        PropertyArrayList initProps = inputs.length > 3 && isNotBlank(inputs[3]) ? unmarshallInitProperties(inputs[3]) : new PropertyArrayList();
        String            outcome   = inputs.length > 4 && isNotBlank(inputs[4]) ? inputs[4] : "";

        // check if the path is already taken
        DomainPath context = new DomainPath(new DomainPath(domPath), newName);
        if (context.exists(transactionKey)) throw new ObjectAlreadyExistsException("The path " + context + " exists already.");

        // generate new item path with random uuid
        ItemPath newItemPath = new ItemPath();

        // create the Item object
        log.info("Creating Item name:{} uuid:{} transactionKey:{}", newName, newItemPath, transactionKey);

        Gateway.getLookupManager().add(newItemPath, transactionKey);

        initialiseItem(newItemPath, agent, descItemPath, initProps, outcome, newName, descVer, context, newItemPath, transactionKey);

        // in case of generated name send it back with the update requestData
        inputs[0] = newName;
        return bundleData(inputs);
    }

    /**
     * 
     * @param descItemPath
     * @param newName
     * @param transactionKey
     * @return
     * @throws InvalidDataException
     * @throws ObjectNotFoundException 
     * @throws ObjectCannotBeUpdated 
     * @throws PersistencyException 
     */
    public String getItemName(ItemPath descItemPath, String newName, TransactionKey transactionKey) 
            throws InvalidDataException, PersistencyException, ObjectCannotBeUpdated, ObjectNotFoundException
    {
        // Check if Name is generated
        if (FACTORY_GENERATED_NAME.equals(newName)) {
            try {
                String  prefix  = getPropertyValue(descItemPath, ID_PREFIX, "", transactionKey);
                Integer padSize = Integer.valueOf(getPropertyValue(descItemPath, LEFT_PAD_SIZE, (String)null, transactionKey));

                log.debug("getItemName() - generating name prefix:{}, padSize:{}", prefix, padSize);

                if (isBlank(prefix) || padSize == null) {
                    throw new InvalidDataException("Item:"+descItemPath.getItemName()+" property '"+ID_PREFIX+"' and '"+LEFT_PAD_SIZE+"' must contain value");
                }

                Integer lastCount = Integer.valueOf(getPropertyValue(descItemPath, LAST_COUNT, "0", transactionKey));
                lastCount++;

                writeProperty(descItemPath, LAST_COUNT, lastCount.toString(), transactionKey);

                return prefix + leftPad(lastCount.toString(), padSize, "0");
            }
            catch (NumberFormatException e) {
                String msg = "Item:"+descItemPath.getItemName()+" properties:'"+LAST_COUNT+"','"+LEFT_PAD_SIZE+"' must contain integer value";
                log.error(msg, e);
                throw new InvalidDataException(msg, e);
            }
        }
        else {
            if (isBlank(newName) || equalsAny(newName, "string", "null")) {
                throw new InvalidDataException("Name must be provided");
            }
        }

        return newName;
    }

    /**
     * 
     * @param agent
     * @param descItemPath
     * @param transactionKey
     * @param input
     * @param newName
     * @param descVer
     * @param context
     * @param newItemPath
     * @param newItem
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     * @throws InvalidDataException
     * @throws ObjectAlreadyExistsException
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    protected void initialiseItem(ItemPath          newItem, 
                                  AgentPath         agent, 
                                  ItemPath          descItemPath, 
                                  PropertyArrayList initProps,
                                  String            outcome,
                                  String            newName, 
                                  String            descVer,
                                  DomainPath        context, 
                                  ItemPath          newItemPath, 
                                  TransactionKey    transactionKey
                                  )
            throws ObjectCannotBeUpdated, 
                   CannotManageException,
                   InvalidDataException, 
                   ObjectAlreadyExistsException, 
                   PersistencyException, 
                   ObjectNotFoundException
    {
        // initialise it with its properties and workflow
        log.info("initialiseItem() - Initializing Item:" + newName);

        try {
            PropertyArrayList   newProps     = instantiateProperties (descItemPath, descVer, initProps, newName, agent, transactionKey);
            CollectionArrayList newColls     = instantiateCollections(descItemPath, descVer, newProps, transactionKey);
            CompositeActivity   newWorkflow  = instantiateWorkflow   (descItemPath, descVer, transactionKey);
            Viewpoint           newViewpoint = instantiateViewpoint  (descItemPath, descVer, transactionKey);

            storeItem(agent, newItem, newProps, newColls, newWorkflow, newViewpoint, outcome, transactionKey);
        }
        catch (InvalidDataException | ObjectNotFoundException | PersistencyException e) {
            if (log.isDebugEnabled()) log.error("initialiseItem()", e);
            Gateway.getLookupManager().delete(newItemPath, transactionKey);
            throw e;
        }

        // add its domain path
        log.info("Creating " + context);
        context.setItemPath(newItemPath);
        Gateway.getLookupManager().add(context, transactionKey);
    }

    /**
     * Unmarshalls initial Properties
     *
     * @param initPropString
     * @return unmarshalled initial PropertyArrayList
     * @throws InvalidDataException
     */
    protected PropertyArrayList unmarshallInitProperties(String initPropString) throws InvalidDataException {
        return (PropertyArrayList) Gateway.getMarshaller().unmarshall(initPropString);
    }

    /**
     *
     * @param descItemPath
     * @param descVer
     * @param initProps
     * @param newName
     * @param agent
     * @param transactionKey
     * @return props
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected PropertyArrayList instantiateProperties(ItemPath descItemPath, String descVer, PropertyArrayList initProps, String newName, AgentPath agent, TransactionKey transactionKey)
            throws ObjectNotFoundException, InvalidDataException
    {
        // copy properties -- intend to create from propdesc
        PropertyDescriptionList pdList = getPropertyDescriptionOutcome(descItemPath, descVer, transactionKey);
        PropertyArrayList       props  = pdList.instantiate(initProps);

        // set Name prop or create if not present
        boolean foundName = false;
        for (Property prop : props.list) {
            if (prop.getName().equals(NAME.toString())) {
                foundName = true;
                prop.setValue(newName);
                break;
            }
        }

        if (!foundName) props.list.add(new Property(NAME, newName, true));
        props.list.add(new Property(CREATOR, agent.getAgentName(transactionKey), false));

        return props;
    }

    /**
     * Retrieve the Workflow dependency for the given description version, instantiate the loaded CompositeActivityDef
     *
     * @param descItemPath
     * @param descVer
     * @param transactionKey
     * @return the Workflow instance
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected CompositeActivity instantiateWorkflow(ItemPath descItemPath, String descVer, TransactionKey transactionKey)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>)
                    Gateway.getStorage().get(descItemPath, COLLECTION + "/" + WORKFLOW + "/" + descVer, transactionKey);

        CollectionMember wfMember  = thisCol.getMembers().list.get(0);
        String           wfDefName = wfMember.resolveItem(transactionKey).getName(transactionKey);
        Object           wfVerObj  = wfMember.getProperties().getBuiltInProperty(VERSION);

        if (wfVerObj == null || String.valueOf(wfVerObj).length() == 0) {
            throw new InvalidDataException("Workflow version number not set");
        }

        try {
            Integer wfDefVer = Integer.parseInt(wfVerObj.toString());

            if (wfDefName == null) throw new InvalidDataException("No workflow given or defined");

            // load workflow def
            CompositeActivityDef wfDef = (CompositeActivityDef) LocalObjectLoader.getActDef(wfDefName, wfDefVer, transactionKey);
            return (CompositeActivity) wfDef.instantiate(transactionKey);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid workflow version number: " + wfVerObj.toString());
        }
        catch (ClassCastException ex) {
            log.error("Activity def '" + wfDefName + "' was not Composite", ex);
            throw new InvalidDataException("Activity def '" + wfDefName + "' was not Composite");
        }
    }

    /**
     * Copies the CollectionDescriptions of the Item requesting this predefined step.
     *
     * @param descItemPath
     * @param descVer
     * @param transactionKey
     * @return the new collection
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws InvalidDataException
     */
    protected CollectionArrayList instantiateCollections(ItemPath descItemPath, String descVer, PropertyArrayList newProps , TransactionKey transactionKey)
            throws ObjectNotFoundException, PersistencyException, InvalidDataException
    {
        // loop through collections, collecting instantiated descriptions and finding the default workflow def
        CollectionArrayList colls = new CollectionArrayList();
        String[] collNames = Gateway.getStorage().getClusterContents(descItemPath, COLLECTION, transactionKey);

        for (String collName : collNames) {
            Collection<?> newColl = instantiateCollection(collName, descItemPath, descVer, newProps, transactionKey);
            if (newColl != null) colls.put(newColl);
        }
        return colls;
    }

    /**
     * 
     * @param collName
     * @param descItemPath
     * @param descVer
     * @param newProps
     * @param transactionKey
     * @return
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public static Collection<?> instantiateCollection(String collName, ItemPath descItemPath, String descVer, PropertyArrayList newProps, TransactionKey transactionKey) 
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> collOfDesc = (Collection<? extends CollectionMember>)
                Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, transactionKey);

        Collection<?> newColl = null;

        if (collOfDesc instanceof CollectionDescription) {
            log.info("Instantiating CollectionDescription:"+ collName);
            CollectionDescription<?> collDesc = (CollectionDescription<?>) collOfDesc;
            newColl = collDesc.newInstance(transactionKey);
        }
        else if(collOfDesc instanceof Dependency) {
            log.info("Instantiating Dependency:"+ collName);
            ((Dependency) collOfDesc).addToItemProperties(newProps, transactionKey);
        }
        else {
            throw new InvalidDataException("CANNOT instantiate collection:"+ collName + " class:"+collOfDesc.getClass().getName());
        }

        return newColl;
    }

    /**
     * 
     * @param descItemPath
     * @param descVer
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected Viewpoint instantiateViewpoint(ItemPath descItemPath, String descVer, TransactionKey transactionKey) 
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        String collPath = COLLECTION + "/" + SCHEMA_INITIALISE;

        if (Gateway.getStorage().getClusterContents(descItemPath, collPath, transactionKey).length == 0) return null;

        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>)
                    Gateway.getStorage().get(descItemPath, collPath + "/" + descVer, transactionKey);

        CollectionMember schemaMember = thisCol.getMembers().list.get(0);
        String           schemaName   = schemaMember.resolveItem(transactionKey).getName();
        Object           schemaVerObj = schemaMember.getProperties().getBuiltInProperty(VERSION);
        Object           viewNameObj  = schemaMember.getProperties().get("View");

        if (schemaName == null) throw new InvalidDataException("No schema given or defined");

        if (schemaVerObj == null || String.valueOf(schemaVerObj).length() == 0) {
            throw new InvalidDataException("schema version number not set");
        }

        try {
            Integer schemaVer = Integer.parseInt(schemaVerObj.toString());
            String viewName = (viewNameObj == null) ? "last": viewNameObj.toString();
            // new ItemPath with random UUID is assigned to make xml marshall work
            return new Viewpoint(new ItemPath(), schemaName, viewName, schemaVer, -1);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid schema version number: " + schemaVerObj.toString());
        }
    }

    /**
     * 
     * @param agent
     * @param item
     * @param props
     * @param initViewpoint
     * @param initOutcomeString
     * @param colls
     * @param ca
     * @param transactionKey
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     * @throws MappingException
     */
    public static void storeItem(AgentPath           agent, 
                                 ItemPath            item, 
                                 PropertyArrayList   props, 
                                 CollectionArrayList colls,
                                 CompositeActivity   ca,
                                 Viewpoint           initViewpoint, 
                                 String              initOutcomeString,
                                 TransactionKey      transactionKey
            )
            throws PersistencyException, 
                   ObjectNotFoundException, 
                   InvalidDataException
    {
        // store properties
        for (Property thisProp : props.list) Gateway.getStorage().put(item, thisProp, transactionKey);

        History hist = new History(item, transactionKey);

        // Store an "Initialize" event and the outcome containing the initial values for properties
        Schema initSchema = LocalObjectLoader.getSchema("ItemInitialization", 0, transactionKey);
        Outcome initOutcome = new Outcome(0, Gateway.getMarshaller().marshall(props), initSchema);
        StateMachine predefSm = LocalObjectLoader.getStateMachine("PredefinedStep", 0, transactionKey);

        Event newEvent = hist.addEvent(agent, "", "Initialize", "", "", initSchema, predefSm, PredefinedStep.DONE, "last");

        initOutcome.setID(newEvent.getID());

        Viewpoint newLastView = new Viewpoint(item, initSchema, "last", newEvent.getID());
        Gateway.getStorage().put(item, initOutcome, transactionKey);
        Gateway.getStorage().put(item, newLastView, transactionKey);

        // Store an "Constructor" event and the outcome containing the "Constructor"
        if (initViewpoint != null) {
            Schema schema = LocalObjectLoader.getSchema(initViewpoint.getSchemaName(), initViewpoint.getSchemaVersion(), transactionKey);
            Outcome outcome = new Outcome(-1, initOutcomeString, schema);
            outcome.validateAndCheck();

            initViewpoint.setItemPath(item);

            Event intiEvent = hist.addEvent(agent, "", "Constructor", "", "", schema, predefSm, PredefinedStep.DONE, initViewpoint.getName());

            initViewpoint.setEventId(intiEvent.getID());
            outcome.setID(intiEvent.getID());

            Gateway.getStorage().put(item, outcome, transactionKey);
            Gateway.getStorage().put(item, initViewpoint, transactionKey);
        }

        // store collections
        if (colls != null) {
            for (Collection<?> thisColl : colls.list) {
                Gateway.getStorage().put(item, thisColl, transactionKey);
            }
        }

        // create wf
        Workflow wf = null;
        PredefinedStepContainer cont = (item instanceof AgentPath) ? new AgentPredefinedStepContainer() : new ItemPredefinedStepContainer();

        if (ca == null) {
            // FIXME check if this could be a real error
            log.warn("storeItem({}) - CompositeActivity was null. Creating workflow with empty domain CompAct.", item);
            wf = new Workflow(new CompositeActivity(), cont);
        }
        else {
            wf = new Workflow(ca, cont);
        }

        // All objects are in place, initialize the workflow
        wf.initialise(item, agent, transactionKey);

        Gateway.getStorage().removeCluster(item, ClusterType.JOB, transactionKey);

        // store the Jobs
        ArrayList<Job> newJobs = ((CompositeActivity)wf.search("workflow/domain")).calculateJobs(agent, item, true);
        for (Job newJob: newJobs) {
            Gateway.getStorage().put(item, newJob, transactionKey);

            if (StringUtils.isNotBlank(newJob.getRoleOverride())) newJob.sendToRoleChannel();
        }

        // store the workflow
        Gateway.getStorage().put(item, wf, transactionKey);
    }
}
