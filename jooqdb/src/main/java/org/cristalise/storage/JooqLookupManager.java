/**
 * This file is part of the CRISTAL-iSE jOOQ Cluster Storage Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.storage;

import static org.jooq.impl.DSL.using;

import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.cristalise.storage.jooqdb.lookup.JooqDomainPathHandler;
import org.cristalise.storage.jooqdb.lookup.JooqItemHandler;
import org.cristalise.storage.jooqdb.lookup.JooqRolePathHandler;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;

/**
 *
 */
public class JooqLookupManager implements LookupManager {

    private DSLContext context;
    
    private JooqItemHandler         items;
    private JooqDomainPathHandler   domains;
    private JooqRolePathHandler     roles;
    private JooqItemPropertyHandler properties;

    @Override
    public void open(Authenticator auth) {
        String uri  = Gateway.getProperties().getString(JooqHandler.JOOQ_URI);
        String user = Gateway.getProperties().getString(JooqHandler.JOOQ_USER); 
        String pwd  = Gateway.getProperties().getString(JooqHandler.JOOQ_PASSWORD);

        if (StringUtils.isAnyBlank(uri, user, pwd)) {
            throw new IllegalArgumentException("JOOQ (uri, user, password) config values must not be blank");
        }

        SQLDialect dialect = SQLDialect.valueOf(Gateway.getProperties().getString(JooqHandler.JOOQ_DIALECT, "POSTGRES"));

        Logger.msg(1, "JOOQLookupManager.open() - uri:'"+uri+"' user:'"+user+"' dialect:'"+dialect+"'");

        try {
            context = using(DriverManager.getConnection(uri, user, pwd), dialect);

            items      = new JooqItemHandler();
            domains    = new JooqDomainPathHandler();
            roles      = new JooqRolePathHandler();
            properties = new JooqItemPropertyHandler();

            items     .createTables(context);
            domains   .createTables(context);
            roles     .createTables(context);
            properties.createTables(context);
        }
        catch (SQLException | DataAccessException | PersistencyException ex) {
            Logger.error("JooqLookupManager could not connect to URI '" + uri + "' with user '" + user + "'");
            Logger.error(ex);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    @Override
    public void initializeDirectory() throws ObjectNotFoundException {
        Logger.msg(6, "JOOQLookupManager.initializeDirectory() - NOTHING done.");
    }

    @Override
    public void close() {
        try {
            context.close();
        }
        catch (DataAccessException e) {
            Logger.error(e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public boolean exists(Path path) {
        try {
            if      (path instanceof ItemPath)   return items  .exists(context, path.getUUID());
            else if (path instanceof AgentPath)  return items  .exists(context, path.getUUID());
            else if (path instanceof DomainPath) return domains.exists(context, path.getStringPath());
            else if (path instanceof RolePath)   return roles  .exists(context, path.getStringPath());
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
        return false;
    }

    @Override
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        if (exists(newPath)) throw new ObjectAlreadyExistsException("Path exist:"+newPath);

        Logger.msg(8, "JooqLookupManager.add() - path:"+newPath);

        try {
            int rows = 0;

            if      (newPath instanceof AgentPath)  rows = items  .insert(context, (AgentPath) newPath, properties);
            else if (newPath instanceof ItemPath)   rows = items  .insert(context, (ItemPath)  newPath);
            else if (newPath instanceof DomainPath) rows = domains.insert(context, (DomainPath)newPath);
            else if (newPath instanceof RolePath)   rows = roles  .insert(context, (RolePath)  newPath);

            if (rows == 0)
                throw new ObjectCannotBeUpdated("JOOQLookupManager must insert some records:"+rows);
            else
                Logger.msg(8, "JooqLookupManager.add() - path:"+newPath+" rows inserted:"+rows);
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }
    }

    @Override
    public void delete(Path path) throws ObjectCannotBeUpdated {
        if (!exists(path)) throw new ObjectCannotBeUpdated("Path does not exist:"+path);

        try {
            int rows = 0;
            if      (path instanceof ItemPath)   rows = items.delete(context, path.getUUID());
            else if (path instanceof AgentPath)  rows = items.delete(context, path.getUUID());
            else if (path instanceof DomainPath) rows = domains.delete(context, path.getStringPath());
            else if (path instanceof RolePath)   rows = roles.delete(context, path.getStringPath());

            if (rows == 0)
                throw new ObjectCannotBeUpdated("JOOQLookupManager must delete some records:"+rows);
            else
                Logger.msg(8, "JooqLookupManager.delete() - path:"+path+" rows deleted:"+rows);
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }

    }

    @Override
    public ItemPath getItemPath(String sysKey) throws InvalidItemPathException, ObjectNotFoundException {
        if (!exists(new ItemPath(sysKey))) throw new ObjectNotFoundException("Path does not exist:"+sysKey);

        try {
            return items.fetch(context, UUID.fromString(sysKey), properties);
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new InvalidItemPathException(e.getMessage());
        }
    }

    @Override
    public String getIOR(Path path) throws ObjectNotFoundException {
        try {
            return getItemPath(path.getStringPath()).getIORString();
        }
        catch (InvalidItemPathException e) {
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public Iterator<Path> search(Path start, String name) {
        
        List<Path> result = null;

        if      (start instanceof DomainPath) result = domains.find(context, start.getStringPath(), name);
        else if (start instanceof RolePath)   result = roles  .find(context, start.getStringPath(), name);

        if (result == null) return new ArrayList<Path>().iterator(); //returns empty iterator
        else                return result.iterator();
    }

    @Override
    public AgentPath getAgentPath(String agentName) throws ObjectNotFoundException {
        List<UUID> uuids = properties.findItemsByName(context, agentName);

        if (uuids.size() == 0) throw new ObjectNotFoundException("Could not find agent:"+agentName);

        try {
            return (AgentPath) items.fetch(context, uuids.get(0), properties);
        }
        catch (PersistencyException e) {
            throw new ObjectNotFoundException("Could not retrieve agentName:"+agentName + " error:"+e.getMessage());
        }
    }

    @Override
    public RolePath getRolePath(String roleName) throws ObjectNotFoundException {
        List<Path> result = roles.find(context, "%/"+roleName);

        if      (result == null || result.size() == 0) throw new ObjectNotFoundException("Role '"+roleName+"' does not exist");
        else if (result.size() > 1)                    throw new ObjectNotFoundException("Unbiguos roleName:'"+roleName+"'");

        return (RolePath)result.get(0);
    }

    @Override
    public ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException {
        if (!exists(domainPath)) throw new ObjectNotFoundException("Path does not exist:"+domainPath);

        try {
            DomainPath dp = domains.fetch(context, domainPath.getStringPath());

            if (dp.getTarget() == null) throw new InvalidItemPathException("DomainPath has no target:"+domainPath);

            return dp.getTarget();
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getAgentName(AgentPath agentPath) throws ObjectNotFoundException {
        if (!exists(agentPath)) throw new ObjectNotFoundException("Path does not exist:"+agentPath);

        try {
            AgentPath ap = (AgentPath)items.fetch(context, agentPath.getUUID(), properties);
            return ap.getAgentName();
        }
        catch (PersistencyException e) {
            Logger.error(e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public Iterator<Path> getChildren(Path path) {
        String pattern = "^" + path.getStringPath() + "\\/\\w+$";

        if      (path instanceof ItemPath) return new ArrayList<Path>().iterator(); //empty iterator
        else if (path instanceof RolePath) return roles  .findByRegex(context, pattern ).iterator();
        else                               return domains.findByRegex(context, pattern ).iterator();
    }

    @Override
    public Iterator<Path> search(Path start, Property... props) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getChildren(org.cristalise.kernel.lookup.Path)
     */
    @Override
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#search(org.cristalise.kernel.lookup.Path, org.cristalise.kernel.property.PropertyDescriptionList)
     */
    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#searchAliases(org.cristalise.kernel.lookup.ItemPath)
     */
    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getAgents(org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public AgentPath[] getAgents(RolePath rolePath) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#getRoles(org.cristalise.kernel.lookup.AgentPath)
     */
    @Override
    public RolePath[] getRoles(AgentPath agentPath) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.Lookup#hasRole(org.cristalise.kernel.lookup.AgentPath, org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public boolean hasRole(AgentPath agentPath, RolePath role) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#addRole(org.cristalise.kernel.lookup.AgentPath, org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public void addRole(AgentPath agent, RolePath rolePath) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#removeRole(org.cristalise.kernel.lookup.AgentPath, org.cristalise.kernel.lookup.RolePath)
     */
    @Override
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#setAgentPassword(org.cristalise.kernel.lookup.AgentPath, java.lang.String)
     */
    @Override
    public void setAgentPassword(AgentPath agent, String newPassword)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.cristalise.kernel.lookup.LookupManager#setHasJobList(org.cristalise.kernel.lookup.RolePath, boolean)
     */
    @Override
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        // TODO Auto-generated method stub

    }

}
