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

import static org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler.ITEM_PROPERTY_TABLE;
import static org.cristalise.storage.jooqdb.lookup.JooqDomainPathHandler.DOMAIN_PATH_TABLE;
import static org.cristalise.storage.jooqdb.lookup.JooqDomainPathHandler.TARGET;
import static org.cristalise.storage.jooqdb.lookup.JooqItemHandler.ITEM_TABLE;
import static org.cristalise.storage.jooqdb.lookup.JooqRolePathHandler.AGENT;
import static org.cristalise.storage.jooqdb.lookup.JooqRolePathHandler.ROLE_PATH_TABLE;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.jooq.Field;
import org.jooq.JoinType;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

/**
 *
 */
public class JooqLookupManager implements LookupManager {

    protected DSLContext context;

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
        if (path == null) return false;

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

    private String getChildrenPattern(Path path) {
        //after the path match everything except '/'
        return "^" + path.getStringPath() + "/[^/]*$";
    }

    @Override
    public Iterator<Path> getChildren(Path path) {
        String pattern = getChildrenPattern(path);

        Logger.msg(8, "JooqLookupManager.getChildren() - pattern:" + pattern);

        if      (path instanceof ItemPath) return new ArrayList<Path>().iterator(); //empty iterator
        else if (path instanceof RolePath) return roles  .findByRegex(context, pattern ).iterator();
        else                               return domains.findByRegex(context, pattern ).iterator();
    }

    @Override
    public PagedResult getChildren(Path path, int offset, int limit) {
        String pattern = getChildrenPattern(path);

        Logger.msg(8, "JooqLookupManager.getChildren() - pattern:%s offset:%d limit:%d", pattern, offset, limit);

        if (path instanceof ItemPath) return new PagedResult();

        int maxRows = 0;

        if      (path instanceof RolePath)   maxRows = roles  .countByRegex(context, pattern);
        else if (path instanceof DomainPath) maxRows = domains.countByRegex(context, pattern);

        if (maxRows == 0) return new PagedResult();

        if (path instanceof RolePath) return new PagedResult(maxRows, roles  .findByRegex(context, pattern, offset, limit));
        else                          return new PagedResult(maxRows, domains.findByRegex(context, pattern, offset, limit));
    }

    private SelectQuery<?> getSearchSelect(Path start, List<Property> props) {
        SelectQuery<?> select = context.selectQuery();

        select.addFrom(DOMAIN_PATH_TABLE);

        for (Property p : props) {
            Field<UUID> joinField = field(name(p.getName(), "UUID"), UUID.class);

            select.addJoin(ITEM_PROPERTY_TABLE.as(p.getName()), JoinType.LEFT_OUTER_JOIN, TARGET.equal(joinField));

            select.addConditions(Operator.AND, field(name(p.getName(), "NAME"),  String.class).equal(p.getName()));
            select.addConditions(Operator.AND, field(name(p.getName(), "VALUE"), String.class).equal(p.getValue()));
        }

        select.addConditions(Operator.AND, JooqDomainPathHandler.PATH.like(domains.getFindPattern(start, "")));

        return select;
    }

    @Override
    public Iterator<Path> search(Path start, Property... props) {
        return search(start, Arrays.asList(props), 0, 0).rows.iterator();
    }

    @Override
    public PagedResult search(Path start, List<Property> props, int offset, int limit) {
        if (!exists(start)) return new PagedResult(0, new ArrayList<Path>());

        int maxRows = -1;

        // without limit no need to count the number of rows
        if (limit > 0) {
            SelectQuery<?> selectCount = getSearchSelect(start, props);
            selectCount.addSelect(DSL.count());

            Logger.msg(8, "JooqLookupManager.search(props) - SQL(count):\n%s", selectCount);

            maxRows = selectCount.fetchOne(0, int.class);

            if(maxRows == 0) return new PagedResult(0, new ArrayList<Path>());
        }

        SelectQuery<?> select = getSearchSelect(start, props);

        select.addSelect(JooqDomainPathHandler.PATH, TARGET);

        if (limit  > 0) select.addLimit(limit);
        if (offset > 0) select.addOffset(offset);

        Logger.msg(8, "JooqLookupManager.search(props) - SQL:\n%s", select);

        return new PagedResult(maxRows, domains.getListOfPath(select.fetch()));
    }

    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props) {
        return search(start, props, 0, 0).rows.iterator();
    }

    @Override
    public PagedResult search(Path start, PropertyDescriptionList props, int offset, int limit) {
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
        if (!exists(role))  throw new ObjectNotFoundException("Role:"+role);
        if (!exists(agent)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            int rows = roles.insert(context, role, agent);
            if (rows != 1) throw new ObjectCannotBeUpdated("Updated rows must be 1 but it was '"+rows+"'");
        }
        catch (Exception e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }
    }

    private SelectQuery<?> getGetAgentsSelect(RolePath role) {
        SelectQuery<?> select = context.selectQuery();

        select.addFrom(ROLE_PATH_TABLE.as("role"));

        select.addJoin(ITEM_TABLE.as("item"),          JoinType.JOIN, AGENT.equal(field(name("item", "UUID"), UUID.class)));
        select.addJoin(ITEM_PROPERTY_TABLE.as("prop"), JoinType.JOIN, AGENT.equal(field(name("prop", "UUID"), UUID.class)));

        select.addConditions(Operator.AND, JooqRolePathHandler.PATH.equal(role.getStringPath()));
        select.addConditions(Operator.AND, JooqItemPropertyHandler.NAME.equal("Name"));

        return select;
    }

    @Override
    public AgentPath[] getAgents(RolePath role) throws ObjectNotFoundException {
        return getAgents(role, -1, -1).rows.toArray(new AgentPath[0]);
    }

    @Override
    public PagedResult getAgents(RolePath role, int offset, int limit) throws ObjectNotFoundException {
        int maxRows = -1;

        if (limit > 0) {
            SelectQuery<?> selectCount = getGetAgentsSelect(role);
            selectCount.addSelect(DSL.count());

            Logger.msg(8, "JooqLookupManager.getAgents(props) - role:%s  SQL(count):\n%s", role, selectCount);

            maxRows = selectCount.fetchOne(0, int.class);
        }

        SelectQuery<?> select = getGetAgentsSelect(role);

        select.addSelect(
                field(name("item", "UUID"), UUID.class),
                JooqItemHandler.IOR,
                JooqItemHandler.IS_AGENT,
                JooqItemPropertyHandler.VALUE.as("Name"));

        select.addOrderBy(field(name("Name")));

        if (limit  > 0) select.addLimit(limit);
        if (offset > 0) select.addOffset(offset);

        Logger.msg(8, "JooqLookupManager.getAgents() - role:%s  SQL:\n%s", role, select);

        Result<?> result = select.fetch();

        PagedResult pResult = new PagedResult();

        if(result != null) {
            pResult.maxRows = maxRows;

            for (Record record: result) {
                try {
                    pResult.rows.add(JooqItemHandler.getItemPath(context, null, record));
                }
                catch (PersistencyException e) {
                    // This shall never happen
                    Logger.error(e);
                    throw new ObjectNotFoundException(e.getMessage());
                }
            }
        }

        return pResult;
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

    @Override
    public PagedResult searchAliases(ItemPath itemPath, int offset, int limit) {
        return new PagedResult(
                domains.countFind(context, itemPath),
                domains.find(context, itemPath, offset, limit) );
    }

    @Override
    public void setIOR(ItemPath item, String ior) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(item)) throw new ObjectNotFoundException("Item:"+item);

        item.setIORString(ior);

        try {
            items.updateIOR(context, item, ior);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new ObjectCannotBeUpdated("Item:" + item + " error:" + e.getMessage());
        }
    }

    @Override

    public PagedResult getRoles(AgentPath agentPath, int offset, int limit) throws ObjectNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }
}
