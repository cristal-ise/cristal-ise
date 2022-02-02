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

import java.util.Collections;
import java.util.ListIterator;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Erase extends PredefinedStep {
    public static final String description =  "Deletes all domain paths (aliases), roles (if agent) and clusters for this item or agent.";

    public Erase() {
        super();
    }

    /**
     * {@value #description}}
     * 
     * @param requestData is empty
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException
    {
        log.debug("Called by {} on {}", agent.getAgentName(transactionKey), item);

        removeAliases(item, transactionKey);
        removeRolesIfAgent(item, transactionKey);
        Gateway.getStorage().removeCluster(item, "", transactionKey); //removes all clusters

        log.info("Done item:"+item);

        return requestData;
    }

    /**
     * 
     * @param item
     * @throws ObjectNotFoundException
     * @throws ObjectCannotBeUpdated
     * @throws CannotManageException
     */
    private void removeAliases(ItemPath item, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException {
        PagedResult domPaths = Gateway.getLookup().searchAliases(item, 0, 100, transactionKey);

        if (domPaths.maxRows > domPaths.rows.size()) {
            throw new CannotManageException("Item:"+item.getItemName()+" has more than 100 DomainPath aliases");
        }

        // sort DomainPathes alphabetically
        Collections.sort(domPaths.rows, (o1, o2) -> (o1.getStringPath().compareTo(o2.getStringPath())));

        ListIterator<Path> listIter = domPaths.rows.listIterator(domPaths.rows.size());

        // Delete the DomainPathes in reverse alphabetical order to avoid 'Path is not a leaf error'
        while (listIter.hasPrevious()) {
            DomainPath path = (DomainPath) listIter.previous();
            log.info("removeAliases() - path:{}", path);
            Gateway.getLookupManager().delete(path, transactionKey);
        }
    }

    /**
     * 
     * @param item
     * @throws InvalidDataException
     * @throws ObjectCannotBeUpdated
     * @throws ObjectNotFoundException
     * @throws CannotManageException
     */
    private void removeRolesIfAgent(ItemPath item, TransactionKey transactionKey) throws InvalidDataException, ObjectCannotBeUpdated, ObjectNotFoundException, CannotManageException {
        try {
            AgentPath targetAgent = new AgentPath(item);
            
            //This check if the item is an agent or not
            if (targetAgent.getAgentName(transactionKey) != null) {
                for (RolePath role : targetAgent.getRoles(transactionKey)) {
                    Gateway.getLookupManager().removeRole(targetAgent, role, transactionKey);
                }
            }
        }
        catch (InvalidItemPathException e) {
            //this is actually never happens, new AgentPath(item) does not throw InvalidAgentPathException
            //but the exception is needed for 'backward compability'
        }
    }
}
