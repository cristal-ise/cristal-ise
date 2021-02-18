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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.io.IOException;

import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddMembersToCollection extends PredefinedStepCollectionBase {

    public static final String description = "Adds many members to the Item";

    public AddMembersToCollection(){
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "Dependency");
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException, ObjectNotFoundException,
            InvalidCollectionModification
    {
        try {
            Dependency inputDependendency = (Dependency) Gateway.getMarshaller().unmarshall(requestData);
            String collectionName = inputDependendency.getName();
            Dependency dep = (Dependency) Gateway.getStorage().get(item, COLLECTION + "/" + collectionName + "/last", transactionKey);

            for (DependencyMember inputMember : inputDependendency.getMembers().list) {
                DependencyMember newMember = null;

                if (inputMember.getProperties() != null && inputMember.getProperties().size() != 0) {
                    newMember = dep.createMember(inputMember.getItemPath(), inputMember.getProperties(), transactionKey);
                }
                else {
                    newMember = dep.createMember(inputMember.getItemPath(), transactionKey);
                }

                evaluateScript(item, dep, newMember, transactionKey);

                dep.addMember(newMember);
            }

            Gateway.getStorage().put(item, dep, transactionKey);

            return Gateway.getMarshaller().marshall(dep);
        }
        catch (IOException | ValidationException | MarshalException | MappingException ex) {
            log.error("Error adding members to collection", ex);
            throw new InvalidDataException("Error adding members to collection: " + ex);
        }
    }
}
