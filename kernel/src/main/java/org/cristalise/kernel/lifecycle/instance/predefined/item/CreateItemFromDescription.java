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
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.CorbaServer;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.AgentPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.SimpleTransactionManager;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
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
    protected String runActivityLogic(AgentPath agent, ItemPath descItemPath, int transitionID, String requestData, Object transactionKey)
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
        CorbaServer corbaFactory = Gateway.getCorbaServer();

        if (corbaFactory == null) throw new CannotManageException("This process cannot create new Items");

        corbaFactory.createItem(newItemPath);
        Gateway.getLookupManager().add(newItemPath);

        initialiseItem(agent, descItemPath, initProps, outcome, newName, descVer, context, newItemPath, transactionKey);

        return requestData;
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
     * @param servant
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     * @throws InvalidDataException
     * @throws ObjectAlreadyExistsException
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    protected void initialiseItem(AgentPath         agent, 
                                  ItemPath          descItemPath, 
                                  PropertyArrayList initProps,
                                  String            outcome,
                                  String            newName, 
                                  String            descVer,
                                  DomainPath        context, 
                                  ItemPath          newItemPath, 
                                  Object            transactionKey
                                  )
            throws ObjectCannotBeUpdated, 
                   CannotManageException,
                   InvalidDataException, 
                   ObjectAlreadyExistsException, 
                   PersistencyException, 
                   ObjectNotFoundException
    {
        log.info("initialiseItem() - Initializing Item:{}({})", newName, newItemPath);

        try {
            PropertyArrayList   newProps     = instantiateProperties (descItemPath, descVer, initProps, newName, agent, transactionKey);
            CollectionArrayList newColls     = instantiateCollections(descItemPath, descVer, newProps, transactionKey);
            CompositeActivity   newWorkflow  = instantiateWorkflow   (descItemPath, descVer, transactionKey);
            Viewpoint           newViewpoint = instantiateViewpoint  (descItemPath, descVer, transactionKey);

            if (Gateway.getStorage() instanceof SimpleTransactionManager) {
                // SimpleTransactionManager cannot update more then one Item in one transaction => start new transaction
                Object newTranasactionKey = new Object();
                try {
                    storeNewItem(agent, newItemPath, newProps, newWorkflow, newColls, newViewpoint, outcome, newTranasactionKey);
                    Gateway.getStorage().commit(newTranasactionKey);
                }
                catch (InvalidDataException | ObjectNotFoundException | PersistencyException e) {
                    log.debug("", e);
                    Gateway.getStorage().abort(newTranasactionKey);
                    throw e;
                }
            }
            else {
                // DelegatingTransactionManager can update more than one Item in one transaction
                storeNewItem(agent, newItemPath, newProps, newWorkflow, newColls, newViewpoint, outcome, transactionKey);
            }
        }
        catch (Exception e) {
            log.debug("", e);
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
     * @param transactionKey
     * @return props
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    protected PropertyArrayList instantiateProperties(ItemPath descItemPath, String descVer, PropertyArrayList initProps, String newName, AgentPath agent, Object transactionKey)
            throws ObjectNotFoundException, InvalidDataException
    {
        // copy properties -- intend to create from propdesc
        PropertyDescriptionList pdList = PropertyUtility.getPropertyDescriptionOutcome(descItemPath, descVer, transactionKey);
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
     * @param transactionKey
     * @return the Workflow instance
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected CompositeActivity instantiateWorkflow(ItemPath descItemPath, String descVer, Object transactionKey)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>)
                Gateway.getStorage().get(descItemPath, COLLECTION + "/" + WORKFLOW + "/" + descVer, transactionKey);

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
     * @param transactionKey
     * @return the new collection
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws InvalidDataException
     */
    protected CollectionArrayList instantiateCollections(ItemPath descItemPath, String descVer, PropertyArrayList newProps , Object transactionKey)
            throws ObjectNotFoundException, PersistencyException, InvalidDataException
    {
        // loop through collections, collecting instantiated descriptions and finding the default workflow def
        CollectionArrayList colls = new CollectionArrayList();
        String[] collNames = Gateway.getStorage().getClusterContents(descItemPath, COLLECTION);

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
    public static Collection<?> instantiateCollection(String collName, ItemPath descItemPath, String descVer, PropertyArrayList newProps, Object transactionKey) 
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> collOfDesc = (Collection<? extends CollectionMember>)
                Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, transactionKey);

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
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws PersistencyException
     */
    protected Viewpoint instantiateViewpoint(ItemPath descItemPath, String descVer, Object transactionKey) 
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        String collPath = COLLECTION + "/" + SCHEMA_INITIALISE;

        if (Gateway.getStorage().getClusterContents(descItemPath, collPath).length == 0) return null;

        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>)
                    Gateway.getStorage().get(descItemPath, collPath + "/" + descVer, transactionKey);

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

    /**
     * 
     * @param agentPath
     * @param itemPath
     * @param props
     * @param wf
     * @param colls
     * @param constructorViewpoint
     * @param constructorOutcome
     * @param transactionKey
     * @throws AccessRightsException
     * @throws InvalidDataException
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     * @throws MappingException
     */
    public static void storeNewItem(AgentPath agentPath,
                                    ItemPath itemPath,
                                    PropertyArrayList props,
                                    CompositeActivity domainWf,
                                    CollectionArrayList colls,
                                    Viewpoint constructorViewpoint, 
                                    String constructorOutcomeString,
                                    Object transactionKey
        )
        throws InvalidDataException,
               PersistencyException,
               ObjectNotFoundException
    {
        // store ItemProperties
        for (Property thisProp : props.list) Gateway.getStorage().put(itemPath, thisProp, transactionKey);

        History hist = new History(itemPath, transactionKey);

        // store initialise event with 'ItemInitialization' Outcome of ItemProperties
        Schema initSchema = LocalObjectLoader.getSchema("ItemInitialization", 0);
        Event initEvent = hist.addEvent(
                agentPath, null, "", "Initialize", "", "", initSchema,
                LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, "last");

        Outcome initOutcome = null;
        try {
            String propsString = Gateway.getMarshaller().marshall(props);
            initOutcome = new Outcome(initEvent.getID(), propsString, initSchema);
        }
        catch (MarshalException | ValidationException | IOException | MappingException e) {
            throw new InvalidDataException(e.getMessage());
        }

        Viewpoint newLastView = new Viewpoint(itemPath, initSchema, "last", initEvent.getID());
        Gateway.getStorage().put(itemPath, initOutcome, transactionKey);
        Gateway.getStorage().put(itemPath, newLastView, transactionKey);

        // store constructor
        if (constructorViewpoint != null) {
            Schema constructorSchema = LocalObjectLoader.getSchema(constructorViewpoint);
            Outcome constructorOutcome = new Outcome(-1, constructorOutcomeString, constructorSchema);

            constructorOutcome.validateAndCheck();

            constructorViewpoint.setItemPath(itemPath);

            Event constructorEvent = hist.addEvent(
                    agentPath, null, "", "Constructor", "", "", constructorOutcome.getSchema(),
                    LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, constructorViewpoint.getName());

            constructorViewpoint.setEventId(constructorEvent.getID());
            constructorOutcome.setID(constructorEvent.getID());

            Gateway.getStorage().put(itemPath, constructorOutcome, transactionKey);
            Gateway.getStorage().put(itemPath, constructorViewpoint, transactionKey);
        }

        // store collection
        if (colls != null) {
            for (Collection<?> thisColl : colls.list) {
                Gateway.getStorage().put(itemPath, thisColl, transactionKey);
            }
        }
        
        PredefinedStepContainer predefCont = new ItemPredefinedStepContainer();
        
        if (itemPath instanceof AgentPath) predefCont = new AgentPredefinedStepContainer();

        // initialise and store workflow
        Workflow wf = new Workflow(domainWf, predefCont);
        wf.initialise(itemPath, agentPath, transactionKey);
        Gateway.getStorage().put(itemPath, wf, transactionKey);
    }
}
