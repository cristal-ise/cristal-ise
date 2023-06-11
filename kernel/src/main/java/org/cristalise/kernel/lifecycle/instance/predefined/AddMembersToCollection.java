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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.MEMBER_ADD_SCRIPT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import org.cristalise.kernel.collection.Collection.Cardinality;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddMembersToCollection extends ManageMembersOfCollectionBase {

    public static final String description = "Adds many members to the named Collection of the Item";

    public AddMembersToCollection() {
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "Dependency");
    }
    
    @Override
    protected void checkCardinatilyConstraint(Dependency currentDependency, Dependency inputDependency, ItemPath itemPath, TransactionKey transactionKey)
            throws InvalidDataException, InvalidCollectionModification
    {
        Cardinality currentDepCardinality = currentDependency.getCardinality();

        if (currentDepCardinality != null) {
            switch (currentDepCardinality) {
                case OneToOne:
                case ManyToOne:
                    String errorMsg = null;
                    if      (currentDependency.getMembers().list.size() != 0) errorMsg = "Cannot add new members";
                    else if (inputDependency.getMembers().list.size() != 1)   errorMsg = "Cannot add more than one member";

                    if (errorMsg != null) {
                        errorMsg = errorMsg + " - " + currentDependency + " of item " + itemPath.getItemName(transactionKey);
                        log.error("runActivityLogic() - {}", errorMsg);
                        throw new InvalidCollectionModification(errorMsg);
                    }
                    break;

                case OneToMany:
                case ManyToMany:
                    // nothing to do
                    break;

                default:
                    String msg = "Unknown cardinality - "+ currentDependency + " of item "+itemPath.getItemName(transactionKey);
                    log.error("runActivityLogic() - {}", msg);
                    throw new InvalidDataException(msg);
            }
        }
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification
    {
        ItemProxy item = Gateway.getProxy(itemPath, transactionKey);

        log.debug("runActivityLogic() - item:{} requestdata:{}", item, requestData);

        Dependency inputDependency = (Dependency) Gateway.getMarshaller().unmarshall(requestData);
        String collectionName = inputDependency.getName();
        Dependency currentDependency =
                (Dependency) item.getCollection(collectionName, null, transactionKey);

        log.debug("runActivityLogic() - Changing {} of item:{}", currentDependency, item);

        checkCardinatilyConstraint(currentDependency, inputDependency, itemPath, transactionKey);

        for (DependencyMember inputMember : inputDependency.getMembers().list) {
            CastorHashMap inputMemberProps = inputMember.getProperties();
            DependencyMember newMember = null;

            if (inputMemberProps.size() != 0) {
                newMember = currentDependency.createMember(inputMember.getItemPath(),
                        inputMemberProps, transactionKey);
            } else {
                newMember =
                        currentDependency.createMember(inputMember.getItemPath(), transactionKey);
            }

            evaluateScript(itemPath, currentDependency, newMember, transactionKey);

            currentDependency.addMember(newMember);
        }

        Gateway.getStorage().put(itemPath, currentDependency, transactionKey);

        return Gateway.getMarshaller().marshall(currentDependency);
    }

    /**
     * 
     * @param item
     * @param dependency
     * @param newMember
     * @param transactionKey
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     * @throws InvalidCollectionModification
     */
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
}
