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

import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;

/**
 * {@value #description} - Params: 0 - collection name, 1 - slot id
 * @deprecated use {@link RemoveMembersToCollection}
 */
public class RemoveSlotFromCollection extends PredefinedStepCollectionBase {

    public static final String description = "Removes the given slot from the collection";

    public RemoveSlotFromCollection() {
        super(description);
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException, InvalidCollectionModification
    {
        unpackParamsAndGetCollection(item, requestData, transactionKey);

        if (slotID == -1) throw new InvalidDataException(item + " must give slot id to remove member");

        if (collection instanceof Dependency && ((Dependency)collection).containsBuiltInProperty(MEMBER_REMOVE_SCRIPT)) {
            Dependency dep = (Dependency) collection;
            DependencyMember member = dep.getMember(slotID);

            CastorHashMap scriptProps = new CastorHashMap();
            scriptProps.put("collection", collection);
            scriptProps.put("slotID", slotID);
            scriptProps.put("member", member);

            evaluateScript(item, (String) dep.getBuiltInProperty(MEMBER_REMOVE_SCRIPT), scriptProps, transactionKey);
        }

        // Remove the slot
        collection.removeMember(slotID);

        Gateway.getStorage().put(item, collection, transactionKey);

        return requestData;
    }
}
