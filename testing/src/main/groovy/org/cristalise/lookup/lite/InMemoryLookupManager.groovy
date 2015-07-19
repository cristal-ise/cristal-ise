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
package org.cristalise.lookup.lite

import groovy.transform.CompileStatic

import java.security.NoSuchAlgorithmException

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.utils.Logger


@CompileStatic
class InMemoryLookupManager extends InMemoryLookup implements LookupManager {
    
    @Override
    public void initializeDirectory() throws ObjectNotFoundException {
        Logger.msg(8, "InMemoryLookupManager.initializeDirectory() - Do nothing");
    }

    @Override
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        Logger.msg(5, "InMemoryLookupManager.add() - Path: $newPath.string");

        if(cache.containsKey(newPath.getString())) { throw new ObjectAlreadyExistsException()}
        else {
            if(Path instanceof DomainPath) {
                Logger.msg(8, "InMemoryLookupManager.add() - Adding each DomainPath element")
                newPath.getPath().each {
                }
            }
            else cache[newPath.getString()] = newPath
        }
    }

    @Override
    public void delete(Path path) throws ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.delete() - Path: $path.string");
        
        if(cache.containsKey(path.getString())) {
            cache.remove(path.getString())
        }
        else {
            throw new ObjectCannotBeUpdated()
        }
    }

    @Override
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.createRole() - RolePath: $role");
        if(exists(role)) throw new ObjectAlreadyExistsException(role.toString())
        add(role)
        return role
    }

    @Override
    public void addRole(AgentPath agent, RolePath rolePath) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookupManager.addRole() - AgentPath: $agent, RolePath: $rolePath");

        if(! exists(agent))    throw new ObjectNotFoundException("$agent")
        if(! exists(rolePath)) throw new ObjectNotFoundException("$rolePath")

        if( agent2RolesCache[agent.string]?.find {it == rolePath.string} ) throw new ObjectCannotBeUpdated("Agent $agent already has role $rolePath")

        if(agent2RolesCache[agent.string] == null)    agent2RolesCache[agent.string] = []
        if(role2AgentsCache[rolePath.string] == null) role2AgentsCache[rolePath.string] = []

        agent2RolesCache[agent.string].add(rolePath.string)
        role2AgentsCache[rolePath.string].add(agent.string)
    }

    @Override
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        Logger.warning("InMemoryLookupManager.removeRole() - AgentPath: $agent, RolePath: $role - This implemetation does NOTHING");
        // TODO Auto-generated method stub
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        Logger.warning("InMemoryLookupManager.setAgentPassword() - AgentPath: $agent - This implemetation does NOTHING");
        // TODO Auto-generated method stub
    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        Logger.warning("InMemoryLookupManager.setAgentPassword() - RolePath: $role - This implemetation does NOTHING");
        // TODO Auto-generated method stub
    }
}
