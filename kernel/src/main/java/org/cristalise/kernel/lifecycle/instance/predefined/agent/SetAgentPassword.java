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
package org.cristalise.kernel.lifecycle.instance.predefined.agent;

import static org.cristalise.kernel.security.BuiltInAuthc.ADMIN_ROLE;

import java.security.NoSuchAlgorithmException;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetAgentPassword extends PredefinedStep {

    public SetAgentPassword() {
        super();
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, AccessRightsException
    {
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)params);

        //FIXME params.length != 1 case is deprecated, shall enforce identity check
        if (params.length != 1 && params.length != 2) 
            throw new InvalidDataException("SetAgentPassword: Invalid number of parameters length:" + params.length);

        try {
            AgentPath targetAgent = new AgentPath(item);
            String newPwd;

            if (!targetAgent.equals(agent) && !agent.hasRole(ADMIN_ROLE.getName(), transactionKey))
                throw new AccessRightsException("Agent passwords may only be set by those Agents or by an Administrator");

            if (params.length == 1) {
                //FIXME these case is deprecated, shall enforce identity check
                newPwd = params[0];
                params[0] = "REDACTED"; // censor password from outcome
            }
            else {
                //Enforce identity check
                try {
                    Gateway.getSecurityManager().authenticate(agent.getAgentName(transactionKey), params[0], null, false, transactionKey);
                }
                catch (Exception e) {
                    throw new AccessRightsException("Authentication failed");
                }

                newPwd = params[1];
                params[0] = "REDACTED"; // censor password from outcome
                params[1] = "REDACTED"; // censor password from outcome
            }

            // Password is temporary when it was set by someone else
            Gateway.getLookupManager().setAgentPassword(targetAgent, newPwd, !targetAgent.equals(agent), transactionKey);

            return bundleData(params);
        }
        catch (InvalidItemPathException ex) {
            log.error("", ex);
            throw new InvalidDataException("Can only set password on an Agent. " + item + " is an Item.");
        }
        catch (NoSuchAlgorithmException e) {
            log.error("", e);
            throw new InvalidDataException("Cryptographic libraries for password hashing not found.");
        }
    }
}
