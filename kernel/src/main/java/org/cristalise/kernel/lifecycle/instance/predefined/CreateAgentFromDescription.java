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

import static org.cristalise.kernel.lifecycle.instance.predefined.agent.Authenticate.REDACTED;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyArrayList;

import lombok.extern.slf4j.Slf4j;

 @Slf4j
public class CreateAgentFromDescription extends CreateItemFromDescription {

    public CreateAgentFromDescription() {
        super();
    }

    /**
     * Params:
     * <ol>
     * <li>Agent name</li>
     * <li>Domain context</li>
     * <li>Comma-delimited Role names to assign to the Agent</li>
     * <li>Password (optional)</li>
     * <li>Description version to use(optional)</li>
     * <li>Initial properties to set in the new Agent (optional)</li>
     * </ol>
     * @throws ObjectNotFoundException
     * @throws InvalidDataException The input parameters were incorrect
     * @throws ObjectAlreadyExistsException The Agent already exists
     * @throws CannotManageException The Agent could not be created
     * @throws ObjectCannotBeUpdated The addition of the new entries into the LookupManager failed
     * @throws PersistencyException
     * @see org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription#runActivityLogic(AgentPath, ItemPath, int, String, Object)
     */
    @Override
    protected String runActivityLogic(AgentPath agentPath, ItemPath descItemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws ObjectNotFoundException, 
                   InvalidDataException, 
                   ObjectAlreadyExistsException, 
                   CannotManageException, 
                   ObjectCannotBeUpdated, 
                   PersistencyException
    {
        String[] input = getDataList(requestData);

        String            newName   = input[0];
        String            contextS  = input[1];
        String[]          roles     = StringUtils.isNotBlank(input[2]) ? input[2].split(",") : new String[0];
        String            pwd       = input.length > 3 && StringUtils.isNotBlank(input[3]) ? input[3] : "";
        String            descVer   = input.length > 4 && StringUtils.isNotBlank(input[4]) ? input[4] : "last";
        PropertyArrayList initProps = input.length > 5 && StringUtils.isNotBlank(input[5]) ? unmarshallInitProperties(input[5]) : new PropertyArrayList();
        String            outcome   = input.length > 6 && StringUtils.isNotBlank(input[6]) ? input[6] : "";

        // generate new agent path with new UUID
        log.debug("Called by {} on {} with parameters {}", agentPath.getAgentName(transactionKey), descItemPath, (Object)input);

        AgentPath newAgentPath = new AgentPath(new ItemPath(), newName);

        // check if the agent's name is already taken
        if (Gateway.getLookup().exists(newAgentPath, transactionKey) )
            throw new ObjectAlreadyExistsException("The agent name " + newName + " exists already.");

        DomainPath context = new DomainPath(new DomainPath(contextS), newName);

        if (context.exists(transactionKey)) throw new ObjectAlreadyExistsException("The path " +context+ " exists already.");

        createAgentAddRoles(newAgentPath, roles, pwd, transactionKey);

        initialiseItem(newAgentPath, agentPath, descItemPath, initProps, outcome, newName, descVer, context, newAgentPath, transactionKey);

        if (input.length > 3) input[3] = REDACTED; // censor password from outcome

        return bundleData(input);
    }

    /**
     * Create Corba server, AgentPath and add Roles to agent
     * 
     * @param newAgentPath
     * @param roles
     * @return
     * @throws CannotManageException
     * @throws ObjectCannotBeUpdated
     * @throws ObjectAlreadyExistsException
     */
    protected void createAgentAddRoles(AgentPath newAgentPath, String[] roles, String pwd, TransactionKey transactionKey) 
            throws CannotManageException, ObjectCannotBeUpdated, ObjectAlreadyExistsException
    {
        log.info("createAgentAddRoles() - Creating Agent {}", newAgentPath.getAgentName(transactionKey));
        Gateway.getLookupManager().add(newAgentPath, transactionKey);

        try {
            if (StringUtils.isNotBlank(pwd)) Gateway.getLookupManager().setAgentPassword(newAgentPath, pwd, true, transactionKey);

            for (String roleName: roles) {
                if (StringUtils.isNotBlank(roleName)) {
                    RolePath role = Gateway.getLookupManager().getRolePath(roleName, transactionKey);
                    Gateway.getLookupManager().addRole(newAgentPath, role, transactionKey);
                }
            }
        }
        catch (Exception e) {
            log.error("createAgentAddRoles()", e);
            Gateway.getLookupManager().delete(newAgentPath, transactionKey);

            throw new CannotManageException(e.getMessage());
        }
    }
}
