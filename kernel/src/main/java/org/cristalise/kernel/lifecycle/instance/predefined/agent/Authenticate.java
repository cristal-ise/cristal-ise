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

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

/**
 * 
 */
public class Authenticate extends PredefinedStep {
    public static final String description = "Records the Login event in the history. Login is assocated with user sessions with tokens validity";

    public Authenticate() {
        super();
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException
    {
        return authenticate(agent, itemPath, requestData, locker);
    }

    protected String authenticate(AgentPath agent, ItemPath itemPath, String requestData, Object locker) 
            throws InvalidDataException, ObjectNotFoundException
    {
        Logger.msg(1, "PredefinedStep.Authenticate() - Starting.");

        String[] params = getDataList(requestData);

        if (params.length < 2 || params.length > 3) 
            throw new InvalidDataException("PredefinedStep.Authenticate() -  Invalid number of parameters (2 <= length="+params.length+" <= 3)");

        Gateway.getSecurityManager().authenticate(params[0], params[1], null, false);

        params[1] = "REDACTED"; // censor password from outcome

        return bundleData(params);
    }

}
