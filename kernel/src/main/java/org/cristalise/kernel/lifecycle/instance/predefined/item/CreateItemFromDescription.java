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
import org.cristalise.kernel.lookup.ItemPath;
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
     * Params encoded in requestData:
     * <ol>
     * <li>Item name</li>
     * <li>Domain context</li>
     * <li>Description version to use(optional)</li>
     * <li>Initial properties to set in the new Agent (optional)</li>
     * <li>Outcome for constructor (optional)</li>
     * </ol>
     * 
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     *             The input parameters were incorrect
     * @throws ObjectAlreadyExistsException
     *             The Agent already exists
     * @throws CannotManageException
     *             The Agent could not be created
     * @throws ObjectCannotBeUpdated
     *             The addition of the new entries into the LookupManager failed
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
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(), descItemPath, (Object) params);

        String newName = params[0];
        String domPath = params[1];
        String descVer = params.length > 2 && StringUtils.isNotBlank(params[2]) ? params[2] : "last";
        PropertyArrayList initProps = params.length > 3 && StringUtils.isNotBlank(params[3]) ? unmarshallInitProperties(params[3]) : new PropertyArrayList();
        String outcome = params.length > 4 && StringUtils.isNotBlank(params[4]) ? params[4] : "";

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
    protected void initialiseItem(ItemOperations newItem,
            AgentPath agent,
            ItemPath descItemPath,
            PropertyArrayList initProps,
            String outcome,
            String newName,
            String descVer,
            DomainPath context,
            ItemPath newItemPath,
            Object locker)
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
            PropertyArrayList newProps = instantiateProperties(descItemPath, descVer, initProps, newName, newItemPath, agent, locker);
            createInitialiseEvent(newItemPath, agent, newProps, locker);
            if (StringUtils.isNotBlank(outcome)) {
                instantiateConstructor(descItemPath, descVer, newItemPath, outcome, agent, locker);
            }
            instantiateCollections(descItemPath, descVer, newProps, newItemPath, locker);
            instantiateWorkflow(descItemPath, descVer, newItemPath, agent, locker);
        }
        catch (MarshalException | ValidationException | AccessRightsException | IOException | MappingException e) {
            log.error("", e);
            Gateway.getLookupManager().delete(newItemPath);
            throw new InvalidDataException("CreateItemFromDescription: Problem initializing new Item. See log: " + e.getMessage());
        }
        catch (InvalidDataException | ObjectNotFoundException | PersistencyException e) {
            log.error("Problem initializing new Item", e);
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
            log.error("Initial property parameter was not a marshalled PropertyArrayList", e);
            throw new InvalidDataException("Initial property parameter was not a marshalled PropertyArrayList: " + initPropString);
        }
    }

    /**
     * Instantiates and stores the properties
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
     * @throws PersistencyException
     */
    protected PropertyArrayList instantiateProperties(ItemPath descItemPath, String descVer, PropertyArrayList initProps,
            String newName, ItemPath newItemPath, AgentPath agent, Object locker)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException {
        // copy properties -- intend to create from propdesc
        PropertyDescriptionList pdList = PropertyUtility.getPropertyDescriptionOutcome(descItemPath, descVer, locker);
        PropertyArrayList props = pdList.instantiate(initProps);

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

        // store them
        for (Property thisProp : props.list) Gateway.getStorage().put(newItemPath, thisProp, locker);

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
    protected void instantiateWorkflow(ItemPath descItemPath, String descVer, ItemPath newItemPath, AgentPath agentPath, Object locker)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>) Gateway.getStorage().get(descItemPath,
                COLLECTION + "/" + WORKFLOW + "/" + descVer, locker);

        CollectionMember wfMember = thisCol.getMembers().list.get(0);
        String wfDefName = wfMember.resolveItem().getName();
        Object wfVerObj = wfMember.getProperties().getBuiltInProperty(VERSION);

        if (wfVerObj == null || String.valueOf(wfVerObj).length() == 0) {
            throw new InvalidDataException("Workflow version number not set");
        }

        try {
            Integer wfDefVer = Integer.parseInt(wfVerObj.toString());

            if (wfDefName == null) throw new InvalidDataException("No workflow given or defined");

            // load workflow def, instantiate, initalise and store
            CompositeActivityDef caDef = (CompositeActivityDef) LocalObjectLoader.getActDef(wfDefName, wfDefVer);
            CompositeActivity ca =  (CompositeActivity) caDef.instantiate();
            Workflow wf = new Workflow(ca, new ItemPredefinedStepContainer());
            wf.initialise(newItemPath, agentPath, locker);
            Gateway.getStorage().put(newItemPath, wf, locker);
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
    protected void instantiateCollections(ItemPath descItemPath, String descVer, PropertyArrayList newProps,
            ItemPath newItemPath, Object locker)
            throws ObjectNotFoundException, PersistencyException, InvalidDataException
    {
        String[] collNames = Gateway.getStorage().getClusterContents(descItemPath, COLLECTION);

        // loop through collections, collecting instantiated descriptions
        for (String collName : collNames) {
            instantiateCollection(collName, descItemPath, descVer, newProps, newItemPath, locker);
        }
    }

    /**
     * 
     * @param collName
     * @param descItemPath
     * @param descVer
     * @param newProps
     * @param locker
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    public static void instantiateCollection(String collName, ItemPath descItemPath, String descVer, PropertyArrayList newProps,
            ItemPath newItemPath, Object locker)
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> collOfDesc = (Collection<? extends CollectionMember>) 
            Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, locker);

        if (collOfDesc instanceof CollectionDescription) {
            log.info("Instantiating CollectionDescription:" + collName);
            CollectionDescription<?> collDesc = (CollectionDescription<?>) collOfDesc;
            Collection<?>  newColl = collDesc.newInstance();

            // store collection
            Gateway.getStorage().put(newItemPath, newColl, locker);
        }
        else if (collOfDesc instanceof Dependency) {
            log.info("Instantiating Dependency:" + collName);
            ((Dependency) collOfDesc).addToItemProperties(newProps);

            // store properties
            for (Property thisProp : newProps.list) Gateway.getStorage().put(newItemPath, thisProp, locker);
        }
        else {
            throw new InvalidDataException("CANNOT instantiate collection:" + collName + " class:" + collOfDesc.getClass().getName());
        }
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
    protected void instantiateConstructor(ItemPath descItemPath, String descVer, ItemPath newItemPath, String initOutcomeString,
            AgentPath agentPath, Object locker)
            throws ObjectNotFoundException, InvalidDataException, PersistencyException
    {
        String collPath = COLLECTION + "/" + SCHEMA_INITIALISE;

        if (Gateway.getStorage().getClusterContents(descItemPath, collPath).length == 0) {
            throw new InvalidDataException("Dependency was not defined:" + SCHEMA_INITIALISE);
        }

        @SuppressWarnings("unchecked")
        Collection<? extends CollectionMember> thisCol = (Collection<? extends CollectionMember>) Gateway.getStorage().get(descItemPath,
                collPath + "/" + descVer, locker);

        CollectionMember schemaMember = thisCol.getMembers().list.get(0);

        String schemaName   = schemaMember.resolveItem().getName();
        Object schemaVerObj = schemaMember.getProperties().getBuiltInProperty(VERSION);
        Object viewNameObj  = schemaMember.getProperties().get("View");

        if (schemaName == null) throw new InvalidDataException("No schema given or defined");

        if (schemaVerObj == null || String.valueOf(schemaVerObj).length() == 0) {
            throw new InvalidDataException("schema version number not set");
        }

        try {
            History hist = new History(newItemPath, locker);

            Integer schemaVer = Integer.parseInt(schemaVerObj.toString());
            String viewName = (viewNameObj == null) ? "last" : viewNameObj.toString();
            Viewpoint vp = new Viewpoint(newItemPath, schemaName, viewName, schemaVer, -1);

            Schema schema = LocalObjectLoader.getSchema(vp.getSchemaName(), vp.getSchemaVersion());
            Outcome outcome = new Outcome(-1, initOutcomeString, schema);
            outcome.validateAndCheck();

            Event newEvent = hist.addEvent(
                    agentPath, null, "", "Constructor", "", "", schema,
                    LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, vp.getName());

            vp.setEventId(newEvent.getID());
            outcome.setID(newEvent.getID());

            Gateway.getStorage().put(newItemPath, outcome, locker);
            Gateway.getStorage().put(newItemPath, vp, locker);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("Invalid schema version number: " + schemaVerObj.toString());
        }
    }

    /**
     * 
     * @param newItemPath
     * @param agentPath
     * @param newProps
     * @param locker
     * @throws AccessRightsException
     * @throws InvalidDataException
     * @throws PersistencyException
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     * @throws MappingException
     * @throws ObjectNotFoundException
     */
    private void createInitialiseEvent(ItemPath newItemPath, AgentPath agentPath, PropertyArrayList newProps, Object locker)
            throws AccessRightsException, InvalidDataException, PersistencyException, MarshalException, ValidationException, IOException,
                   MappingException, ObjectNotFoundException 
    {
        History hist = new History(newItemPath, locker);

        // Store an "Initialize" event and the outcome containing the initial values for properties
        Schema initSchema = LocalObjectLoader.getSchema("ItemInitialization", 0);
        Outcome initOutcome = new Outcome(0, Gateway.getMarshaller().marshall(newProps), initSchema);

        Event newEvent = hist.addEvent(
                agentPath, null, "", "Initialize", "", "", initSchema,
                LocalObjectLoader.getStateMachine("PredefinedStep", 0), PredefinedStep.DONE, "last");

        initOutcome.setID(newEvent.getID());
        Viewpoint newLastView = new Viewpoint(newItemPath, initSchema, "last", newEvent.getID());
        Gateway.getStorage().put(newItemPath, initOutcome, locker);
        Gateway.getStorage().put(newItemPath, newLastView, locker);
    }
}
