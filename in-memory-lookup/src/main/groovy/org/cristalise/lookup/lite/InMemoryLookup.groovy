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
package org.cristalise.lookup.lite;

import groovy.transform.CompileStatic

import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.InvalidItemPathException
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.Lookup
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.auth.Authenticator
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.utils.Logger

@CompileStatic
abstract class InMemoryLookup implements Lookup {

    protected Map cache = [:] //LinkedHashMap
    
    //Maps String RolePath to List of String AgentPath
    protected Map<String,List<String>> role2AgentsCache  = [:]

    //Maps String RolePath to List of String AgentPath
    protected Map<String,List<String>> agent2RolesCache = [:]
    
    private Iterator<Path> getEmptyPathIter() {
        return new Iterator<Path>() {
            public boolean hasNext() { return false; }
            public Path next() { return null; }
            public void remove() {}
        };
    }

    /**
     * 
     * 
     * @param key
     * @return
     */
    protected Path retrievePath(String key) throws ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.retrievePath() - key: $key")

        Path p = (Path) cache[key]
        
        if(p) return p
        else throw new ObjectNotFoundException("$key does not exist")
    }

    /**
     * Connect to the directory using the credentials supplied in the Authenticator. 
     * 
     * @param user The connected Authenticator. The Lookup implementation may use the AuthObject in this to communicate with the database.
     */
    @Override
    public void open(Authenticator user) {
        Logger.msg(8, "InMemoryLookup.open(user) - Do nothing")
    }

    /**
     * Shutdown the lookup
     */
    @Override
    public void close() {
        Logger.msg(8, "InMemoryLookup.close() - Do nothing");
        cache.clear()
    }

    /**
     * Fetch the correct subclass class of ItemPath for a particular Item, derived from its lookup entry. 
     * This is used by the CORBA Server to make sure the correct Item subclass is used. 
     * 
     * @param sysKey The system key of the Item
     * @return an ItemPath or AgentPath
     * @throws InvalidItemPathException When the system key is invalid/out-of-range
     * @throws ObjectNotFoundException When the Item does not exist in the directory.
     */
    @Override
    public ItemPath getItemPath(String sysKey) throws InvalidItemPathException, ObjectNotFoundException {
        return (ItemPath) retrievePath(new ItemPath(sysKey).string)
    }

    @Override
    public AgentPath getAgentPath(String agentName) throws ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.getAgentPath() - agentName: $agentName")

        def pList = cache.values().findAll {it instanceof AgentPath && ((AgentPath)it).agentName ==  agentName}

        if     (pList.size() == 0) throw new ObjectNotFoundException("$agentName")
        else if(pList.size() > 1)  throw new ObjectNotFoundException("Umbiguous result for agent '$agentName'")

        Logger.msg(5, "InMemoryLookup.getAgentPath() - agentName '$agentName' was found")

        return (AgentPath)pList[0]
    }

    @Override
    public RolePath getRolePath(String roleName) throws ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.getRolePath() - roleName: $roleName")

        def pList = cache.values().findAll {it instanceof RolePath && ((RolePath)it).name ==  roleName}

        if     (pList.size() == 0) throw new ObjectNotFoundException("$roleName")
        else if(pList.size() > 1)  throw new ObjectNotFoundException("Umbiguous result for agent '$roleName'")

        Logger.msg(5, "InMemoryLookup.getRolePath() - roleName '$roleName' was found")

        return (RolePath)pList[0]
    }

    /**
     * Find the ItemPath for which a DomainPath is an alias.
     * 
     * @param domainPath The path to resolve
     * @return The ItemPath it points to (should be an AgentPath if the path references an Agent)
     * @throws InvalidItemPathException
     * @throws ObjectNotFoundException
     */
    @Override
    public ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.resolvePath() - domainPath: $domainPath")
        DomainPath dp = (DomainPath) retrievePath(domainPath.string)
        return dp.getItemPath()
    }

    /**
     * Resolve a path to a CORBA Object Item or Agent
     * 
     * @param path The path to be resolved
     * @return The CORBA Object
     * @throws ObjectNotFoundException When the Path doesn't exist, or doesn't have an IOR associated with it
     */
    @Override
    public String getIOR(Path path) throws ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.getIOR() - Path: $path")
        return retrievePath(path.string).IOR.toString()
    }

    /**
     * Checks if a particular Path exists in the directory
     * 
     * @param path The path to check
     * @return boolean true if the path exists, false if it doesn't
     */
    @Override
    public boolean exists(Path path) {
        //Logger.msg(5, "InMemoryLookup.exists() - Path: $path");
        return cache.keySet().contains(path.string)
    }

    @Override
    public Iterator<Path> getChildren(Path path) {
        Logger.msg(5, "InMemoryLookup.getChildren() - Path: $path")
        return cache.values().findAll { ((Path)it).string =~ /^$path.string\/\w+$/ }.iterator()
    }

    @Override
    public Iterator<Path> search(Path start, String name) {
        Logger.msg(5, "InMemoryLookup.search(Name: $name) - start: $start");
        def result = cache.values().findAll { ((Path)it).string =~ /^$start.string.*$name/ }
        Logger.msg(5, "InMemoryLookup.search(Name: $name) - returning ${result.size()} pathes");
        return result.iterator()
    }

    @Override
    public Iterator<Path> search(Path start, Property... props) {
        // TODO: Implement search(Path, props)
        Logger.warning("InMemoryLookup.search() - Path: $start, # of props: $props.length, props[0]: ${props[0].name} - ${props[0].value} -This implemetation ALWAYS returns empty result!");
        return getEmptyPathIter();
    }

    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props) {
        // TODO: Implement search(Path,PropDescList)
        Logger.warning("InMemoryLookup.search() - Path: $start, # of propDescList: $props.list.size - This implemetation ALWAYS returns empty result!");
        return getEmptyPathIter();
    }

    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath) {
        // TODO: Implement searchAliases
        Logger.warning("InMemoryLookup.searchAliases() - ItemPath: $itemPath - This implemetation ALWAYS returns empty result!");
        return getEmptyPathIter();
    }

    @Override
    public AgentPath[] getAgents(RolePath role) throws ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.getAgents() - RolePath: $role");
        List<String> agents = role2AgentsCache[retrievePath(role.string).string]

        if(agents) {
            AgentPath[] retVal = new AgentPath[agents.size()]
            agents.eachWithIndex { key, i -> retVal[i] = (AgentPath) retrievePath(key) }
            return retVal
        }
        
        return new AgentPath[0];
    }

    @Override
    public RolePath[] getRoles(AgentPath agent) {
        Logger.msg(5,"InMemoryLookup.getRoles() - AgentPath: $agent");

        try {
            List roles = agent2RolesCache[retrievePath(agent.string).string]

            if(roles) {
                RolePath[] retVal = new RolePath[roles.size()]
                roles.eachWithIndex {key, i -> retVal[i] = (RolePath) retrievePath(key) }
                return retVal
            }
        } 
        catch (Exception e) {}

        return new RolePath[0];
    }

    @Override
    public boolean hasRole(AgentPath agent, RolePath role) {
        Logger.msg(5, "InMemoryLookup.hasRole() - AgentPath: $agent, RolePath: $role");
        try {
            return agent2RolesCache[retrievePath(agent.string).string].contains(role.string)
        }
        catch(Exception e) {
            return false
        }
    }

    @Override
    public String getAgentName(AgentPath agentPath) throws ObjectNotFoundException {
        Logger.msg(5, "InMemoryLookup.getAgentName() - AgentPath: $agentPath");
        AgentPath p = (AgentPath) retrievePath(agentPath.string)
        return p.agentName;
    }
}
