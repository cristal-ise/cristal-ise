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

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.util.Arrays;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeName extends PredefinedStep {
    public static final String description = "Removes Items old Name, add the new Name and changes the Name property";

    public ChangeName() {
        super();
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectCannotBeUpdated, ObjectAlreadyExistsException, CannotManageException, ObjectNotFoundException
    {
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)params);

        if (params.length != 2) throw new InvalidDataException("ChangeName: Invalid parameters: "+Arrays.toString(params));

        String oldName = params[0];
        String newName = params[1];

        log.info("oldName:{} newName:{}", oldName, newName);

        if (oldName.equals(newName)) {
            log.info("oldName:{} == newName:{} - NOTHING DONE", oldName, newName);
            return requestData;
        }

        String currentNameProp = PropertyUtility.getProperty(item, NAME, transactionKey).getValue();

        if ( ! oldName.equals(currentNameProp)) {
            throw new InvalidDataException(item + " current Name propety '"+currentNameProp+"' is different from old name '"+oldName+"'");
        }

        // First find the DomainPath (alias) and change it. Note that 'pure' Agent does not have any DomainPath
        PagedResult result = Gateway.getLookup().searchAliases(item, 0, 100, transactionKey);
        DomainPath currentDP = null;
        DomainPath newDP = null;

        if (result.rows.size() > 0) {
            for (Path path: result.rows) {
                if (path.getName().equals(oldName)) {
                    currentDP = (DomainPath)path;
                    break;
                }
            }

            if (currentDP == null) throw new InvalidDataException(item + " does not domainPath with name:" + oldName);

            newDP = changeDomianPath(item, newName, currentDP, transactionKey);
        }

        // Update the Name property of the Item or Agent
        try {
            PropertyUtility.writeProperty(item, NAME, newName, transactionKey);
        }
        catch (Exception e) {
            log.error("", e);

            //recover original state
            if (newDP != null) {
                Gateway.getLookupManager().delete(newDP, transactionKey);
                Gateway.getLookupManager().add(currentDP, transactionKey);
            }

            throw new CannotManageException(e.getMessage());
        }

        return requestData;
    }
    
    private DomainPath changeDomianPath(ItemPath item, String newName, DomainPath currentDP, TransactionKey transactionKey) 
        throws ObjectCannotBeUpdated, ObjectAlreadyExistsException, CannotManageException
    {
        DomainPath rootDP = currentDP.getParent();
        DomainPath newDP = new DomainPath(rootDP, newName);
        newDP.setItemPath(item);

        // Throws an exception if newName exists
        Gateway.getLookupManager().add(newDP, transactionKey);

        try {
            Gateway.getLookupManager().delete(currentDP, transactionKey);
        }
        catch (Exception e) {
            log.error("Could not delete old domain path: " + currentDP.getStringPath(), e);

            //recover original state
            try {
                Gateway.getLookupManager().delete(newDP, transactionKey);
            }
            catch (Exception ex) {
                log.error("Could not delete new domain path: " + newDP.getStringPath(), ex);
            }

            throw new CannotManageException(e.getMessage());
        }
        return newDP;
    }
}
