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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Import extends PredefinedStep {
    public Import() {
        super("Imports an outcome into the Item, with a given schema and viewpoint");
    }

    /**
     * Params: Schemaname_version:Viewpoint (optional), Outcome, Timestamp (optional)
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)params);

        int split1 = params[0].indexOf('_');
        int split2 = params[0].indexOf(':');

        if (split1 == -1) throw new InvalidDataException("Import: Invalid parameters " + Arrays.toString(params));

        requestData = params[1];
        Schema schema;
        String viewpoint = null;

        String schemaName = params[0].substring(0, split1);
        int schemaVersion;
        if (split2 > -1) {
            schemaVersion = Integer.parseInt(params[0].substring(split1 + 1, split2));
            viewpoint = params[0].substring(split2 + 1);
        }
        else schemaVersion = Integer.parseInt(params[0].substring(split1 + 1));

        schema = LocalObjectLoader.getSchema(schemaName, schemaVersion, transactionKey);

        String timestamp;
        if (params.length == 3) timestamp = params[2];
        else timestamp = DateUtility.timeToString(DateUtility.getNow());

        // write event, outcome and viewpoints to storage

        ClusterStorageManager storage = Gateway.getStorage();
        History hist = new History(item, transactionKey);
        Event event = hist.addEvent(agent, getCurrentAgentRole(), getName(), getPath(), getType(), schema,
                getStateMachine(), transitionID, viewpoint, timestamp);

        try {
            storage.put(item, new Outcome(event.getID(), requestData, schema), transactionKey);
            storage.put(item, new Viewpoint(item, schema, viewpoint, event.getID()), transactionKey);
            if (!"last".equals(viewpoint))
                storage.put(item, new Viewpoint(item, schema, "last", event.getID()), transactionKey);
        }
        catch (PersistencyException e) {
            storage.abort(transactionKey);
            throw e;
        }
//        storage.commit(transactionKey);
        return requestData;
    }
}
