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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.*;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyArrayList;

import static org.cristalise.kernel.lifecycle.instance.predefined.agent.Authenticate.REDACTED;

@Slf4j
public class CreateAgentFromDescription extends CreateItemFromDescription {

    public CreateAgentFromDescription() {
        super("Create a new agent using this item as its description");
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
        String[] inputs = getDataList(requestData);

        if (inputs == null || inputs.length < 2) throw new InvalidDataException("Invalid input data:"+requestData);

        String            newName   = inputs[0];
        String            contextS  = inputs[1];
        String[]          roles     = StringUtils.isNotBlank(inputs[2]) ? inputs[2].split(",") : new String[0];
        String            pwd       = inputs.length > 3 && StringUtils.isNotBlank(inputs[3]) ? inputs[3] : "";
        String            descVer   = inputs.length > 4 && StringUtils.isNotBlank(inputs[4]) ? inputs[4] : "last";
        PropertyArrayList initProps = inputs.length > 5 && StringUtils.isNotBlank(inputs[5]) ? unmarshallInitProperties(inputs[5]) : new PropertyArrayList();
        String            outcome   = inputs.length > 6 && StringUtils.isNotBlank(inputs[6]) ? inputs[6] : "";

        ItemProxy descItem = Gateway.getProxy(descItemPath, transactionKey);
        AgentProxy agent = Gateway.getAgentProxy(agentPath, transactionKey);

        // generate new agent path with new UUID
        log.debug("Called by {} on {} with parameters {}", agent, descItem, (Object)inputs);

        AgentPath newAgentPath = new AgentPath(new ItemPath(), newName);

        // check if the agent's name is already taken
        DomainPath context = new DomainPath(new DomainPath(contextS), newName);

        if (context.exists(transactionKey)) throw new ObjectAlreadyExistsException("The path " +context+ " exists already.");

        createAgentAddRoles(newAgentPath, roles, pwd, transactionKey);

        initialiseItem(newAgentPath, agent, descItem, initProps, outcome, newName, descVer, context, newAgentPath, transactionKey);

        if (inputs.length > 3) inputs[3] = REDACTED; // censor password from outcome

        return bundleData(inputs);
    }

    /**
     * Create AgentPath and add Roles to agent
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
