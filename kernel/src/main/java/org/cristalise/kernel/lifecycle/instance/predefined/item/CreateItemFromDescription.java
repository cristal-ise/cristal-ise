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
package org.cristalise.kernel.lifecycle.instance.predefined.item;

import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE;
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.VERSION;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;
import static org.cristalise.kernel.property.BuiltInItemProperties.CREATOR;
import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionArrayList;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.CorbaServer;
import org.cristalise.kernel.entity.ItemImplementation;
import org.cristalise.kernel.entity.ItemOperations;
import org.cristalise.kernel.entity.TraceableEntity;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateItemFromDescription extends PredefinedStep {

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
    protected String runActivityLogic(AgentPath agent, ItemPath descItemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException,
                   ObjectNotFoundException,
                   ObjectAlreadyExistsException,
                   CannotManageException,
                   ObjectCannotBeUpdated,
                   PersistencyException
    {
        String[] input = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(), descItemPath, (Object)input);

        String            newName   = input[0];
        String            domPath   = input[1];
        String            descVer   = input.length > 2 && StringUtils.isNotBlank(input[2]) ? input[2] : "last";
        PropertyArrayList initProps = input.length > 3 && StringUtils.isNotBlank(input[3]) ? unmarshallInitProperties(input[3]) : new PropertyArrayList();
        String            outcome   = input.length > 4 && StringUtils.isNotBlank(input[4]) ? input[4] : "";

        // check if the path is already taken
        DomainPath context = new DomainPath(new DomainPath(domPath), newName);

        if (context.exists()) throw new ObjectAlreadyExistsException("The path " + context + " exists already.");

        // generate new item path with random uuid
        ItemPath newItemPath = new ItemPath();

        // create the Item object
        log.info("Creating Item name:{} uuid:{}", newName, newItemPath);
        CorbaServer factory = Gateway.getCorbaServer();

        if (factory == null) throw new CannotManageException("This process cannot create new Items");

        TraceableEntity newItem = factory.createItem(newItemPath);
        Gateway.getLookupManager().add(newItemPath);

        initialiseItem(newItem, agent, descItemPath, initProps, outcome, newName, descVer, context, newItemPath, locker);

        return requestData;
    }

    /**
     * 
     * @param agent
     * @param descItemPath
     * @param locker
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
    protected void initialiseItem(ItemOperations    newItem, 
                                  AgentPath         agent, 
                                  ItemPath          descItemPath, 
                                  PropertyArrayList initProps,
                                  String            outcome,
                                  String            newName, 
                                  String            descVer,
                                  DomainPath        context, 
                                  ItemPath          newItemPath, 
                                  Object            locker
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
            PropertyArrayList   newProps     = instantiateProperties (descItemPath, descVer, initProps, newName, agent, locker);
            CollectionArrayList newColls     = instantiateCollections(descItemPath, descVer, newProps, locker);
            CompositeActivity   newWorkflow  = instantiateWorkflow   (descItemPath, descVer, locker);
            Viewpoint           newViewpoint = instantiateViewpoint  (descItemPath, descVer, locker);

            CastorXMLUtility xml = Gateway.getMarshaller();
           
            
            if ( ! Gateway.getProperties().getBoolean("ServerSideScripting", false)
                    || "Client" .equals( Gateway.getProperties().getString("ProcessType", "Client")) ) {
            	
            	newItem.initialise( agent.getSystemKey(),
                        xml.marshall(newProps),
                        xml.marshall(newWorkflow),
                        xml.marshall(newColls),
                        (newViewpoint != null) ? xml.marshall(newViewpoint) : "",
                        (outcome != null) ? outcome: "");
             } else {
            	 
            	 AgentPath agentPath;
                 try {
                     agentPath = new AgentPath(agent.getSystemKey());
                 }
                 catch (InvalidItemPathException e) {
                     throw new AccessRightsException("Invalid Agent Id:" + agent.getSystemKey());
                 }
                 
                 initialiseItemProperties(newItemPath,agent.getSystemKey(), newProps, locker);
                 initialiseItemViewpointAndOutcome(newItemPath, agent.getSystemKey(), newViewpoint, outcome != null ? outcome : "", locker);
                 initialiseItemCollection(newItemPath, newColls, locker);
                 initialiseItemWorkflow(newItemPath, agentPath, newWorkflow, locker);
             }
         
        }
        catch (MarshalException | ValidationException | AccessRightsException | IOException | MappingException | InvalidCollectionModification e) {
            log.error("", e);
            Gateway.getLookupManager().delete(newItemPath);
            throw new InvalidDataException("CreateItemFromDescription: Problem initializing new Item. See log: " + e.getMessage());
        }
        catch (InvalidDataException | ObjectNotFoundException | PersistencyException e) {
            log.error("", e);
            Gateway.getLookupManager().delete(newItemPath);
            throw e;
        }

        // add its domain path
        log.info("Creating " + context);
        context.setItemPath(newItemPath);
        Gateway.getLookupManager().add(context);
    }

    /**
     * Unmarshalls initial Properties
     *
     * @param initPropString
     * @return unmarshalled initial PropertyArrayList
     * @throws InvalidDataException
     */
    protected PropertyArrayList unmarshallInitProperties(String initPropString) throws InvalidDataException {
        try {
            return (PropertyArrayList) Gateway.getMarshaller().unmarshall(initPropString);
        }
        catch (Exception e) {
            log.error("", e);
            throw new InvalidDataException("Initial property parameter was not a marshalled PropertyArrayList: " + initPropString);
        }
    }

    /**
     *
     * @param descItemPath
     * @param descVer
     * @param initProps
     * @param newName
     * @param agent
     * @param locker
     * @return props
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected PropertyArrayList instantiateProperties(ItemPath descItemPath, String descVer, PropertyArrayList initProps, String newName, AgentPath agent, Object locker)
            throws ObjectNotFoundException, InvalidDataException
    {
        // copy properties -- intend to create from propdesc
        PropertyDescriptionList pdList = PropertyUtility.getPropertyDescriptionOutcome(descItemPath, descVer, locker);
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
        props.list.add(new Property(CREATOR, agent.getAgentName(), false));

        return props;
    }

    /**
     * Retrieve the Workflow dependency for the given description version, instantiate the loaded CompositeActivityDef
     *
     * @param descItemPath
     * @param descVer
     * @param locker
     * @return the Workflow instance
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected CompositeActivity instantiateWorkflow(ItemPath descItemPath, String descVer, Object locker)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>)
                    Gateway.getStorage().get(descItemPath, COLLECTION + "/" + WORKFLOW + "/" + descVer, locker);

        CollectionMember wfMember  = thisCol.getMembers().list.get(0);
        String           wfDefName = wfMember.resolveItem().getName();
        Object           wfVerObj  = wfMember.getProperties().getBuiltInProperty(VERSION);

        if (wfVerObj == null || String.valueOf(wfVerObj).length() == 0) {
            throw new InvalidDataException("Workflow version number not set");
        }

        try {
            Integer wfDefVer = Integer.parseInt(wfVerObj.toString());

            if (wfDefName == null) throw new InvalidDataException("No workflow given or defined");

            // load workflow def
            CompositeActivityDef wfDef = (CompositeActivityDef) LocalObjectLoader.getActDef(wfDefName, wfDefVer);
            return (CompositeActivity) wfDef.instantiate();
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
     * @param locker
     * @return the new collection
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws InvalidDataException
     */
    protected CollectionArrayList instantiateCollections(ItemPath descItemPath, String descVer, PropertyArrayList newProps , Object locker)
            throws ObjectNotFoundException, PersistencyException, InvalidDataException
    {
        // loop through collections, collecting instantiated descriptions and finding the default workflow def
        CollectionArrayList colls = new CollectionArrayList();
        String[] collNames = Gateway.getStorage().getClusterContents(descItemPath, COLLECTION);

        for (String collName : collNames) {
            Collection<?> newColl = instantiateCollection(collName, descItemPath, descVer, newProps, locker);
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
     * @param locker
     * @return
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public static Collection<?> instantiateCollection(String collName, ItemPath descItemPath, String descVer, PropertyArrayList newProps, Object locker) 
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> collOfDesc = (Collection<? extends CollectionMember>)
                Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, locker);

        Collection<?> newColl = null;

        if (collOfDesc instanceof CollectionDescription) {
            log.info("Instantiating CollectionDescription:"+ collName);
            CollectionDescription<?> collDesc = (CollectionDescription<?>) collOfDesc;
            newColl = collDesc.newInstance();
        }
        else if(collOfDesc instanceof Dependency) {
            log.info("Instantiating Dependency:"+ collName);
            ((Dependency) collOfDesc).addToItemProperties(newProps);
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
     * @param locker
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected Viewpoint instantiateViewpoint(ItemPath descItemPath, String descVer, Object locker) 
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        String collPath = COLLECTION + "/" + SCHEMA_INITIALISE;

        if (Gateway.getStorage().getClusterContents(descItemPath, collPath).length == 0) return null;

        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>)
                    Gateway.getStorage().get(descItemPath, collPath + "/" + descVer, locker);

        CollectionMember schemaMember = thisCol.getMembers().list.get(0);
        String           schemaName   = schemaMember.resolveItem().getName();
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
    
    protected void initialiseItemProperties(ItemPath itemPath, SystemKey agentId, PropertyArrayList newProps, Object transactionKey) 
    		throws MarshalException, ValidationException, IOException, MappingException, InvalidDataException, PersistencyException {
    	
    	String propString = Gateway.getMarshaller().marshall(newProps);
    	
    	
    	if(transactionKey == null) {
    		 throw new InvalidDataException("initialiseItemProperties () - TransactionKey should be provided on server side scripting");
    	}
    	
        if (StringUtils.isBlank(propString) || propString.equals("<NULL/>")) {
            throw new InvalidDataException("No properties supplied");
        }
        
        try {
            PropertyArrayList props = (PropertyArrayList) Gateway.getMarshaller().unmarshall(propString);
            for (Property thisProp : props.list) {
                Gateway.getStorage().put(itemPath, thisProp, transactionKey);
            }
        }
        catch (Exception ex) {
            log.error("initialiseItemProperties() - Properties were invalid: {}", itemPath, propString, ex);
            Gateway.getStorage().abort(transactionKey);
            throw new InvalidDataException("Properties were invalid");
        }
        initialiseItemEvents(itemPath, agentId, propString, transactionKey);
    }
    
    protected void initialiseItemEvents(ItemPath itemPath, SystemKey agentId, String propString, Object transactionKey) 
    		throws MarshalException, ValidationException, IOException, MappingException, InvalidDataException, PersistencyException {
    	
    	History hist = new History(itemPath, transactionKey);

         // Store an "Initialize" event and the outcome containing the initial values for properties
         try {
             Schema initSchema = LocalObjectLoader.getSchema("ItemInitialization", 0);
             Outcome initOutcome = new Outcome(0, propString, initSchema);

             Event newEvent = hist.addEvent(
                     new AgentPath(agentId), null, "", "Initialize", "", "", initSchema,
                     LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, "last");

             initOutcome.setID(newEvent.getID());
             Viewpoint newLastView = new Viewpoint(itemPath, initSchema, "last", newEvent.getID());
             Gateway.getStorage().put(itemPath, initOutcome, transactionKey);
             Gateway.getStorage().put(itemPath, newLastView, transactionKey);
         }
         catch (Exception ex) {
             log.error("initialiseItemEvents() - Could not store event and outcome.", itemPath, ex);
             Gateway.getStorage().abort(transactionKey);
             throw new PersistencyException("Error storing 'Initialize' event and outcome:" + ex.getMessage());
         }
         
    }
    
    protected void initialiseItemViewpointAndOutcome(ItemPath itemPath, SystemKey agentId, Viewpoint newViewpoint, String initOutcomeString, Object transactionKey)
    		throws PersistencyException, InvalidDataException, MarshalException, ValidationException, IOException, MappingException {
    
    	String initViewpointString = Gateway.getMarshaller().marshall(newViewpoint);
    	if(initViewpointString.equals("<NULL/>")) {
    		initViewpointString = "";
    	}

    	
    	
    	if(transactionKey == null) {
    		throw new InvalidDataException("initialiseItemViewpointAndOutcome() : TransactionKey should be provided on server side scripting");
    	}
    	
    	History hist = new History(itemPath, transactionKey);
    
        if (StringUtils.isNotBlank(initViewpointString)) {
            try {
                Viewpoint vp = (Viewpoint)Gateway.getMarshaller().unmarshall(initViewpointString);
                Schema schema = LocalObjectLoader.getSchema(vp.getSchemaName(), vp.getSchemaVersion());
                Outcome outcome = new Outcome(-1, initOutcomeString, schema);
                outcome.validateAndCheck();

                vp.setItemPath(itemPath);

                Event newEvent = hist.addEvent(
                        new AgentPath(agentId), null, "", "Constructor", "", "", schema,
                        LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, vp.getName());
                vp.setEventId(newEvent.getID());
                outcome.setID(newEvent.getID());

                Gateway.getStorage().put(itemPath, outcome, transactionKey);
                Gateway.getStorage().put(itemPath, vp, transactionKey);
            }
            catch (Exception ex) {
                log.error("initialiseItemViewpointAndOutcome() - Could not store event and outcome.", itemPath, ex);
                Gateway.getStorage().abort(transactionKey);
                throw new PersistencyException("Error storing 'Constructor event and outcome:" + ex.getMessage());
            }
        }

    }
    
    protected void initialiseItemCollection(ItemPath itemPath, CollectionArrayList newColls, Object transactionKey) 
    		throws MarshalException, ValidationException, IOException, MappingException, InvalidDataException {
    	
       String initCollsString = Gateway.getMarshaller().marshall(newColls);
       
    	
    	if(transactionKey == null) {
    		throw new InvalidDataException("initialiseItemCollection() : TransactionKey should be provided on server side scripting");
    	}
    	// init collections
        if (StringUtils.isNotBlank(initCollsString) && !initCollsString.equals("<NULL/>")) {
            try {
                CollectionArrayList colls = (CollectionArrayList) Gateway.getMarshaller().unmarshall(initCollsString);
                for (Collection<?> thisColl : colls.list) {
                	Gateway.getStorage().put(itemPath, thisColl, transactionKey);
                }
            }
            catch (Exception ex) {
                log.error("initialiseItemCollection() - Collections were invalid: " + initCollsString, ex);
                Gateway.getStorage().abort(transactionKey);
                throw new InvalidDataException("Collections were invalid");
            }
        }
    }
    protected void initialiseItemWorkflow(ItemPath itemPath, AgentPath agentPath, CompositeActivity newWorkflow, Object transactionKey) 
    		throws MarshalException, ValidationException, IOException, MappingException, InvalidDataException {
    	
    	String initWfString = Gateway.getMarshaller().marshall(newWorkflow);
        
     	if(transactionKey == null) {
     		throw new InvalidDataException("initialiseItemCollection() : TransactionKey should be provided on server side scripting");
     	}
    	// create wf
        Workflow lc = null;
        try {
            if (StringUtils.isBlank(initWfString) || initWfString.equals("<NULL/>")) {
                lc = new Workflow(new CompositeActivity(), new ItemPredefinedStepContainer());
            }
            else{
                lc = new Workflow((CompositeActivity) Gateway.getMarshaller().unmarshall(initWfString), new ItemPredefinedStepContainer());
            }

            lc.initialise(itemPath, agentPath, transactionKey);
            Gateway.getStorage().put(itemPath, lc, transactionKey);
            // TODO : should commit be called here ??
            Gateway.getStorage().commit (transactionKey);
        }
        catch (Exception ex) {
            log.error("initialiseItemWorkflow() - Workflow was invalid: " + initWfString, ex);
            Gateway.getStorage().abort(transactionKey);
            throw new InvalidDataException("Workflow was invalid");
        }
    }
}
