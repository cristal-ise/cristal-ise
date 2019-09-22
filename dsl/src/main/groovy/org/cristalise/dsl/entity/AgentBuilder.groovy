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
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.PropertyArrayList


/**
 *
 */
@CompileStatic
class AgentBuilder {

    public AgentBuilder() {}

    public static AgentPath create(Map<String, Object> attrs, Closure cl) {
        assert attrs && attrs.agent && (attrs.agent instanceof AgentPath)

        return create((AgentPath)attrs.agent, build(attrs, cl))
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

    public static AgentPath create(AgentPath builderAgent, ImportAgent newAgent) {
        assert builderAgent && newAgent
        return (AgentPath)newAgent.create(builderAgent, true)
    }
}
