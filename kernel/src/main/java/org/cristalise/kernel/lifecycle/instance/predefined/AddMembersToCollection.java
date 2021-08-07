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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.cristalise.kernel.collection.Collection.Type.Bidirectional;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DEPENDENCY_NAME;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DEPENDENCY_TO;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.MEMBER_ADD_SCRIPT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Collection.Cardinality;
import org.cristalise.kernel.collection.Collection.Type;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.w3c.dom.Node;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddMembersToCollection extends PredefinedStep {

    public static final String description = "Adds many members to the named Collection of the Item";

    public AddMembersToCollection() {
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "Dependency");
    }

    private Dependency getCurrentDependency(ItemPath item, String collectionName, Integer version, TransactionKey transactionKey ) 
            throws PersistencyException, ObjectNotFoundException
    {
        String versionString = version != null ? version.toString() : "last";
        return (Dependency) Gateway.getStorage().get(item, COLLECTION + "/" + collectionName + "/" + versionString, transactionKey);
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification
    {
        try {
            Dependency inputDependendy = (Dependency) Gateway.getMarshaller().unmarshall(requestData);
            String collectionName = inputDependendy.getName();

            Dependency currentDependency = getCurrentDependency(item, collectionName, null, transactionKey);
            Cardinality currentDepCardinality = currentDependency.getCardinality();

            log.debug("runActivityLogic() - {} of item {}", currentDependency, item);

            // check cardinality constraints
            if (currentDepCardinality != null) {
                switch (currentDepCardinality) {
                    case OneToOne:
                    case ManyToOne:
                        String errorMsg = null;
                        if      (currentDependency.getMembers().list.size() != 0) errorMsg = "Cannot add new members";
                        else if (inputDependendy.getMembers().list.size() != 1)   errorMsg = "Cannot add more than one member";

                        if (errorMsg != null) {
                            errorMsg = errorMsg + " - " + currentDependency + " of " + item;
                            log.error("runActivityLogic() - {}", errorMsg);
                            throw new InvalidCollectionModification(errorMsg);
                        }
                        break;

                    case OneToMany:
                    case ManyToMany:
                        // nothing to do
                        break;

                    default:
                        String msg = "Unknown cardinality - "+ currentDependency + " of item "+item;
                        log.error("runActivityLogic() - {}", msg);
                        throw new InvalidDataException(msg);
                }
            }

            for (DependencyMember inputMember : inputDependendy.getMembers().list) {
                DependencyMember newMember = null;

                if (inputMember.getProperties() != null && inputMember.getProperties().size() != 0) {
                    newMember = currentDependency.createMember(inputMember.getItemPath(), inputMember.getProperties(), transactionKey);
                }
                else {
                    newMember = currentDependency.createMember(inputMember.getItemPath(), transactionKey);
                }

                evaluateScript(item, currentDependency, newMember, transactionKey);

                currentDependency.addMember(newMember);
            }

            Gateway.getStorage().put(item, currentDependency, transactionKey);

            return Gateway.getMarshaller().marshall(currentDependency);
        }
        catch (IOException | ValidationException | MarshalException | MappingException ex) {
            log.error("Error adding members to collection", ex);
            throw new InvalidDataException("Error adding members to collection: " + ex);
        }
    }

    protected void evaluateScript(ItemPath item, Dependency dependency, DependencyMember newMember, TransactionKey transactionKey)
            throws ObjectNotFoundException, InvalidDataException, InvalidCollectionModification
    {
        if (dependency.containsBuiltInProperty(MEMBER_ADD_SCRIPT)) {
            CastorHashMap scriptProps = new CastorHashMap();
            scriptProps.put("collection", dependency);
            scriptProps.put("member", newMember);

            evaluateScript(item, (String) dependency.getBuiltInProperty(MEMBER_ADD_SCRIPT), scriptProps, transactionKey);
        }
    }

    protected void evaluateScript(ItemPath item, String propertyValue, CastorHashMap scriptProps , TransactionKey transactionKey)
            throws ObjectNotFoundException, InvalidDataException, InvalidCollectionModification
    {
        if (StringUtils.isBlank(propertyValue) || !propertyValue.contains(":")) {
            throw new InvalidDataException(this.getClass().getSimpleName() + " cannot resolve Script from '" + propertyValue + "' value");
        }

        String[] tokens = propertyValue.split(":");

        try {
            Script script = Script.getScript(tokens[0], Integer.valueOf(tokens[1]));
            script.evaluate(item, scriptProps, getActContext(), transactionKey);
        }
        catch (ScriptingEngineException e) {
            log.error("evaluateScript() - failed to execute script:{}", propertyValue, e);
            throw new InvalidCollectionModification(e.getMessage());
        }
    }

    /**
     * Updates an update instruction to call the AddMembersToCollection on the current Item.
     * 
     * @param currentItem the Item currently processing an Activity.requets()
     * @param inputOutcome contains the marshaled input Dependency
     * @return the input Dependency object unmarshaled from the Outcome
     * @throws InvalidDataException processing outcome had an error
     */
    private Dependency addCurrentDependencyUpdate(ItemPath currentItem, Outcome inputOutcome) throws InvalidDataException {
        Node dependencyNode = null;

        try {
            //The outcome must contain the serialized Dependency
            dependencyNode = inputOutcome.getNodeByXPath("//Dependency");

            if (dependencyNode != null) {
                String dependencyString = Outcome.serialize(dependencyNode, false);
                getAutoUpdates().put(currentItem, dependencyString);

                if (log.isTraceEnabled()) {
                    log.trace("addCurrentDependencyUpdate() - currentItem:{} outcom:{}", currentItem.getItemName(), dependencyString);
                }

                return (Dependency) Gateway.getMarshaller().unmarshall(dependencyString);
            }
            else {
                log.error("The outcome must contain the serialized Dependency - outcome:{}", inputOutcome.getData());
                throw new InvalidDataException("The outcome must contain the serialized Dependency");
            }
        }
        catch (XPathExpressionException | MarshalException | ValidationException | IOException | MappingException e) {
            log.error("The outcome must contain the serialized Dependency - outcome:{}", inputOutcome.getData(), e);
            throw new InvalidDataException("The outcome must contain the serialized Dependency - error:" + e.getMessage());
        }
    }

    /**
     * Loops all members in the inputDependency and adds an update instruction for each of them. This will result in a call 
     * of AddMembersToCollection on the Items referenced by the member.
     * 
     * @param currentItem the Item currently processing an Activity.requets()
     * @param toDependencyName the name of the Dependency in the other Item, i.e. referenced by the member.
     * @param inputDependency the Dependency object extracted from the Outcome of the Activity.requets()
     * @throws InvalidDataException 
     */
    private void addToDependencyUpdates(ItemPath currentItem, String toDependencyName, Dependency inputDependency)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException
    {
        for (DependencyMember inputMember : inputDependency.getMembers().list) {
            Dependency toDep = new Dependency(toDependencyName);

            try {
                toDep.addMember(currentItem, inputMember.getProperties(), inputMember.getClassProps(), null);

                String dependencyString = Gateway.getMarshaller().marshall(toDep);

                if (log.isTraceEnabled()) {
                    log.trace("addToDependencyUpdates() - toItem:{} outcome:{}", inputMember.getItemPath().getItemName(), dependencyString);
                }

                getAutoUpdates().put(inputMember.getItemPath(), dependencyString);
            }
            catch (MarshalException | ValidationException | IOException | MappingException e) {
                log.error("computeUpdates()", e);
                throw new InvalidDataException(e.getMessage());
            }
        }
    }

    @Override
    public void computeUpdates(ItemPath currentItem, Activity currentActivity, Outcome inputOutcome, TransactionKey transactionKey) 
            throws InvalidDataException, PersistencyException, ObjectNotFoundException, ObjectAlreadyExistsException, InvalidCollectionModification
    {
        String dependencyName = currentActivity.getBuiltInProperty(DEPENDENCY_NAME, "").toString();

        if (isBlank(dependencyName)) {
            throw new InvalidDataException("Missing ActivityProperty:"+DEPENDENCY_NAME+ " item:"+currentItem+" activity:"+currentActivity.getName());
        }

        Dependency currentDependency = getCurrentDependency(currentItem, dependencyName, null, transactionKey);
        Type currentDepType = currentDependency.getType();

        Dependency inputDependency = addCurrentDependencyUpdate(currentItem, inputOutcome);

        if (isBlank(inputDependency.getName()) || ! dependencyName.equals(inputDependency.getName())) {
            throw new InvalidDataException(dependencyName + " != " + inputDependency.getName());
        }

        if (currentDepType != null && currentDepType == Bidirectional) {
            addToDependencyUpdates(currentItem, (String)currentDependency.getBuiltInProperty(DEPENDENCY_TO), inputDependency);
        }
    }
}
