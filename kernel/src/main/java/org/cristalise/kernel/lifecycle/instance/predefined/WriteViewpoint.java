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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.LocalObjectLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WriteViewpoint extends PredefinedStep {

    public WriteViewpoint() {
        super("Writes a viewpoint to the Item");
    }

    /**
     * SchemaName, name and event Id. Event and Outcome should be checked so schema version should be discovered.
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, PersistencyException
    {
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(), item, (Object)params);

        if (params.length != 3) {
            throw new InvalidDataException("WriteViewpoint: Invalid parameters "+Arrays.toString(params));
        }

        String schemaName = params[0];
        String viewName   = params[1];
        int eventId;

        try {
            eventId = Integer.parseInt(params[2]);
        }
        catch (NumberFormatException ex) {
            throw new InvalidDataException("WriteViewpoint: Parameter 3 (EventId) must be an integer");
        }

        write(item, schemaName, viewName, eventId, transactionKey);

        return requestData;
    }

    public static void write(ItemPath item, String schemaName, String viewName, int eventId, TransactionKey transactionKey)
            throws PersistencyException, ObjectNotFoundException, InvalidDataException
    {
        Event event = (Event)Gateway.getStorage().get(item, ClusterType.HISTORY+"/"+eventId, transactionKey);

        if (StringUtils.isBlank(event.getSchemaName())) {
            throw new InvalidDataException("Event "+eventId+" does not reference an Outcome, so cannot be assigned to a Viewpoint.");
        }

        //checks Schema name/version
        Schema thisSchema = LocalObjectLoader.getSchema(schemaName, event.getSchemaVersion(), transactionKey);

        if (!event.getSchemaName().equals(thisSchema.getItemID())) { 
            throw new InvalidDataException("Event outcome schema is "+event.getSchemaName()+", and cannot be used for a "+schemaName+" Viewpoint");
        }

        Gateway.getStorage().put(item, new Viewpoint(item, thisSchema, viewName, eventId), transactionKey);
    }
}
