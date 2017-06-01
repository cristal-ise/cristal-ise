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
package org.cristalise.storage.jooqdb.lookup;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.auth.Argon2Password;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.jooq.DSLContext;

/**
 *
 */
public class JooqLookupManager implements LookupManager {

    private DSLContext context;

    private JooqItemHandler         items;
    private JooqDomainPathHandler   domains;
    private JooqRolePathHandler     roles;
    private JooqItemPropertyHandler properties;

    private Argon2Password passwordHasher;

    @Override
    public void open(Authenticator auth) {
        try {
            context = JooqHandler.connect();

            items      = new JooqItemHandler();
            domains    = new JooqDomainPathHandler();
            roles      = new JooqRolePathHandler();
            properties = new JooqItemPropertyHandler();

            items     .createTables(context);
            domains   .createTables(context);
            roles     .createTables(context);
            properties.createTables(context);

            passwordHasher = new Argon2Password();
        }
        catch (PersistencyException ex) {
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
        context.close();
    }

    @Override
    public boolean exists(Path path) {
        try {
            if      (path instanceof ItemPath)   return items  .exists(context, path.getUUID());
            else if (path instanceof AgentPath)  return items  .exists(context, path.getUUID());
            else if (path instanceof DomainPath) return domains.exists(context, (DomainPath)path);
            else if (path instanceof RolePath)   return roles  .exists(context, (RolePath)path, null);
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
            else if (newPath instanceof RolePath)   rows = (createRole((RolePath) newPath) != null) ? 1 : 0;

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

        Logger.msg(8, "JooqLookupManager.delete() - path:"+path);

        try {
            if (getChildren(path).hasNext()) throw new ObjectCannotBeUpdated("Path is not a leaf");

            int rows = 0;
            if      (path instanceof ItemPath)   rows = items  .delete(context, path.getUUID());
            else if (path instanceof AgentPath)  rows = items  .delete(context, path.getUUID());
            else if (path instanceof DomainPath) rows = domains.delete(context, path.getStringPath());
            else if (path instanceof RolePath)   rows = roles  .delete(context, (RolePath)path, null);

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
        ItemPath ip = new ItemPath(sysKey);

        if (!exists(ip)) throw new ObjectNotFoundException("Path does not exist:"+sysKey);

        try {
            return items.fetch(context, ip.getUUID(), properties);
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

    private List<Path> find(Path start, String name, List<UUID> uuids) {
        Logger.msg(8, "JooqLookupManager.find() - start:"+start+" name:"+name);

        if      (start instanceof DomainPath) return domains.find(context, (DomainPath)start, name, uuids);
        else if (start instanceof RolePath)   return roles  .find(context, (RolePath)start,   name, uuids);

        return new ArrayList<Path>();
    }

    @Override
    public Iterator<Path> search(Path start, String name) {
        List<Path> result = find(start, name, null);

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
        List<UUID> uuids = new ArrayList<>();
        uuids.add(JooqRolePathHandler.NO_AGENT);

        List<Path> result = roles.find(context, "%/"+roleName, uuids);

        if      (result == null || result.size() == 0) throw new ObjectNotFoundException("Role '"+roleName+"' does not exist");
        else if (result.size() > 1)                    throw new ObjectNotFoundException("Unbiguos roleName:'"+roleName+"'");

        return (RolePath)result.get(0);
    }

    @Override
    public ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException {
        if (!exists(domainPath)) throw new ObjectNotFoundException("Path does not exist:"+domainPath);

        try {
            DomainPath dp = domains.fetch(context, domainPath);

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
        //after the path match words only
        //String pattern = "^" + path.getStringPath() + "/\\w+$";

        //after the path match everything except '/'
        String pattern = "^" + path.getStringPath() + "/[^/]*$";

        Logger.msg(8, "JooqLookupManager.getChildren() - pattern:"+pattern);

        if      (path instanceof ItemPath) return new ArrayList<Path>().iterator(); //empty iterator
        else if (path instanceof RolePath) return roles  .findByRegex(context, pattern ).iterator();
        else                               return domains.findByRegex(context, pattern ).iterator();
    }

    @Override
    public Iterator<Path> search(Path start, Property... props) {
        if (!exists(start)) return new ArrayList<Path>().iterator(); //empty iterator
        
        List<UUID> uuids = properties.findItems(context, props);
        
        if(uuids == null || uuids.size() == 0) return new ArrayList<Path>().iterator(); //empty iterator

        return find(start, "", uuids).iterator();
    }

    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props) {
        //FIXME: UNIMPLEMENTED search(PropertyDescriptionList)
        throw new RuntimeException("InMemoryLookup.search(PropertyDescriptionList) - UNIMPLEMENTED start:"+start);
    }

    @Override
    public RolePath createRole(RolePath role) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        Logger.msg(5, "JooqLookupManager.createRole() - role:"+role);

        if(exists(role)) throw new ObjectAlreadyExistsException("Role:"+role);

        try {
            role.getParent();
            roles.insert(context, role, null);
            return role;
        } 
        catch (Throwable t) {
            Logger.error(t); 
            throw new ObjectCannotBeUpdated("Parent role for '"+role+"' does not exists");
        }
    }

    @Override
    public void addRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        if (!exists(role)) throw new ObjectNotFoundException("Role:"+role);

        try {
            int rows = roles.insert(context, role, agent);
            if (rows != 1) throw new ObjectCannotBeUpdated("Updated rows must be 1 but it was '"+rows+"'");
        }
        catch (Exception e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }
    }

    @Override
    public AgentPath[] getAgents(RolePath role) throws ObjectNotFoundException {
        try {
            List<UUID> uuids = roles.findAgents(context, role);
            return items.fetchAll(context, uuids, properties).toArray(new AgentPath[0]);
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
        return new AgentPath[0];
    }

    @Override
    public RolePath[] getRoles(AgentPath agent) {
        try {
            return roles.findRolesOfAgent(context, agent).toArray(new RolePath[0]);
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
        return new RolePath[0];
    }

    @Override
    public boolean hasRole(AgentPath agent, RolePath role) {
        try {
            return roles.exists(context, role, agent);
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
        return false;
    }

    @Override
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        if (!exists(role))  throw new ObjectNotFoundException("Role:"+role);
        if (!exists(agent)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            int rows = roles.delete(context, role, agent);
            
            if (rows == 0) 
                throw new ObjectCannotBeUpdated("Role:"+role+" Agent:"+agent + " are not related.");
        }
        catch (Exception e) {
            throw new ObjectCannotBeUpdated("Role:"+role+" Agent:"+agent + " error:" + e.getMessage());
        }
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        if (!exists(agent)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            int rows = items.updatePassword(context, agent, passwordHasher.hashPassword(newPassword.toCharArray()));
            if (rows != 1) throw new ObjectCannotBeUpdated("Agent:"+agent);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated("Agent:"+agent + " error:" + e.getMessage());
        }
    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(role)) throw new ObjectNotFoundException("Role:"+role);

        role.setHasJobList(hasJobList);

        try {
            roles.update(context, role);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated("Role:"+role + " error:" + e.getMessage());
        }
    }

    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath) {
        return domains.find(context, itemPath).iterator();
    }
}
