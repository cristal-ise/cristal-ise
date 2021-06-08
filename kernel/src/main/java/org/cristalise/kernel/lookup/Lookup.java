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
package org.cristalise.kernel.lookup;

import static org.cristalise.kernel.lookup.Lookup.SearchConstraints.WILDCARD_MATCH;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;


/**
 *
 */
public interface Lookup {

    /**
     *
     */
    public class PagedResult {
        public int maxRows;
        public List<Path> rows;

        public PagedResult() {
            maxRows = 0;
            rows =  new ArrayList<>();
        }

        public PagedResult(int size, List<Path> result) {
            maxRows = size;
            rows = result;
        }
    }

    public enum SearchConstraints { EXACT_NAME_MATCH, WILDCARD_MATCH };

    /**
     * Connect to the directory using the credentials supplied in the Authenticator.
     *
     * @param user The connected Authenticator. The Lookup implementation may use the AuthObject in this to communicate with the database.
     */
    public void open(Authenticator user);

    /**
     * Shutdown the lookup
     */
    public void close();

    /**
     * Fetch the correct subclass class of ItemPath for a particular Item, derived from its lookup entry.
     *
     * @param sysKey The system key of the Item
     * @return an ItemPath or AgentPath
     * @throws InvalidItemPathException When the system key is invalid/out-of-range
     * @throws ObjectNotFoundException When the Item does not exist in the directory.
     */
    public default ItemPath getItemPath(String sysKey) throws InvalidItemPathException, ObjectNotFoundException {
        return getItemPath(sysKey, null);
    }

    /**
     * Fetch the correct subclass class of ItemPath for a particular Item, derived from its lookup entry.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param sysKey The system key of the Item
     * @param transactionKey identifier of the active transaction
     * @return an ItemPath or AgentPath
     * @throws InvalidItemPathException When the system key is invalid/out-of-range
     * @throws ObjectNotFoundException When the Item does not exist in the directory.
     */
    public ItemPath getItemPath(String sysKey, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException;

    /**
     * Find the ItemPath for which a DomainPath is an alias.
     *
     * @param domainPath The path to resolve
     * @return The ItemPath it points to (should be an AgentPath if the path references an Agent)
     */
    public default ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException {
        return resolvePath(domainPath, null);
    }

    /**
     * Find the ItemPath for which a DomainPath is an alias.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param domainPath The path to resolve
     * @param transactionKey identifier of the active transaction
     * @return The ItemPath it points to (should be an AgentPath if the path references an Agent)
     */
    public ItemPath resolvePath(DomainPath domainPath, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException;

    /**
     * Checks if a particular Path exists in the directory
     * 
     * @param path The path to check
     * @return boolean true if the path exists, false if it doesn't
     */
    public default boolean exists(Path path) {
        return exists(path, null);
    }

    /**
     * Checks if a particular Path exists in the directory
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param path The path to check
     * @param transactionKey identifier of the active transaction
     * @return boolean true if the path exists, false if it doesn't
     */
    public boolean exists(Path path, TransactionKey transactionKey);

    /**
     * List the next-level-deep children of a Path
     *
     * @param path The parent Path
     * @return An Iterator of child Paths
     */
    public default Iterator<Path> getChildren(Path path) {
        return getChildren(path, null);
    };

    /**
     * List the next-level-deep children of a Path
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param path The parent Path
     * @param transactionKey identifier of the active transaction
     * @return An Iterator of child Paths
     */
    public Iterator<Path> getChildren(Path path, TransactionKey transactionKey);

    /**
     * List the next-level-deep children of a Path
     *
     * @param path The parent Path
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @return A List of child Paths
     */
    public default PagedResult getChildren(Path path, int offset, int limit) {
        return getChildren(path, offset, limit, null);
    }

    /**
     * List the next-level-deep children of a Path
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param path The parent Path
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @param transactionKey identifier of the active transaction
     * @return A List of child Paths
     */
    public PagedResult getChildren(Path path, int offset, int limit, TransactionKey transactionKey);

    /**
     * Find a path with a particular name (last component).  Uses WILDCARD_MATCH as default constraints.
     *
     * @param start Search root
     * @param name The name to search for
     * @return An Iterator of matching Paths. Should be an empty Iterator if there are no matches.
     */
    public default Iterator<Path> search(Path start, String name) {
        return search(start, name, WILDCARD_MATCH, (TransactionKey)null);
    }

    /**
     * Find a path with a particular name (last component)
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param start Search root
     * @param name The name to search for
     * @param constraints to optimise the backend query
     * @param transactionKey identifier of the active transaction
     * @return An Iterator of matching Paths. Should be an empty Iterator if there are no matches.
     */
    public Iterator<Path> search(Path start, String name, SearchConstraints constraints, TransactionKey transactionKey);

    /**
     * Search for Items in the specified path with the given property list
     *
     * @param start Search root
     * @param props list of Properties
     * @return An Iterator of matching Paths
     */
    public default Iterator<Path> search(Path start, Property... props) {
        return search(start, (TransactionKey)null, props);
    }

    /**
     * Search for Items in the specified path with the given property list
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param start Search root
     * @param transactionKey identifier of the active transaction
     * @param props list of Properties
     * @return An Iterator of matching Paths
     */
    public Iterator<Path> search(Path start, TransactionKey transactionKey, Property... props);

    /**
     * Search for Items in the specified path with the given property list
     *
     * @param start Search root
     * @param props list of Properties
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @return PagedResult of matching Paths
     */
    public default PagedResult search(Path start, List<Property> props, int offset, int limit) {
        return search(start, props, offset, limit, null);
    }

    /**
     * Search for Items in the specified path with the given property list
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param start Search root
     * @param props list of Properties
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @param transactionKey identifier of the active transaction
     * @return PagedResult of matching Paths
     */
    public PagedResult search(Path start, List<Property> props, int offset, int limit, TransactionKey transactionKey);

    /**
     * Search for Items of a particular type, based on its PropertyDescription outcome
     *
     * @param start Search root
     * @param props Properties unmarshalled from an ItemDescription's property description outcome.
     * @return An Iterator of matching Paths
     */
    public default Iterator<Path> search(Path start, PropertyDescriptionList props) {
        return search(start, props, (TransactionKey)null);
    }

    /**
     * Search for Items of a particular type, based on its PropertyDescription outcome
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param start Search root
     * @param props Properties unmarshalled from an ItemDescription's property description outcome.
     * @param transactionKey identifier of the active transaction
     * @return An Iterator of matching Paths
     */
    public Iterator<Path> search(Path start, PropertyDescriptionList props, TransactionKey transactionKey);

    /**
     * Search for Items of a particular type, based on its PropertyDescription outcome
     *
     * @param start Search root
     * @param props Properties unmarshalled from an ItemDescription's property description outcome.
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @return An PagedResult of matching Paths
     */
    public default PagedResult search(Path start, PropertyDescriptionList props, int offset, int limit) {
        return search(start, props, offset, limit, null);
    }

    /**
     * Search for Items of a particular type, based on its PropertyDescription outcome
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param start Search root
     * @param props Properties unmarshalled from an ItemDescription's property description outcome.
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @param transactionKey identifier of the active transaction
     * @return An PagedResult of matching Paths
     */
    public PagedResult search(Path start, PropertyDescriptionList props, int offset, int limit, TransactionKey transactionKey);

    /**
     * Find all DomainPaths that are aliases for a particular Item or Agent
     * 
     * @param itemPath The ItemPath
     * @return An Iterator of DomainPaths that are aliases for that Item
     */
    public default Iterator<Path> searchAliases(ItemPath itemPath) {
        return searchAliases(itemPath, null);
    }

    /**
     * Find all DomainPaths that are aliases for a particular Item or Agent
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param itemPath The ItemPath
     * @param transactionKey identifier of the active transaction
     * @return An Iterator of DomainPaths that are aliases for that Item
     */
    public Iterator<Path> searchAliases(ItemPath itemPath, TransactionKey transactionKey);

    /**
     * Find all DomainPaths that are aliases for a particular Item or Agent
     *
     * @param itemPath The ItemPath
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @return An PagedResult of DomainPaths that are aliases for that Item
     */
    public default PagedResult searchAliases(ItemPath itemPath, int offset, int limit) {
        return searchAliases(itemPath, offset, limit, null);
    }

    /**
     * Find all DomainPaths that are aliases for a particular Item or Agent
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param itemPath The ItemPath
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @param transactionKey identifier of the active transaction
     * @return An PagedResult of DomainPaths that are aliases for that Item
     */
    public PagedResult searchAliases(ItemPath itemPath, int offset, int limit, TransactionKey transactionKey);

    /**
     * Find the AgentPath for the named Agent
     *
     * @param agentName then name of the Agent
     * @return the AgentPath representing the Agent
     */
    public default AgentPath getAgentPath(String agentName) throws ObjectNotFoundException {
        return getAgentPath(agentName, null);
    }

    /**
     * Find the AgentPath for the named Agent
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param agentName then name of the Agent
     * @param transactionKey identifier of the active transaction
     * @return the AgentPath representing the Agent
     */
    public AgentPath getAgentPath(String agentName, TransactionKey transactionKey) throws ObjectNotFoundException;

    /**
     * Find the RolePath for the named Role
     *
     * @param roleName the name of the Role
     * @return the RolePath representing the Role
     */
    public default RolePath getRolePath(String roleName) throws ObjectNotFoundException {
        return getRolePath(roleName, null);
    }

    /**
     * Find the RolePath for the named Role
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param roleName the name of the Role
     * @param transactionKey identifier of the active transaction
     * @return the RolePath representing the Role
     */
    public RolePath getRolePath(String roleName, TransactionKey transactionKey) throws ObjectNotFoundException;

    /**
     * Returns all of the Agents in this centre who hold this role (including sub-roles)
     *
     * @param rolePath the path representing the given Role
     * @return the list of Agents
     */
    public default AgentPath[] getAgents(RolePath rolePath) throws ObjectNotFoundException {
        return getAgents(rolePath, null);
    }

    /**
     * Returns all of the Agents in this centre who hold this role (including sub-roles)
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param rolePath the path representing the given Role
     * @param transactionKey identifier of the active transaction
     * @return the list of Agents
     */
    public AgentPath[] getAgents(RolePath rolePath, TransactionKey transactionKey) throws ObjectNotFoundException;

    /**
     * Returns all of the Agents who hold this role (including sub-roles)
     *
     * @param rolePath the path representing the given Role
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @return the PagedResult of Agents
     */
    public default PagedResult getAgents(RolePath rolePath, int offset, int limit) throws ObjectNotFoundException {
        return getAgents(rolePath, offset, limit, null);
    }

    /**
     * Returns all of the Agents who hold this role (including sub-roles)
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param rolePath the path representing the given Role
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @param transactionKey identifier of the active transaction
     * @return the PagedResult of Agents
     */
    public PagedResult getAgents(RolePath rolePath, int offset, int limit, TransactionKey transactionKey) throws ObjectNotFoundException;

    /**
     * Get all roles held by the given Agent
     *
     * @param agentPath the path representing the given Agent
     * @return the list of Roles
     */
    public default RolePath[] getRoles(AgentPath agentPath) {
        return getRoles(agentPath, null);
    }

    /**
     * Get all roles held by the given Agent
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param agentPath the path representing the given Agent
     * @param transactionKey identifier of the active transaction
     * @return the list of Roles
     */
    public RolePath[] getRoles(AgentPath agentPath, TransactionKey transactionKey);

    /**
     * Get all roles held by the given Agent
     *
     * @param agentPath the path representing the given Agent
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @return the PagedResult of Roles
     */
    public default PagedResult getRoles(AgentPath agentPath, int offset, int limit) {
        return getRoles(agentPath, offset, limit, null);
    }

    /**
     * Get all roles held by the given Agent
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param agentPath the path representing the given Agent
     * @param offset the number of records to be skipped from the result
     * @param limit the max number of records to be returned
     * @param transactionKey identifier of the active transaction
     * @return the PagedResult of Roles
     */
    public PagedResult getRoles(AgentPath agentPath, int offset, int limit, TransactionKey transactionKey);

    /**
     * Checks if an agent qualifies as holding the stated Role, including any sub-role logic.
     *
     * @param agentPath the path representing the given Agent
     * @param role the path representing the given Role
     * @return true or false
     */
    public default boolean hasRole(AgentPath agentPath, RolePath role) {
        return hasRole(agentPath, role, null);
    }

    /**
     * Checks if an agent qualifies as holding the stated Role, including any sub-role logic.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     *
     * @param agentPath the path representing the given Agent
     * @param role the path representing the given Role
     * @param transactionKey identifier of the active transaction
     * @return true or false
     */
    public boolean hasRole(AgentPath agentPath, RolePath role, TransactionKey transactionKey);

    /**
     * Return the name of the Agent referenced by an AgentPath
     * 
     * @param agentPath the path representing the given Agent
     * @return the name string
     */
    public default String getAgentName(AgentPath agentPath) throws ObjectNotFoundException {
        return getAgentName(agentPath, null);
    }

    /**
     * Return the name of the Agent referenced by an AgentPath
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param agentPath the path representing the given Agent
     * @param transactionKey identifier of the active transaction
     * @return the name string
     */
    public String getAgentName(AgentPath agentPath, TransactionKey transactionKey) throws ObjectNotFoundException;
}
