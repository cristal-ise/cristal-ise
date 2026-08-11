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

import org.apache.commons.lang3.NotImplementedException
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.InvalidItemPathException
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.Lookup
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.persistency.ClusterStorage
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.TransactionKey

import org.cristalise.kernel.property.Property
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.storage.MemoryOnlyClusterStorage
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


@CompileStatic
@Slf4j
abstract class InMemoryLookup extends ClusterStorage implements Lookup {

    @Delegate MemoryOnlyClusterStorage propertyStore = new MemoryOnlyClusterStorage()

    protected Map<String, Path> cache = [:] //LinkedHashMap

    //Maps String RolePath to List of String AgentPath
    protected Map<String,List<String>> role2AgentsCache  = [:]

    //Maps String RolePath to List of String AgentPath
    protected Map<String,List<String>> agent2RolesCache = [:]

    private Iterator<Path> getEmptyPathIter() {
        return new Iterator<Path>() {
                    public boolean hasNext() { return false }
                    public Path next() { return null }
                    public void remove() {}
                }
    }

    public void clear() {
        log.info("clear() - Clearing lookup cache and property store")
        cache.clear()
        propertyStore.clear()
        role2AgentsCache.clear()
        agent2RolesCache.clear()
    }

    /**
     *
     *
     * @param key
     * @return
     */
    protected Path retrievePath(String key) throws ObjectNotFoundException {
        log.debug("retrievePath() - key: $key")

        Path p = (Path) cache[key]

        if(p) return p
        else throw new ObjectNotFoundException("$key does not exist")
    }

    /**
     * Connect to lookup
     */
    @Override
    public void open() {
        log.info("open() - Do nothing")
        clear()
    }

    /**
     * Shutdown the lookup
     */
    @Override
    public void close() {
        log.info("close() - Do nothing")
        clear()
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
    public ItemPath getItemPath(String sysKey, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException {
        return (ItemPath) retrievePath(new ItemPath(sysKey).stringPath)
    }

    @Override
    public AgentPath getAgentPath(String agentName, TransactionKey transactionKey) throws ObjectNotFoundException {
        log.debug("getAgentPath() - agentName: $agentName")

        def pList = cache.values().findAll {it instanceof AgentPath && ((AgentPath)it).agentName ==  agentName}

        if     (pList.size() == 0) throw new ObjectNotFoundException("$agentName")
        else if(pList.size() > 1)  throw new ObjectNotFoundException("Umbiguous result for agent '$agentName'")

        log.debug("getAgentPath() - agentName '$agentName' was found")

        return (AgentPath)pList[0]
    }

    @Override
    public RolePath getRolePath(String roleName, TransactionKey transactionKey) throws ObjectNotFoundException {
        log.debug("getRolePath() - roleName: $roleName")

        def pList = cache.values().findAll {it instanceof RolePath && ((RolePath)it).name ==  roleName}

        if     (pList.size() == 0) throw new ObjectNotFoundException("$roleName")
        else if(pList.size() > 1)  throw new ObjectNotFoundException("Umbiguous result for agent '$roleName'")

        log.debug("getRolePath() - roleName '$roleName' was found")

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
    public ItemPath resolvePath(DomainPath domainPath, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException {
        log.debug("resolvePath() - domainPath: $domainPath")
        DomainPath dp = (DomainPath) retrievePath(domainPath.stringPath)
        return dp.getTarget()
    }

    /**
     * Checks if a particular Path exists in the directory
     *
     * @param path The path to check
     * @return boolean true if the path exists, false if it doesn't
     */
    @Override
    public boolean exists(Path path, TransactionKey transactionKey) {
        //log.debug("exists() - Path: $path");
        return cache.keySet().contains(path.stringPath)
    }

    @Override
    public PagedResult getChildren(Path path, int offset, int limit, TransactionKey transactionKey) {
        //cache.values().findAll { ((Path)it).stringPath =~ /^$path.stringPath\/\w+$/ }
        throw new NotImplementedException("Retrieving children of PagedResult is not implemented");
    }

    @Override
    public Iterator<Path> getChildren(Path path, TransactionKey transactionKey) {
        log.debug("getChildren() - Path: $path")
        return cache.values().findAll { ((Path)it).stringPath =~ /^$path.stringPath\/\w+$/ }.iterator()
    }

    @Override
    public PagedResult getChildren(Path path, int offset, int limit, boolean contextOnly, TransactionKey transactionKey) {
        if (!contextOnly) getChildren(path, offset, limit, transactionKey);

        throw new NotImplementedException("Retrieving only children of DomainContext is not implemented");
    }

    @Override
    public Iterator<Path> search(Path start, String name, SearchConstraints constraints, TransactionKey transactionKey) {
        log.debug("search(name: $name) - start: $start")
        def pattern = "^${start.stringPath}.*$name"
        if (constraints == SearchConstraints.EXACT_NAME_MATCH) pattern = "^${start.stringPath}/.*/$name\$"
        def result = cache.values().findAll { ((Path)it).stringPath =~ /$pattern/ }
        log.debug("search(name: $name) - returning ${result.size()} pathes")
        return result.iterator()
    }

    @Override
    public Iterator<Path> search(Path start, TransactionKey transactionKey, Property... props) {
        log.debug("search(props) - Start: $start, # of props: $props.length")
        String name = ""

        for(def prop in props) {
            log.debug("search(props) - Property: ${prop.name} - ${prop.value}")
            if(prop.name == "Name") name = prop.value
        }

        List<Path> result = []
        def foundPathes = search(start, name, SearchConstraints.WILDCARD_MATCH, transactionKey)

        foundPathes.each { Path p ->
            ItemPath ip = null

            if     (p instanceof DomainPath) { if(!((DomainPath)p).isContext()) ip = ((DomainPath)p).itemPath }
            else if(p instanceof ItemPath)   { ip = (ItemPath)p}

            if(ip && checkItemProps(ip, transactionKey, props)) { result.add(p) }
        }
        log.debug("search(props) - returning ${result.size()} pathes")
        return result.iterator()
    }

    private boolean checkItemProps(ItemPath itemP, TransactionKey transactionKey, Property... props) {
        log.debug("checkItemProps(props) - ItemPath:$itemP # of props: $props.length")

        for(Property prop: props) {
            Property p = (Property)propertyStore.get(itemP.itemPath, ""+ClusterType.PROPERTY+"/"+prop.name, transactionKey)
            if(!p || p.value != prop.value) return false
        }
        return true
    }

    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props, TransactionKey transactionKey) {
        // TODO: Implement search(Path,PropDescList)
        throw new RuntimeException("search() - UNIMPLEMENTED Start: $start, # of propDescList: $props.list.size - This implemetation ALWAYS returns empty result!")
        //return getEmptyPathIter();
    }

    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath, TransactionKey transactionKey) {
        // TODO: Implement searchAliases
        throw new RuntimeException("searchAliases() - UNIMPLEMENTED ItemPath: $itemPath - This implemetation ALWAYS returns empty result!")
        //return getEmptyPathIter();
    }

    @Override
    public AgentPath[] getAgents(RolePath role, TransactionKey transactionKey) throws ObjectNotFoundException {
        log.debug("getAgents() - RolePath: $role")
        List<String> agents = role2AgentsCache[retrievePath(role.stringPath).stringPath]

        if(agents) {
            AgentPath[] retVal = new AgentPath[agents.size()]
            agents.eachWithIndex { key, i -> retVal[i] = (AgentPath) retrievePath(key) }
            return retVal
        }

        return new AgentPath[0]
    }

    @Override
    public RolePath[] getRoles(AgentPath agent, TransactionKey transactionKey) {
        log.debug("getRoles() - AgentPath: $agent")

        try {
            List roles = agent2RolesCache[retrievePath(agent.stringPath).stringPath]

            if(roles) {
                RolePath[] retVal = new RolePath[roles.size()]
                roles.eachWithIndex {key, i -> retVal[i] = (RolePath) retrievePath(key) }
                return retVal
            }
        }
        catch (Exception e) {}

        return new RolePath[0]
    }

    @Override
    public boolean hasRole(AgentPath agent, RolePath role, TransactionKey transactionKey) {
        log.debug("hasRole() - AgentPath: $agent, RolePath: $role")
        try {
            return agent2RolesCache[retrievePath(agent.stringPath).stringPath].contains(role.stringPath)
        }
        catch(Exception e) {
            return false
        }
    }

    @Override
    public String getAgentName(AgentPath agentPath, TransactionKey transactionKey) throws ObjectNotFoundException {
        log.debug("getAgentName() - AgentPath: $agentPath")
        AgentPath p = (AgentPath) retrievePath(agentPath.stringPath)
        return p.getAgentName(transactionKey)
    }

    @Override
    public PagedResult search(Path start, List<Property> props, int offset, int limit, TransactionKey transactionKey) {
        throw new NotImplementedException("search is not implemented");
    }

    @Override
    public PagedResult search(Path start, PropertyDescriptionList props, int offset, int limit, TransactionKey transactionKey) {
        throw new NotImplementedException("search is not implemented");
    }

    @Override
    public PagedResult searchAliases(ItemPath itemPath, int offset, int limit, TransactionKey transactionKey) {
        throw new NotImplementedException("searchAliases is not implemented");
    }

    @Override
    public PagedResult getAgents(RolePath rolePath, int offset, int limit, TransactionKey transactionKey) throws ObjectNotFoundException {
        throw new NotImplementedException("getAgents is not implemented");
    }

    @Override
    public PagedResult getRoles(AgentPath agentPath, int offset, int limit, TransactionKey transactionKey) {
        throw new NotImplementedException("getRoles is not implemented");
    }

    @Override
    public PagedResult getContextTree(DomainPath path, TransactionKey transactionKey) {
        throw new NotImplementedException("Retrieving ContextTree support is not implemented");
    }
}
