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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.MEMBER_REMOVE_SCRIPT;
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import java.io.IOException;

import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoveMembersFromCollection extends ManageMembersOfCollectionBase {
    
    public static final String description = "Removes many members from the named Collection of the Item";

    public RemoveMembersFromCollection() {
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "Dependency");
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException, InvalidCollectionModification
    {
        ItemProxy item = Gateway.getProxy(itemPath);

        try {
            Dependency inputDependendy = (Dependency) Gateway.getMarshaller().unmarshall(requestData);
            String collectionName = inputDependendy.getName();

            Dependency currentDependency = (Dependency) item.getCollection(collectionName, null, transactionKey);

            log.debug("runActivityLogic() - {} of item {}", currentDependency, itemPath);

            checkCardinatilyConstraint(currentDependency, inputDependendy, itemPath,transactionKey);

            for (DependencyMember inputMember : inputDependendy.getMembers().list) {
                evaluateScript(itemPath, currentDependency, inputMember, transactionKey);

                currentDependency.removeMember(inputMember.getID());
            }

            Gateway.getStorage().put(itemPath, currentDependency, transactionKey);

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
        if (dependency.containsBuiltInProperty(MEMBER_REMOVE_SCRIPT)) {
            CastorHashMap scriptProps = new CastorHashMap();
            scriptProps.put("collection", dependency);
            scriptProps.put("member", newMember);

            evaluateScript(item, (String) dependency.getBuiltInProperty(MEMBER_REMOVE_SCRIPT), scriptProps, transactionKey);
        }
    }
}