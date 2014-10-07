/**
 * This file is part of the CRISTAL-iSE default user interface.
 * Copyright (c) 2001-2014 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.gui.tree;

import org.cristalise.gui.ItemTabManager;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;


public class NodeRole extends NodeContext {

	RolePath role;
	public NodeRole(Path path, ItemTabManager desktop) {
		super(path, desktop);
		role = (RolePath)path;
	}
	@Override
	public void loadChildren() {
		AgentPath[] agents;
		try {
			agents = Gateway.getLookup().getAgents(role);
			for (AgentPath agentPath : agents) {
				add (newNode(agentPath));
			}
		} catch (ObjectNotFoundException e) {
			Logger.error("Role "+role.getName()+" not found");
		}
		super.loadChildren();
	}
	
	
}
