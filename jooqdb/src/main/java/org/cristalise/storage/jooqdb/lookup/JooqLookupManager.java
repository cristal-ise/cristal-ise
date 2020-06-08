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
import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.upper;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class JooqLookupManager implements LookupManager {

    private JooqItemHandler         items;
    private JooqDomainPathHandler   domains;
    private JooqRolePathHandler     roles;
    private JooqPermissionHandler   permissions;
    private JooqItemPropertyHandler properties;

    private Argon2Password passwordHasher;

    @Override
    public void open(Authenticator auth) {
        try {
            items       = new JooqItemHandler();
            domains     = new JooqDomainPathHandler();
            roles       = new JooqRolePathHandler();
            permissions = new JooqPermissionHandler();
            properties  = new JooqItemPropertyHandler();

            if (! JooqHandler.readOnlyDataSource) {
                JooqHandler.connect().transaction(nested -> {
                    items.createTables(DSL.using(nested));
                    domains.createTables(DSL.using(nested));
                    roles.createTables(DSL.using(nested));
                    permissions.createTables(DSL.using(nested));
                    properties.createTables(DSL.using(nested));
                });
            }

            passwordHasher = new Argon2Password();
        }
        catch (PersistencyException ex) {
            log.error("", ex);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    public void dropHandlers() throws PersistencyException {

        JooqHandler.connect().transaction(nested -> {
            properties .dropTables(DSL.using(nested));
            permissions.dropTables(DSL.using(nested));
            roles      .dropTables(DSL.using(nested));
            domains    .dropTables(DSL.using(nested));
            items      .dropTables(DSL.using(nested));
        });
    }

    @Override
    public void initializeDirectory() throws ObjectNotFoundException {
        log.debug("initializeDirectory() - NOTHING done.");
    }

    @Override
    public void close() {
        try {
            JooqHandler.closeDataSource();
        }
        catch (DataAccessException | PersistencyException e) {
            log.error("", e);
        }
    }

    @Override
    public boolean exists(Path path) {
        if (path == null) return false;

        List<Boolean> itemExists = new ArrayList<>();

        try {
           JooqHandler.connect().transaction(nested -> {
                boolean isExist = false;

                if      (path instanceof ItemPath)   isExist = items.exists(DSL.using(nested), path.getUUID());
                else if (path instanceof AgentPath)  isExist = items.exists(DSL.using(nested), path.getUUID());
                else if (path instanceof DomainPath) isExist = domains.exists(DSL.using(nested), (DomainPath)path);
                else if (path instanceof RolePath)   isExist = roles.exists(DSL.using(nested), (RolePath)path,null);

                if (isExist) itemExists.add(isExist);

                JooqHandler.logConnectionCount("JooqLookupManager.exists()", DSL.using(nested));
            });
        }
        catch (PersistencyException e) {
            log.error("", e);
        }



        return itemExists.size() > 0 ? true : false;
    }

    @Override
    public void add(Path newPath) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        if (exists(newPath)) throw new ObjectAlreadyExistsException("Path exist:"+newPath);

        log.debug("add() - path:"+newPath);

        try {
            DSLContext context = JooqHandler.connect();

            context.transaction(nested -> {
                int rows = 0;
                if (newPath instanceof AgentPath) {
                    rows = items.insert(DSL.using(nested), (AgentPath) newPath, properties);
                } else if  (newPath instanceof ItemPath) {
                    rows = items.insert(DSL.using(nested), (ItemPath) newPath);
                } else if (newPath instanceof DomainPath) {
                    rows = domains.insert(DSL.using(nested), (DomainPath)newPath);
                } else if (newPath instanceof RolePath) {
                    rows = (createRole((RolePath)newPath) != null) ? 1 : 0;
                }

                if (rows == 0) throw new ObjectCannotBeUpdated("JOOQLookupManager must insert some records:"+rows);
                else           log.debug("add() - path:"+newPath+" rows inserted:"+rows);
            });

            JooqHandler.logConnectionCount("JooqLookupManager.add()", context);
        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }
    }

    @Override
    public void delete(Path path) throws ObjectCannotBeUpdated {
        if (!exists(path)) throw new ObjectCannotBeUpdated("Path does not exist:"+path);

        log.debug("delete() - path:"+path);

        try {
            if (getChildren(path).hasNext()) throw new ObjectCannotBeUpdated("Path is not a leaf");

            DSLContext context = JooqHandler.connect();

            context.transaction(nested -> {
                int rows = 0;
                if  (path instanceof ItemPath) {
                    rows = items  .delete(DSL.using(nested), path.getUUID());
                } else if  (path instanceof AgentPath) {
                    rows = items  .delete(DSL.using(nested), path.getUUID());
                } else if (path instanceof DomainPath) {
                    rows = domains  .delete(DSL.using(nested), path.getStringPath());
                } else if (path instanceof RolePath) {
                    permissions.delete(DSL.using(nested), path.getStringPath());
                    rows = roles.delete(DSL.using(nested), (RolePath)path, null);
                }

                if (rows == 0) throw new ObjectCannotBeUpdated("JOOQLookupManager must delete some records:"+rows);
                else           log.debug("delete() - path:"+path+" rows deleted:"+rows);
            });

        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }

    }

    @Override
    public ItemPath getItemPath(String sysKey) throws InvalidItemPathException, ObjectNotFoundException {
        ItemPath ip = new ItemPath(sysKey);

        if (!exists(ip)) throw new ObjectNotFoundException("Path does not exist:"+sysKey);

        try {
            DSLContext context = JooqHandler.connect();
            return items.fetch(context, ip.getUUID(), properties);
        }
        catch (PersistencyException e) {
            log.error("", e);
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

    private List<Path> find(DSLContext context , Path start, String name, List<UUID> uuids) throws PersistencyException {
        log.debug("find() - start:"+start+" name:"+name);
        List<Path> paths = new ArrayList<>();

        context.transaction(nested -> {
            if      (start instanceof DomainPath) paths.addAll(domains.find(DSL.using(nested), (DomainPath)start, name, uuids));
            else if (start instanceof RolePath)   paths.addAll(roles.find(  DSL.using(nested), (RolePath)start,   name, uuids));
        });

        return paths;
    }

    @Override
    public Iterator<Path> search(Path start, String name) {
        List<Path> result = new ArrayList<>();
        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested -> {
                result.addAll(find(DSL.using(nested), start, name, null));
            });

        }
        catch (PersistencyException e) {
            log.error("", e);
        }

        if (result.isEmpty()) return new ArrayList<Path>().iterator(); //returns empty iterator
        else                  return result.iterator();
    }

    @Override
    public AgentPath getAgentPath(String agentName) throws ObjectNotFoundException {
        try {
            DSLContext context = JooqHandler.connect();

            List<UUID> uuids = properties.findItemsByName(context, agentName);

            if (uuids.size() == 0) throw new ObjectNotFoundException("Could not find agent:"+agentName);

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

        DSLContext context;
        try {
            context = JooqHandler.connect();
            List<Path> result = roles.find(context, "%/"+roleName, uuids);

            if      (result == null || result.size() == 0) throw new ObjectNotFoundException("Role '"+roleName+"' does not exist");
            else if (result.size() > 1)                    throw new ObjectNotFoundException("Unbiguos roleName:'"+roleName+"'");

            RolePath role = (RolePath)result.get(0);
            role.setPermissions(permissions.fetch(context, role.getStringPath()));

            return role;
        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public ItemPath resolvePath(DomainPath domainPath) throws InvalidItemPathException, ObjectNotFoundException {
        if (!exists(domainPath)) throw new ObjectNotFoundException("Path does not exist:"+domainPath);

        try {
            DSLContext context = JooqHandler.connect();
            DomainPath dp = domains.fetch(context, domainPath);

            if (dp.getTarget() == null) throw new InvalidItemPathException("DomainPath has no target:"+domainPath);

            //issue #165: using items.fetch() ensures that Path is either ItemPath or AgentPath
            return items.fetch(context, dp.getTarget().getUUID(), properties);
        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getAgentName(AgentPath agentPath) throws ObjectNotFoundException {
        if (!exists(agentPath)) throw new ObjectNotFoundException("Path does not exist:"+agentPath);

        try {
            DSLContext context = JooqHandler.connect();
            ItemPath ip = items.fetch(context, agentPath.getUUID(), properties);

            if (ip instanceof AgentPath) return ((AgentPath)ip).getAgentName();
            else                         throw new ObjectNotFoundException("Path is not an agent:"+agentPath);
        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    private String getChildrenPattern(Path path, DSLContext context) {
        //after the path match everything except '/'
        if (context.dialect().equals(POSTGRES)) {
            return convertToPostgresPattern(path.getStringPath());
        }
        return "^" + path.getStringPath() + "/[^/]*$";
    }

    public String convertToPostgresPattern(String path) {
        String specialCharsToReplace = Gateway.getProperties().getString("JooqLookupManager.getChildrenPattern.specialCharsToReplace", "[^a-zA-Z0-9 ]");
        return "(?e)^" + path.replaceAll("(" + specialCharsToReplace + ")", "\\\\$1") + "/[^/]*$";
    }

    @Override
    public Iterator<Path> getChildren(Path path) {
        try {
            DSLContext context = JooqHandler.connect();

            String pattern = getChildrenPattern(path, context);
            log.debug("getChildren() - pattern:" + pattern);

            if      (path instanceof ItemPath) return new ArrayList<Path>().iterator(); //empty iterator
            else if (path instanceof RolePath) return roles  .findByRegex(context, pattern ).iterator();
            else                               return domains.findByRegex(context, pattern ).iterator();
        }
        catch (Exception e) {
            log.error("", e);
        }
        return new ArrayList<Path>().iterator();
    }

    @Override
    public PagedResult getChildren(Path path, int offset, int limit) {
        if (path instanceof ItemPath) return new PagedResult();

        int maxRows = 0;

        DSLContext context;
        try {
            context = JooqHandler.connect();
        }
        catch (PersistencyException e) {
            log.error("", e);
            return new PagedResult();
        }

        String pattern = getChildrenPattern(path, context);
        log.debug("getChildren() - pattern:{} offset:{} limit:{}", pattern, offset, limit);

        if      (path instanceof RolePath)   maxRows = roles  .countByRegex(context, pattern);
        else if (path instanceof DomainPath) maxRows = domains.countByRegex(context, pattern);

        if (maxRows == 0) return new PagedResult();

        List<Path> pathes = null;

        if      (path instanceof RolePath)   pathes = roles  .findByRegex(context, pattern, offset, limit);
        else if (path instanceof DomainPath) pathes = domains.findByRegex(context, pattern, offset, limit);

        JooqHandler.logConnectionCount("JooqLookupManager.getChildren()", context);

        if (pathes == null) return new PagedResult();
        else                return new PagedResult(maxRows, pathes);
    }

    private SelectQuery<?> getSearchSelect(DSLContext context, Path start, List<Property> props) {
        SelectQuery<?> select = context.selectQuery();

        select.addFrom(DOMAIN_PATH_TABLE);

        for (Property p : props) {
            Field<UUID> joinField = field(name(p.getName(), "UUID"), UUID.class);

            select.addJoin(ITEM_PROPERTY_TABLE.as(p.getName()), JoinType.LEFT_OUTER_JOIN, TARGET.equal(joinField));

            select.addConditions(Operator.AND, field(name(p.getName(), "NAME"),  String.class).equal(p.getName()));
            select.addConditions(Operator.AND, upper(field(name(p.getName(), "VALUE"), String.class)).like(upper(p.getValue())));
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

        DSLContext context;
        try {
            context = JooqHandler.connect();
        }
        catch (PersistencyException e) {
            log.error("", e);
            return new PagedResult();
        }

        int maxRows = -1;

        // without limit no need to count the number of rows
        if (limit > 0) {
            SelectQuery<?> selectCount = getSearchSelect(context, start, props);
            selectCount.addSelect(DSL.count());

            log.debug("search(props) - SQL(count):\n{}", selectCount);

            maxRows = selectCount.fetchOne(0, int.class);

            if (maxRows == 0) return new PagedResult(0, new ArrayList<Path>());
        }

        SelectQuery<?> select = getSearchSelect(context, start, props);

        select.addSelect(JooqDomainPathHandler.PATH, TARGET);
        select.addOrderBy(JooqDomainPathHandler.PATH);

        if (limit  > 0) select.addLimit(limit);
        if (offset > 0) select.addOffset(offset);

        log.debug("search(props) - SQL:\n{}", select);

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
        log.debug("createRole() - role:"+role);

        if (exists(role)) throw new ObjectAlreadyExistsException("Role:"+role);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested -> {
                role.getParent();
                roles.insert(DSL.using(nested), role, null);
                permissions.insert(DSL.using(nested), role.getStringPath(), role.getPermissionsList());
            });

            return role;
        }
        catch (Exception e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated("Parent role for '"+role+"' does not exists");
        }
    }

    @Override
    public void addRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        if (!exists(role))  throw new ObjectNotFoundException("Role:"+role);
        if (!exists(agent)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested -> {
                int rows = roles.insert(DSL.using(nested), role, agent);
                if (rows != 1) throw new ObjectCannotBeUpdated("Updated rows must be 1 but it was '"+rows+"'");
            });

        }
        catch (Exception e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }
    }

    private SelectQuery<?> getGetAgentsSelect(DSLContext context, RolePath role) {
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

        DSLContext context;
        try {
            context = JooqHandler.connect();
        }
        catch (PersistencyException e) {
            log.error("", e);
            return new PagedResult();
        }

        if (limit > 0) {
            SelectQuery<?> selectCount = getGetAgentsSelect(context, role);
            selectCount.addSelect(DSL.count());

            log.debug("getAgents(props) - role:{}  SQL(count):\n{}", role, selectCount);

            maxRows = selectCount.fetchOne(0, int.class);
        }

        SelectQuery<?> select = getGetAgentsSelect(context, role);

        select.addSelect(
                field(name("item", "UUID"), UUID.class),
                JooqItemHandler.IOR,
                JooqItemHandler.IS_AGENT,
                JooqItemPropertyHandler.VALUE.as("Name"));

        if (Gateway.getProperties().getBoolean("JOOQ.TemporaryPwdFieldImplemented", true))
            select.addSelect(JooqItemHandler.IS_PASSWORD_TEMPORARY);

        select.addOrderBy(field(name("Name")));

        if (limit  > 0) select.addLimit(limit);
        if (offset > 0) select.addOffset(offset);

        log.debug("getAgents() - role:{}  SQL:\n{}", role, select);

        Result<?> result = select.fetch();

        PagedResult pResult = new PagedResult();

        if (result != null) {
            pResult.maxRows = maxRows;

            for (Record record: result) {
                try {
                    pResult.rows.add(JooqItemHandler.getItemPath(context, null, record));
                }
                catch (PersistencyException e) {
                    // This shall never happen
                    log.error("", e);
                    throw new ObjectNotFoundException(e.getMessage());
                }
            }
        }

        return pResult;
    }

    @Override
    public RolePath[] getRoles(AgentPath agent) {
        try {
            DSLContext context = JooqHandler.connect();
            return roles.findRolesOfAgent(context, agent, permissions).toArray(new RolePath[0]);
        }
        catch (PersistencyException e) {
            log.error("", e);
        }
        return new RolePath[0];
    }

    @Override
    public PagedResult getRoles(AgentPath agent, int offset, int limit) {
        try {
            DSLContext context = JooqHandler.connect();
            return new PagedResult(
                    roles.countRolesOfAgent(context, agent),
                    roles.findRolesOfAgent(context, agent, offset, limit, permissions));
        }
        catch (PersistencyException e) {
            log.error("", e);
        }
        return new PagedResult();
    }

    @Override
    public boolean hasRole(AgentPath agent, RolePath role) {
        try {
            DSLContext context = JooqHandler.connect();
            return roles.exists(context, role, agent);
        }
        catch (PersistencyException e) {
            log.error("", e);
        }
        return false;
    }

    @Override
    public void removeRole(AgentPath agent, RolePath role) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        if (!exists(role))  throw new ObjectNotFoundException("Role:"+role);
        if (!exists(agent)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested -> {
                int rows = roles.delete(DSL.using(nested), role, agent);
                if (rows == 0)
                    throw new ObjectCannotBeUpdated("Role:"+role+" Agent:"+agent + " are not related.");
            });

        }
        catch (Exception e) {
            throw new ObjectCannotBeUpdated("Role:"+role+" Agent:"+agent + " error:" + e.getMessage());
        }
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        setAgentPassword(agent, newPassword, false);
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword, boolean temporary) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        if (!exists(agent)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested ->{
                int rows = items.updatePassword(DSL.using(nested), agent, passwordHasher.hashPassword(newPassword.toCharArray()), temporary);
                if (rows != 1) throw new ObjectCannotBeUpdated("Agent:"+agent);
            });
        }
        catch (Exception e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated("Agent:"+agent + " error:" + e.getMessage());
        }
    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(role)) throw new ObjectNotFoundException("Role:"+role);

        role.setHasJobList(hasJobList);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested -> {
                roles.update(DSL.using(nested), role);
            });

        }
        catch (Exception e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated("Role:"+role + " error:" + e.getMessage());
        }
    }

    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath) {
        try {
            DSLContext context = JooqHandler.connect();
            return domains.find(context, itemPath).iterator();
        }
        catch (PersistencyException e) {
            log.error("", e);
        }
        return new ArrayList<Path>().iterator();
    }

    @Override
    public PagedResult searchAliases(ItemPath itemPath, int offset, int limit) {
        try {
            DSLContext context = JooqHandler.connect();
            return new PagedResult(
                    domains.countFind(context, itemPath),
                    domains.find(context, itemPath, offset, limit) );
        }
        catch (PersistencyException e) {
            log.error("", e);
        }
        return new PagedResult();
    }

    @Override
    public void setIOR(ItemPath item, String ior) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(item)) throw new ObjectNotFoundException("Item:"+item);

        item.setIORString(ior);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested -> {
                items.updateIOR(DSL.using(nested), item, ior);
            });
        }
        catch (Exception e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated("Item:" + item + " error:" + e.getMessage());
        }
    }

    @Override
    public void setPermission(RolePath role, String permission) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        ArrayList<String> permissions = new ArrayList<>();

        if (StringUtils.isNotBlank(permission)) permissions.add(permission);

        //empty permission list shall clear the permissions of Role
        setPermissions(role, permissions);
    }

    @Override
    public void setPermissions(RolePath role, List<String> permissions) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(role)) throw new ObjectNotFoundException("Role:"+role);

        role.setPermissions(permissions);

        try {
            DSLContext context = JooqHandler.connect();
            context.transaction(nested ->{
              //empty permission list shall clear the permissions of Role
                if (this.permissions.exists(DSL.using(nested),role.getStringPath())) {
                    this.permissions.delete(DSL.using(nested), role.getStringPath());
                }
                this.permissions.insert(DSL.using(nested), role.getStringPath(), role.getPermissionsList());
            });
        }
        catch (Exception e) {
            log.error("", e);
            throw new ObjectCannotBeUpdated("Role:"+role + " error:" + e.getMessage());
        }
    }

    @Override
    public void postStartServer() {
    }

    @Override
    public void postBoostrap() {
    }
}
