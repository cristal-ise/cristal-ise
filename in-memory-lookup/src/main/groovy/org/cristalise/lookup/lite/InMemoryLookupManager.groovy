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

import java.security.NoSuchAlgorithmException

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.persistency.TransactionKey
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic


@CompileStatic
@Singleton
class InMemoryLookupManager extends InMemoryLookup implements LookupManager {

    @Override
    public void initializeDirectory(TransactionKey transactionKey) throws ObjectNotFoundException {
        Logger.msg(8, "InMemoryLookupManager.initializeDirectory() - Do nothing");
    }

    @Override
    public void add(Path newPath, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        Logger.msg(5, "InMemoryLookupManager.add() - Path: $newPath");

        if(cache.containsKey(newPath.getStringPath())) { throw new ObjectAlreadyExistsException("$newPath")}
        else {
            if(newPath instanceof RolePath) {
                createRole(newPath, transactionKey)
            }
            else if(newPath instanceof DomainPath) {
                cache[newPath.stringPath] = newPath

                Logger.msg(8, "InMemoryLookupManager.add() + Adding each DomainPath element")
                GString sPath
                newPath.getPath().each {
                    if(sPath) { sPath = "$sPath/$it" }
                    else      { sPath = "$it" }

                    DomainPath d = new DomainPath(sPath)

                    if(exists(d, transactionKey)) {
                        Logger.msg(8, "InMemoryLookupManager.add() + DomainPath '$d' already exists")
                    }
                    else {
                        cache[d.stringPath] = d
                        Logger.msg(8, "InMemoryLookupManager.add() + DomainPath '$d' was added")
                    }
                }
            }
            else cache[newPath.stringPath] = newPath
        }
    }

    @Override
    public void delete(Path path, TransactionKey transactionKey) throws ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.delete() - Path: $path");

        if(exists(path, transactionKey)) {
            if(search(path, "", SearchConstraints.WILDCARD_MATCH, transactionKey).size() != 1 ) throw new ObjectCannotBeUpdated("Path $path is not a leaf")

            if(path instanceof RolePath && role2AgentsCache.containsKey(path.stringPath)) {
                Logger.msg(8, "InMemoryLookupManager.delete() - RolePath: $path");
                role2AgentsCache[path.stringPath].each { removeRole(new AgentPath(it), (RolePath)path, transactionKey) }
            }
            else if(path instanceof AgentPath && agent2RolesCache.containsKey(path.stringPath)) {
                Logger.msg(8, "InMemoryLookupManager.delete() - AgentPath: $path");
                agent2RolesCache[path.stringPath].each { removeRole((AgentPath)path, new RolePath(it.split("/"), false), transactionKey) }
            }

            cache.remove(path.stringPath)
            Logger.msg(8, "InMemoryLookupManager.delete() - $path removed");
        }
        else {
            throw new ObjectCannotBeUpdated("$path does not exists")
        }
    }

    @Override
    public RolePath createRole(RolePath role, TransactionKey transactionKey) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.createRole() - RolePath: $role");

        if(exists(role, transactionKey)) throw new ObjectAlreadyExistsException("$role")

        try                 { role.getParent(transactionKey) } 
        catch (Throwable t) { Logger.error(t); throw new ObjectCannotBeUpdated("Parent role for '$role' does not exists") }
        
        cache[role.stringPath] = role
    }

    @Override
    public void addRole(AgentPath agent, RolePath role, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookupManager.addRole() - AgentPath: $agent, RolePath: $role");

        if(! exists(agent, transactionKey)) throw new ObjectNotFoundException("$agent")
        if(! exists(role, transactionKey))  throw new ObjectNotFoundException("$role")

        if( agent2RolesCache[agent.stringPath]?.find {it == role.stringPath} ) throw new ObjectCannotBeUpdated("Agent '$agent' already has role '$role'")

        if(agent2RolesCache[agent.stringPath] == null) agent2RolesCache[agent.stringPath] = []
        if(role2AgentsCache[role.stringPath]  == null) role2AgentsCache[role.stringPath] = []

        agent2RolesCache[agent.stringPath].add(role.stringPath)
        role2AgentsCache[role.stringPath].add(agent.stringPath)
    }

    @Override
    public void removeRole(AgentPath agent, RolePath role, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookupManager.removeRole() - AgentPath: $agent, RolePath: $role");

        if(! exists(agent, transactionKey)) throw new ObjectNotFoundException("$agent")
        if(! exists(role, transactionKey))  throw new ObjectNotFoundException("$role")

        if(! agent2RolesCache[agent.stringPath]?.find {it == role.stringPath} ) throw new ObjectCannotBeUpdated("Agent '$agent' has not got such role '$role'")

        List<String> roles = agent2RolesCache[agent.stringPath]

        roles.remove(role)
        Logger.msg(5, "InMemoryLookupManager.removeRole() - AgentPath: $agent, RolePath: $role -> DONE");
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword, boolean temporary, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        ((AgentPath)retrievePath(agent.stringPath))
        Logger.msg(5, "InMemoryLookupManager.setAgentPassword() - AgentPath: $agent NOTHING DONE");
    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        Logger.msg(5, "InMemoryLookupManager.setHasJobList() - RolePath: $role, hasJobList: $hasJobList");
        ((RolePath)retrievePath(role.stringPath)).setHasJobList(hasJobList)
    }

    @Override
    public void setIOR(ItemPath item, String ior, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        ((ItemPath)retrievePath(item.stringPath)).setIORString(ior)
    }
    
    @Override
    public void setPermission(RolePath role, String permission, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        setPermissions(role, [permission], transactionKey)
    }

    @Override
    public void setPermissions(RolePath role, List<String> permissions, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        ((RolePath)retrievePath(role.stringPath)).setPermissions(permissions)
    }

    @Override
    public void postStartServer() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void postBoostrap() {
        // TODO Auto-generated method stub
        
    }
}
