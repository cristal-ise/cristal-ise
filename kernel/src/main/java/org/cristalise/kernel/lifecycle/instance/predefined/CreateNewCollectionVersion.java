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

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateNewCollectionVersion extends PredefinedStep {

    /**
     * Constructor for Castor
     */
    public CreateNewCollectionVersion() {
        super("Creates a new numbered collection version in this Item from the current one.");
    }

    /**
     * Generates a new snapshot of a collection from its current state. 
     * The new version is given the next available number, starting at 0.
     * <pre>
     * Params:
     * 0 - Collection name
     * 1 - Version (optional)
     * </pre>
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey) 
            throws InvalidDataException, PersistencyException, ObjectNotFoundException 
    {
        // extract parameters
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)params);

        if (params.length == 0 || params.length > 2) { 
            throw new InvalidDataException("CreateNewCollectionVersion: Invalid parameters "+Arrays.toString(params));
        }

        String collName = params[0];
        Collection<?> coll = (Collection<?>)Gateway.getStorage().get(item, ClusterType.COLLECTION+"/"+collName+"/last", transactionKey);
        int newVersion;

        if (params.length > 1) {
            newVersion = Integer.valueOf(params[1]);
        }
        else {
            // find last numbered version
            String[] versions = Gateway.getStorage().getClusterContents(item, ClusterType.COLLECTION+"/"+collName, transactionKey);
            int lastVer = -1;

            for (String thisVerStr : versions) {
                try {
                    int thisVer = Integer.parseInt(thisVerStr);
                    if (thisVer > lastVer) lastVer = thisVer;
                }
                catch (NumberFormatException ex) { } // ignore non-integer versions
            }
            newVersion = lastVer + 1;
        }

        // Remove it from the cache before we change it
        Gateway.getStorage().clearCache(item, ClusterType.COLLECTION+"/"+collName+"/last");

        // Set the version & store it
        coll.setVersion(newVersion);
        Gateway.getStorage().put(item, coll, transactionKey);

        return requestData;
    }
}
