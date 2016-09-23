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
package org.cristalise.dsl.entity

import groovy.transform.CompileStatic

import org.cristalise.kernel.common.CannotManageException
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.entity.CorbaServer
import org.cristalise.kernel.entity.agent.ActiveEntity
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.PropertyArrayList
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class AgentBuilder {

    public AgentBuilder() {}

    public static def create(Map<String, Object> attrs, Closure cl) {
        assert attrs && attrs.agent && (attrs.agent instanceof AgentPath)

        def item = build(attrs, cl)
        return null
    }

    public static ImportAgent build(Map<String, Object> attrs, Closure cl) {
        assert attrs && attrs.name
        return build((String)attrs.name, (String)attrs.pwd, cl)
    }

    public static ImportAgent build(String name, String pwd, Closure cl) {
        def agentD = new AgentDelegate(name, pwd)

        agentD.processClosure(cl)

        if(!agentD.newAgent.roles) throw new InvalidDataException("Agent '$name' does not have any Roles defined")

        return agentD.newAgent
    }

    public static AgentPath create(AgentPath agentPath, ImportAgent agent) {
        assert agentPath
        return (AgentPath)agent.create(agentPath, true)
    }
/*
    public AgentPath create(AgentPath agentPath) {
        assert agentPath
        Logger.msg(3, "AgentBuilder.create() - Creating CORBA Object");
        CorbaServer factory = Gateway.getCorbaServer();
        if (factory == null) throw new CannotManageException("This process cannot create new Items");
        AgentPath newAgentPath = new AgentPath(getItemPath(), name);
        if(pwd) newAgentPath.setPassword(pwd);
        ActiveEntity newAgent = factory.createAgent(newAgentPath);

        Logger.msg(3, "AgentBuilder.create() - Adding entity '$newAgentPath' to lookup");
        Gateway.getLookupManager().add(newAgentPath);

        Logger.msg(3, "AgentBuilder.create() - Initializing entity");
        try {
            newAgent.initialise(
                agent.getSystemKey(), 
                Gateway.getMarshaller().marshall(props), 
                wf == null ? null : Gateway.getMarshaller().marshall(wf.search("workflow/domain")),
                null);
        }
        catch (Exception ex) {
            Logger.error(ex);
            Gateway.getLookupManager().delete(newAgentPath);
            throw new InvalidDataException("Problem initializing new Agent '"+name+"'. See log: "+ex.getMessage());
        }

        Logger.msg(3, "AgentBuilder.create() - Creating roles");
        RoleBuilder.createRoles(roles).each { RolePath aRole ->
            Gateway.getLookupManager().addRole(newAgentPath, aRole);
        }

        return newAgentPath;
    }

    private ItemPath getItemPath() {
        try {
            return Gateway.getLookup().getAgentPath(name);
        }
        catch (ObjectNotFoundException ex) {
            return new AgentPath(new ItemPath(), name);
        }
    }
*/
}
