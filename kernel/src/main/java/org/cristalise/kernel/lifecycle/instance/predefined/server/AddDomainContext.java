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
package org.cristalise.kernel.lifecycle.instance.predefined.server;

import java.util.Arrays;
import java.util.Stack;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddDomainContext extends PredefinedStep {

    public AddDomainContext() {
        super("Creates an empty domain context in the tree");
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectAlreadyExistsException, CannotManageException
    {
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)params);

        if (params.length != 1) throw new InvalidDataException("AddDomainContext: Invalid parameters " + Arrays.toString(params));

        DomainPath pathToAdd = new DomainPath(params[0]);

        if (pathToAdd.exists(transactionKey))
            throw new ObjectAlreadyExistsException("Context " + pathToAdd + " already exists");

        // collect parent paths if they don't exist
        Stack<DomainPath> pathsToAdd = new Stack<DomainPath>();

        while (pathToAdd != null && !pathToAdd.exists()) {
            pathsToAdd.push(pathToAdd);
            pathToAdd = pathToAdd.getParent();
        }

        while (!pathsToAdd.empty()) {
            pathToAdd = pathsToAdd.pop();
            Gateway.getLookupManager().add(pathToAdd, transactionKey);
        }

        return requestData;
    }
}
