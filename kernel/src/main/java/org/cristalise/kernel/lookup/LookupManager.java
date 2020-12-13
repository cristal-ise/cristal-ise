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

import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;


/**
 * The LookupManager interface contains all of the directory modifying methods
 * of the Lookup. This allows read-only Lookup implementations. Server processes
 * will attempt to cast their Lookups into LookupManagers, and fail to start up
 * if this is not possible.
 *
 */
public interface LookupManager extends Lookup {

    /**
     * Called when a server starts up. The Lookup implementation should ensure 
     * that the initial structure of its directory is valid, and create it on 
     * first boot.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param transactionKey identifier of the active transaction
     * @throws ObjectNotFoundException When initialization data is not found
     */
    public void initializeDirectory(Object transactionKey) throws ObjectNotFoundException;

    /**
     * Register a new a Path in the directory.
     * 
     * @param newPath The path to add
     * @throws ObjectCannotBeUpdated When there is an error writing to the directory
     * @throws ObjectAlreadyExistsException When the Path has already been registered
     */
    public default void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        add(newPath, null);
    }

    /**
     * Register a new a Path in the directory.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param newPath The path to add
     * @param transactionKey identifier of the active transaction
     * @throws ObjectCannotBeUpdated When there is an error writing to the directory
     * @throws ObjectAlreadyExistsException When the Path has already been registered
     */
    public void add(Path newPath, Object transactionKey) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException;

    /**
     * Remove a Path from the directory.
     * 
     * @param path The path to remove
     * @throws ObjectCannotBeUpdated When an error occurs writing to the directory
     */
    public default void delete(Path path) throws ObjectCannotBeUpdated {
        delete(path, null);
    }

    /**
     * Remove a Path from the directory.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param path The path to remove
     * @param transactionKey identifier of the active transaction
     * @throws ObjectCannotBeUpdated When an error occurs writing to the directory
     */
    public void delete(Path path, Object transactionKey) throws ObjectCannotBeUpdated;

    /**
     * Creates a new Role. Checks if parent role exists or not and throws ObjectCannotBeUpdated if parent does not exist
     * Called by the server predefined step 'CreateNewRole'
     * 
     * @param role The new role path
     * @return the RolePath representing the newly create Role
     */
    public default RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        return createRole(role, null);
    }

    /**
     * Creates a new Role. Checks if parent role exists or not and throws ObjectCannotBeUpdated if parent does not exist
     * Called by the server predefined step 'CreateNewRole'
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param role The new role path
     * @param transactionKey identifier of the active transaction
     * @return the RolePath representing the newly create Role
     */
    public RolePath createRole(RolePath role, Object transactionKey) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated;

    /**
     * Adds the given Agent to the given Role, if they both exist.
     * 
     * @param agent  the path representing the given Agent
     * @param rolePath the path representing the given Role
     */
    public default void addRole(AgentPath agent, RolePath rolePath) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        addRole(agent, rolePath, null);
    }

    /**
     * Adds the given Agent to the given Role, if they both exist.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param agent  the path representing the given Agent
     * @param rolePath the path representing the given Role
     * @param transactionKey identifier of the active transaction
     */
    public void addRole(AgentPath agent, RolePath rolePath, Object transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException;

    /**
     * Remove the given Agent from the given Role. Does not delete the Role.
     * 
     * @param agent the path representing the given Agent
     * @param role the path representing the given Role
     */
    public default void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        removeRole(agent, role, null);
    }

    /**
     * Remove the given Agent from the given Role. Does not delete the Role.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param agent the path representing the given Agent
     * @param role the path representing the given Role
     * @param transactionKey identifier of the active transaction
     */
    public void removeRole(AgentPath agent, RolePath role, Object transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException;

    /**
     * Set permanent password of Agent's
     * 
     * @param agent The Agent
     * @param newPassword The Agent's new password
     */
    public default void setAgentPassword(AgentPath agent, String newPassword) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        setAgentPassword(agent, newPassword, false, null);
    }

    /**
     * Set an Agent's password
     * 
     * @param agent The Agent
     * @param newPassword The Agent's new password
     * @param temporary whether the new password is temporary or not
     */
    public default void setAgentPassword(AgentPath agent, String newPassword, boolean temporary) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        setAgentPassword(agent, newPassword, temporary, null);
    }

    /**
     * Set an Agent's password
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param agent The Agent
     * @param newPassword The Agent's new password
     * @param temporary whether the new password is temporary or not
     * @param transactionKey identifier of the active transaction
     */
    public void setAgentPassword(AgentPath agent, String newPassword, boolean temporary, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException;

    /**
     * Set the flag specifying whether Activities holding this Role should push Jobs its Agents.
     * 
     * @param role The role to modify
     * @param hasJobList boolean flag
     */
    public default void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        setHasJobList(role, hasJobList, null);
    }

    /**
     * Set the flag specifying whether Activities holding this Role should push Jobs its Agents.
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param role The role to modify
     * @param hasJobList boolean flag
     * @param transactionKey identifier of the active transaction
     */
    public void setHasJobList(RolePath role, boolean hasJobList, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated;

    /**
     * Set the IOR of the Item
     * 
     * @param item the Item to be updated
     * @param ior the new ior
     * @throws ObjectNotFoundException Item does not exists
     * @throws ObjectCannotBeUpdated there was a probelm updating the ior
     */
    public default void setIOR(ItemPath item, String ior) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        setIOR(item, ior, null);
    }

    /**
     * Set the IOR of the Item
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param item the Item to be updated
     * @param ior the new ior
     * @param transactionKey identifier of the active transaction
     * @throws ObjectNotFoundException Item does not exists
     * @throws ObjectCannotBeUpdated there was a probelm updating the ior
     */
    public void setIOR(ItemPath item, String ior, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated;

    /**
     * Sets the permission of the given Role. Use blank string to clear the permissions
     * 
     * @param role the RolePath to change
     * @param permission String using WildcardPermission format of shiro
     * @param transactionKey identifier of the active transaction
     * @throws ObjectCannotBeUpdated there was a problem updating the permissions
     */
    public default void setPermission(RolePath role, String permission) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        setPermission(role, permission, null);
    }

    /**
     * Sets the permission of the given Role. Use blank string to clear the permissions
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param role the RolePath to change
     * @param permission String using WildcardPermission format of shiro
     * @param transactionKey identifier of the active transaction
     * @throws ObjectNotFoundException Role does not exists
     * @throws ObjectCannotBeUpdated there was a problem updating the permissions
     */
    public void setPermission(RolePath role, String permission, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated;

    /**
     * Sets the list of permission of the given Role. Use empty list to clear the permissions
     * 
     * @param role the RolePath to change
     * @param permissions list of String using WildcardPermission format of shiro
     * @throws ObjectNotFoundException Role does not exists
     * @throws ObjectCannotBeUpdated there was a problem updating the permissions
     */
    public default void setPermissions(RolePath role, List<String> permissions) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        setPermissions(role, permissions, null);
    }

    /**
     * Sets the list of permission of the given Role. Use empty list to clear the permissions
     * This method can be used in server side code or Script to find uncommitted changes during the active transaction.
     * 
     * @param role the RolePath to change
     * @param permissions list of String using WildcardPermission format of shiro
     * @param transactionKey identifier of the active transaction
     * @throws ObjectNotFoundException Role does not exists
     * @throws ObjectCannotBeUpdated there was a problem updating the permissions
     */
    public void setPermissions(RolePath role, List<String> permissions, Object transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated;

    /**
     * 
     */
    public void postStartServer();

    /**
     * 
     */
    public void postBoostrap();
}
