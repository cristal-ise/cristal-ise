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

import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;

public class AgentPredefinedStepContainer extends PredefinedStepContainer {

    @Override
    public void createChildren() {
        super.createChildren();
        predInit(RemoveAgent.class, "Deletes the Agent", new RemoveAgent());
        predInit(SetAgentPassword.class, "Changes the Agent's password", new SetAgentPassword());
        predInit(SetAgentRoles.class, "Sets the roles of the Agent", new SetAgentRoles());
        predInit(Authenticate.class, Authenticate.description, new Authenticate());
        predInit(Login.class, Login.description, new Login());
        predInit(LoginTimeout.class, LoginTimeout.description, new LoginTimeout());
        predInit(Logout.class, Logout.description, new Logout());
        predInit(ForcedLogout.class, ForcedLogout.description, new ForcedLogout());
        predInit(Sign.class, Sign.description, new Sign());
        predInit(RefreshJobList.class, RefreshJobList.description, new RefreshJobList());
    }
}
