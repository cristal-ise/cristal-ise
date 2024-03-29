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

import java.util.Arrays;

import org.cristalise.kernel.collection.Aggregation;
import org.cristalise.kernel.collection.AggregationMember;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClearSlot extends PredefinedStep {

    public ClearSlot() {
        super("Clears an aggregation member slot, given a slot no or item uuid");
    }

    /**
     * Params: 0 - collection name 1 - slot number
     * 
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     * @throws ObjectCannotBeUpdated
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException, ObjectCannotBeUpdated
    {
        String collName;
        int slotNo;
        Aggregation agg;

        // extract parameters
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)params);

        try {
            collName = params[0];
            slotNo = Integer.parseInt(params[1]);
        }
        catch (Exception e) {
            throw new InvalidDataException("ClearSlot: Invalid parameters " + Arrays.toString(params));
        }

        // load collection
        try {
            agg = (Aggregation) Gateway.getStorage().get(item, ClusterType.COLLECTION + "/" + collName + "/last", transactionKey);
        }
        catch (PersistencyException ex) {
            log.error("ClearSlot: Error loading collection '" + collName + "'", ex);
            throw new PersistencyException("ClearSlot: Error loading collection '" + collName + "': " + ex.getMessage());
        }

        // find member and clear
        boolean stored = false;
        for (AggregationMember member : agg.getMembers().list) {
            if (member.getID() == slotNo) {
                if (member.getItemPath() == null)
                    throw new ObjectCannotBeUpdated("ClearSlot: Member slot " + slotNo + " already empty for collection "+collName);
                member.clearItem();
                stored = true;
                break;
            }
        }
        if (!stored) {
            throw new ObjectNotFoundException("ClearSlot: Member slot " + slotNo + " not found for collection "+collName);
        }

        try {
            Gateway.getStorage().put(item, agg, transactionKey);
        }
        catch (PersistencyException e) {
            log.error("Error storing collection {}", collName, e);
            throw new PersistencyException("ClearSlot: Error storing collection "+collName);
        }
        return requestData;
    }
}
