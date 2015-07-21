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

        if(cache.containsKey(newPath.getString())) { throw new ObjectAlreadyExistsException("$newPath")}
        else {
            if(newPath instanceof RolePath) {
                createRole(newPath)
            }
            else if(newPath instanceof DomainPath) {
                Logger.msg(8, "InMemoryLookupManager.add() + Adding each DomainPath element")
                GString sPath
                newPath.getPath().each {
                    if(sPath) { sPath = "$sPath/$it" }
                    else      { sPath = "$it" }

                    if(exists(new DomainPath(sPath))) {
                        Logger.msg(8, "InMemoryLookupManager.add() + DomainPath '$sPath' already exists")
                    }
                    else {
                        DomainPath d = new DomainPath(sPath)
                        cache[d.string] = d
                        Logger.msg(8, "InMemoryLookupManager.add() + DomainPath '$d' was added")
                    }
                }
            }
            else cache[newPath.string] = newPath
        }
    }

    @Override
    public void delete(Path path) throws ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.delete() - Path: $path.string");

        if(exists(path)) {
            cache.remove(path.getString())
            //FIXME: remove path from role2AgentsCache and agent2RolesCache
        }
        else {
            throw new ObjectCannotBeUpdated("$path does not exists")
        }
    }

    @Override
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.createRole() - RolePath: $role");
        RolePath parent = new RolePath()

        if(!exists(parent)) cache[parent.string] = parent

        role.path.each { String name ->
            if(name == "agent") return

            RolePath child = new RolePath(parent, name)

            if(exists(child)) {
                Logger.msg(8, "InMemoryLookupManager.createRole() - RolePath '$child' already exists");
            }
            else {
                Logger.msg(5, "InMemoryLookupManager.createRole() - Adding RolePath '$child' ")
                cache[child.string] = child
            }
            parent = child
        }

        return parent
    }

    @Override
    public void addRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookupManager.addRole() - AgentPath: $agent, RolePath: $role");

        if(! exists(agent)) throw new ObjectNotFoundException("$agent")
        if(! exists(role))  throw new ObjectNotFoundException("$role")

        if( agent2RolesCache[agent.string]?.find {it == role.string} ) throw new ObjectCannotBeUpdated("Agent $agent already has role $role")

        if(agent2RolesCache[agent.string] == null) agent2RolesCache[agent.string] = []
        if(role2AgentsCache[role.string]  == null) role2AgentsCache[role.string] = []

        agent2RolesCache[agent.string].add(role.string)
        role2AgentsCache[role.string].add(agent.string)
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
