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

import static org.cristalise.kernel.collection.Collection.Type.Bidirectional;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DEPENDENCY_TO;

import java.io.IOException;

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
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.Activity;
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

/**
 */
@Slf4j
public abstract class ManageMembersOfCollectionBase extends PredefinedStep {

    public ManageMembersOfCollectionBase() {
        super();
    }

    /**
     * 
     * @param item
     * @param propertyValue
     * @param scriptProps
     * @param transactionKey
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws InvalidCollectionModification
     */
    protected void evaluateScript(ItemPath item, String propertyValue, CastorHashMap scriptProps, TransactionKey transactionKey)
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
            throw new InvalidCollectionModification(e);
        }
    }

    protected void checkCardinatilyConstraint(Dependency currentDependency, Dependency inputDependency, ItemPath itemPath, TransactionKey transactionKey) 
            throws InvalidDataException, InvalidCollectionModification
    {
        Cardinality currentDepCardinality = currentDependency.getCardinality();

        if (currentDepCardinality != null) {
            switch (currentDepCardinality) {
                case OneToOne:
                case ManyToOne:
                case OneToMany:
                case ManyToMany:
                    log.trace("checkCardinatilyConstraint() - NO check was implemented");
                    break;

                default:
                    String msg = "Unknown cardinality - "+ currentDependency + " of item "+itemPath;
                    log.error("checkCardinatilyConstraint() - {}", msg);
                    throw new InvalidDataException(msg);
            }
        }
    }

    /**
     * Adds an update instruction to call the predefined step on the current Item based on  serialized Dependency.
     * 
     * @param currentItem the Item currently processing an Activity.requets()
     * @param inputOutcome contains the marshaled input Dependency
     * @return the input Dependency object unmarshaled from the Outcome
     * @throws InvalidDataException processing outcome had an error
     */
    protected Dependency addCurrentDependencyUpdate(ItemPath currentItem, Node dependencyNode) throws InvalidDataException {
        String dependencyString = Outcome.serialize(dependencyNode, false);

        try {
            getAutoUpdates().put(currentItem, dependencyString);

            if (log.isTraceEnabled()) {
                log.trace("addCurrentDependencyUpdate() - currentItem:{} outcome:{}", currentItem.getItemName(), dependencyString);
            }

            return (Dependency) Gateway.getMarshaller().unmarshall(dependencyString);
        }
        catch (MarshalException | ValidationException | IOException | MappingException e) {
            log.error("The outcome must contain the serialized Dependency - outcome:{}", dependencyString, e);
            throw new InvalidDataException("The outcome must contain the serialized Dependency", e);
        }
    }

    /**
     * Loops all members in the inputDependency and adds an update instruction for each of them. This will result 
     * in a call of AddMembersToCollection/RemoveMembersFromCollection on the Items referenced by the member.
     * 
     * @param currentItem the Item currently processing an Activity.request()
     * @param toDependencyName the name of the Dependency in the other Item, i.e. referenced by the member.
     * @param inputDependency the Dependency object extracted from the Outcome of the Activity.requets()
     * @param transactionKey
     * @throws InvalidDataException 
     */
    private void addUpdates_DependencyTo(ItemPath currentItem, Dependency currentDependency, Dependency inputDependency, TransactionKey transactionKey)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException
    {
        String toDependencyName = currentDependency.getToDependencyName(currentItem, transactionKey);

        for (DependencyMember inputMember : inputDependency.getMembers().list) {
            Dependency toDep = new Dependency(toDependencyName);

            try {
                CastorHashMap inputMemberProps = inputMember.getProperties();
                inputMemberProps.setBuiltInProperty(DEPENDENCY_TO, currentDependency.getName());

                toDep.addMember(currentItem, inputMemberProps, inputMember.getClassProps(), null);

                String dependencyString = Gateway.getMarshaller().marshall(toDep);

                if (log.isTraceEnabled()) {
                    log.trace("addUpdates_DependencyTo() - toItem:{} outcome:{}", inputMember.getItemPath().getItemName(), dependencyString);
                }

                getAutoUpdates().put(inputMember.getItemPath(), dependencyString);
            }
            catch (MarshalException | ValidationException | IOException | MappingException e) {
                log.error("addUpdates_DependencyTo()", e);
                throw new InvalidDataException(e.getMessage());
            }
        }
    }

    @Override
    public void computeUpdates(ItemPath currentItemPath, Activity currentActivity, Node inputNode, TransactionKey transactionKey)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException, ObjectAlreadyExistsException, InvalidCollectionModification 
    {
        ItemProxy item = Gateway.getProxy(currentItemPath, transactionKey);
        Dependency inputDependency = addCurrentDependencyUpdate(currentItemPath, inputNode);

        Dependency currentDependency = (Dependency) item.getCollection(inputDependency.getName(), null, transactionKey);
        Type currentDepType = currentDependency.getType();

        if (currentDepType != null && currentDepType == Bidirectional) {
            addUpdates_DependencyTo(currentItemPath, currentDependency, inputDependency, transactionKey);
        }
    }
}
