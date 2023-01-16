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

import static org.cristalise.kernel.entity.proxy.ProxyMessage.Type.ADD;
import static org.cristalise.kernel.entity.proxy.ProxyMessage.Type.DELETE;
import static org.cristalise.kernel.lookup.Lookup.SearchConstraints.WILDCARD_MATCH;
import static org.cristalise.storage.jooqdb.JooqDataSourceHandler.retrieveContext;
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
import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.storage.jooqdb.JooqDataSourceHandler;
import org.cristalise.storage.jooqdb.auth.Argon2Password;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JoinType;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
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
        JooqDataSourceHandler.readSystemProperties();

        try {
            items       = new JooqItemHandler();
            domains     = new JooqDomainPathHandler();
            roles       = new JooqRolePathHandler();
            permissions = new JooqPermissionHandler();
            properties  = new JooqItemPropertyHandler();

            if (! JooqDataSourceHandler.readOnlyDataSource) {
                retrieveContext(null).transaction(nested -> {
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
            log.error("open()", ex);
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    public void dropHandlers() throws PersistencyException {
        retrieveContext(null).transaction(nested -> {
            properties .dropTables(DSL.using(nested));
            permissions.dropTables(DSL.using(nested));
            roles      .dropTables(DSL.using(nested));
            domains    .dropTables(DSL.using(nested));
            items      .dropTables(DSL.using(nested));
        });

        items       = null;
        domains     = null;
        roles       = null;
        permissions = null;
        properties  = null;
    }

    @Override
    public void initializeDirectory(TransactionKey transactionKey) throws ObjectNotFoundException {
        log.debug("initializeDirectory() - NOTHING done.");
    }

    @Override
    public void close() {
        try {
            JooqDataSourceHandler.closeDataSource();
        }
        catch (DataAccessException | PersistencyException e) {
            log.error("close()", e);
        }
    }

    @Override
    public boolean exists(Path path, TransactionKey transactionKey) {
        if (path == null) return false;

        List<Boolean> itemExists = new ArrayList<>();

        try {
            DSLContext context = retrieveContext(transactionKey);
            boolean isExist = false;

            if      (path instanceof ItemPath)   isExist = items.exists(context, path.getUUID());
            else if (path instanceof AgentPath)  isExist = items.exists(context, path.getUUID());
            else if (path instanceof DomainPath) isExist = domains.exists(context, (DomainPath)path);
            else if (path instanceof RolePath)   isExist = roles.exists(context, (RolePath)path,null);

            if (isExist) itemExists.add(isExist);

            JooqDataSourceHandler.logConnectionCount("JooqLookupManager.exists()", context);
        }
        catch (PersistencyException e) {
            log.error("exists()", e);
        }

        return itemExists.size() > 0 ? true : false;
    }

    @Override
    public void add(Path newPath, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectAlreadyExistsException {
        if (exists(newPath, transactionKey)) throw new ObjectAlreadyExistsException("Path exist:"+newPath);

        log.debug("add() - path:"+newPath);

        try {
            DSLContext context = retrieveContext(transactionKey);

            int rows = 0;
            if (newPath instanceof AgentPath) {
                rows = items.insert(context, (AgentPath) newPath, properties);
            } else if  (newPath instanceof ItemPath) {
                rows = items.insert(context, (ItemPath) newPath);
            } else if (newPath instanceof DomainPath) {
                rows = domains.insert(context, (DomainPath)newPath);
            } else if (newPath instanceof RolePath) {
                rows = (createRole((RolePath)newPath, transactionKey) != null) ? 1 : 0;
            }

            if (rows == 0) throw new ObjectCannotBeUpdated("JOOQLookupManager must insert some records:"+rows);
            else           log.debug("add() - path:"+newPath+" rows inserted:"+rows);

            if (newPath instanceof DomainPath) {
                Gateway.sendProxyEvent(new ProxyMessage(null, newPath.toString(), ADD));
            }

            JooqDataSourceHandler.logConnectionCount("JooqLookupManager.add()", context);
        }
        catch (PersistencyException e) {
            log.error("add()", e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }
    }

    @Override
    public void delete(Path path, TransactionKey transactionKey) throws ObjectCannotBeUpdated {
        if (!exists(path, transactionKey)) throw new ObjectCannotBeUpdated("Path does not exist:"+path);

        log.debug("delete() - path:"+path);

        try {
            if (getChildren(path, transactionKey).hasNext()) {
                throw new ObjectCannotBeUpdated("Path '"+path+"' is not a leaf");
            }

            DSLContext context = retrieveContext(transactionKey);

            int rows = 0;
            if  (path instanceof ItemPath) {
                rows = items  .delete(context, path.getUUID());
            } else if  (path instanceof AgentPath) {
                rows = items  .delete(context, path.getUUID());
            } else if (path instanceof DomainPath) {
                rows = domains  .delete(context, path.getStringPath());
            } else if (path instanceof RolePath) {
                permissions.delete(context, path.getStringPath());
                rows = roles.delete(context, (RolePath)path, null);
            }

            if (rows == 0) throw new ObjectCannotBeUpdated("JOOQLookupManager must delete some records:"+rows);
            else           log.debug("delete() - path:"+path+" rows deleted:"+rows);

            if (path instanceof DomainPath) {
                Gateway.sendProxyEvent(new ProxyMessage(null, path.toString(), DELETE));
            }
        }
        catch (PersistencyException e) {
            log.error("delete()", e);
            throw new ObjectCannotBeUpdated(e.getMessage());
        }

    }

    @Override
    public ItemPath getItemPath(String sysKey, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException {
        ItemPath ip = new ItemPath(sysKey);

        if (!exists(ip, transactionKey)) throw new ObjectNotFoundException("Path does not exist:"+sysKey);

        try {
            DSLContext context = retrieveContext(transactionKey);
            return items.fetch(context, ip.getUUID(), properties);
        }
        catch (PersistencyException e) {
            log.error("getItemPath()", e);
            throw new InvalidItemPathException(e.getMessage());
        }
    }

    private List<Path> find(DSLContext context , Path start, String name, List<UUID> uuids, SearchConstraints constraints) throws PersistencyException {
        log.debug("find() - start:"+start+" name:"+name);
        List<Path> paths = new ArrayList<>();

        if      (start instanceof DomainPath) paths.addAll(domains.find(context, (DomainPath)start, name, uuids, constraints));
        else if (start instanceof RolePath)   paths.addAll(roles.find(  context, (RolePath)start,   name, uuids, constraints));

        return paths;
    }

    @Override
    public Iterator<Path> search(Path start, String name, SearchConstraints constraints, TransactionKey transactionKey) {
        List<Path> result = new ArrayList<>();
        try {
            DSLContext context = retrieveContext(transactionKey);
            result.addAll(find(context, start, name, null, constraints));
        }
        catch (PersistencyException e) {
            log.error("search()", e);
        }

        if (result.isEmpty()) return new ArrayList<Path>().iterator(); //returns empty iterator
        else                  return result.iterator();
    }

    @Override
    public AgentPath getAgentPath(String agentName, TransactionKey transactionKey) throws ObjectNotFoundException {
        try {
            DSLContext context = retrieveContext(transactionKey);

            List<UUID> uuids = properties.findItemsByName(context, agentName);

            if (uuids.size() == 0) throw new ObjectNotFoundException("Could not find agent:"+agentName);

            // FIXME (Dirty Fix) there could many Items (very likely only 2) created with the name of the Agent.
            // Instead using the findItemsByName() create a new similar SQL joining ITEM table 'IS_AGENT = true'.
            for (UUID uuid: uuids) {
                ItemPath ip = items.fetch(context, uuid, properties);

                if (ip instanceof AgentPath) return (AgentPath)ip;
            }

            throw new ObjectNotFoundException("Could not find agent:"+agentName);
        }
        catch (PersistencyException e) {
            throw new ObjectNotFoundException("Could not retrieve agentName:"+agentName + " error:"+e.getMessage());
        }
    }

    @Override
    public RolePath getRolePath(String roleName, TransactionKey transactionKey) throws ObjectNotFoundException {
        List<UUID> uuids = new ArrayList<>();
        uuids.add(JooqRolePathHandler.NO_AGENT);

        try {
            DSLContext context = retrieveContext(transactionKey);
            List<Path> result = roles.find(context, "%/"+roleName, uuids);

            if      (result == null || result.size() == 0) throw new ObjectNotFoundException("Role '"+roleName+"' does not exist");
            else if (result.size() > 1)                    throw new ObjectNotFoundException("Unbiguos roleName:'"+roleName+"'");

            RolePath role = (RolePath)result.get(0);
            role.setPermissions(permissions.fetch(context, role.getStringPath()));

            return role;
        }
        catch (PersistencyException e) {
            log.error("getRolePath()", e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public ItemPath resolvePath(DomainPath domainPath, TransactionKey transactionKey) throws InvalidItemPathException, ObjectNotFoundException {
        if (!exists(domainPath, transactionKey)) throw new ObjectNotFoundException("Path does not exist:"+domainPath);

        try {
            DSLContext context = retrieveContext(transactionKey);
            DomainPath dp = domains.fetch(context, domainPath);

            if (dp.getTarget() == null) throw new InvalidItemPathException("DomainPath has no target:"+domainPath);

            //issue #165: using items.fetch() ensures that Path is either ItemPath or AgentPath
            return items.fetch(context, dp.getTarget().getUUID(), properties);
        }
        catch (PersistencyException e) {
            log.error("resolvePath()", e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    @Override
    public String getAgentName(AgentPath agentPath, TransactionKey transactionKey) throws ObjectNotFoundException {
        if (!exists(agentPath, transactionKey)) throw new ObjectNotFoundException("Path does not exist:"+agentPath);

        try {
            DSLContext context = retrieveContext(transactionKey);
            ItemPath ip = items.fetch(context, agentPath.getUUID(), properties);

            if (ip instanceof AgentPath) return ((AgentPath)ip).getAgentName(transactionKey);
            else                         throw new ObjectNotFoundException("Path is not an agent:"+agentPath);
        }
        catch (PersistencyException e) {
            log.error("getAgentName()", e);
            throw new ObjectNotFoundException(e.getMessage());
        }
    }

    public String getFullRegexPattern(Path path, String pattern, SQLDialect dialect) {
        String begin = "^";
        String end = "$";
        String pathString = path.getStringPath();

        if (! pathString.endsWith("/")) pathString = pathString + "/";

        if (dialect.equals(POSTGRES)) {
            begin = "(?e)" + begin;
            pathString = escapeSpecialChars(pathString);
        }

        return begin + pathString + pattern + end;
    }

    /**
     * use regex to match everything except '/' after the Path 
     * 
     * @param path
     * @param dialect
     * @return
     */
    public String getChildrenPattern(Path path, SQLDialect dialect) {
        return getFullRegexPattern(path, "[^/]*", dialect);
    }

    /**
     * use regex to match everything after the Path
     * 
     * @param path
     * @param dialect
     * @return
     */
    public String getContextTreePattern(Path path, SQLDialect dialect) {
        return getFullRegexPattern(path, ".*", dialect);
    }

    private String escapeSpecialChars(String s) {
        String specialCharsToReplace = Gateway.getProperties().getString("JooqLookupManager.getChildrenPattern.specialCharsToReplace", "[^a-zA-Z0-9 ]");
        return s.replaceAll("(" + specialCharsToReplace + ")", "\\\\$1");
    }

    @Override
    public PagedResult getContextTree(DomainPath path, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);

            String pattern = getContextTreePattern(path, context.dialect());
            log.info("getContextTree() - pattern:" + pattern);

            List<Path> result = domains.findByRegex(context, pattern, -1, 0, true);

            return new PagedResult(result.size(), result);
        }
        catch (Exception e) {
            log.error("getContextTree()", e);
        }
        return new PagedResult();
    }

    @Override
    public Iterator<Path> getChildren(Path path, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);

            String pattern = getChildrenPattern(path, context.dialect());
            log.debug("getChildren() - pattern:" + pattern);

            if      (path instanceof ItemPath) return new ArrayList<Path>().iterator(); //empty iterator
            else if (path instanceof RolePath) return roles  .findByRegex(context, pattern).iterator();
            else                               return domains.findByRegex(context, pattern, -1, 0, false).iterator();
        }
        catch (Exception e) {
            log.error("getChildren()", e);
        }
        return new ArrayList<Path>().iterator();
    }

    @Override
    public PagedResult getChildren(Path path, int offset, int limit, boolean contextOnly, TransactionKey transactionKey) {
        if (path instanceof ItemPath) return new PagedResult();

        int maxRows = 0;

        DSLContext context;
        try {
            context = retrieveContext(transactionKey);
        }
        catch (PersistencyException e) {
            log.error("getChildren()", e);
            return new PagedResult();
        }

        String pattern = getChildrenPattern(path, context.dialect());
        log.debug("getChildren() - pattern:{} offset:{} limit:{}", pattern, offset, limit);

        if      (path instanceof RolePath)   maxRows = roles  .countByRegex(context, pattern);
        else if (path instanceof DomainPath) maxRows = domains.countByRegex(context, pattern);

        if (maxRows == 0) return new PagedResult();

        List<Path> pathes = null;

        if      (path instanceof RolePath)   pathes = roles  .findByRegex(context, pattern, offset, limit);
        else if (path instanceof DomainPath) pathes = domains.findByRegex(context, pattern, offset, limit, contextOnly);

        JooqDataSourceHandler.logConnectionCount("JooqLookupManager.getChildren()", context);

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

        select.addConditions(Operator.AND, JooqDomainPathHandler.PATH.like(domains.getFindPattern(start, "", WILDCARD_MATCH)));

        return select;
    }

    @Override
    public Iterator<Path> search(Path start, TransactionKey transactionKey, Property... props) {
        return search(start, Arrays.asList(props), 0, 0, transactionKey).rows.iterator();
    }

    @Override
    public PagedResult search(Path start, List<Property> props, int offset, int limit, TransactionKey transactionKey) {
        if (!exists(start, transactionKey)) return new PagedResult(0, new ArrayList<Path>());

        DSLContext context;
        try {
            context = retrieveContext(transactionKey);
        }
        catch (PersistencyException e) {
            log.error("search()", e);
            return new PagedResult();
        }

        int maxRows = -1;

        // without limit no need to count the number of rows
        if (limit > 0) {
            SelectQuery<?> selectCount = getSearchSelect(context, start, props);
            selectCount.addSelect(DSL.count());

            log.trace("search(props) - SQL(count):\n{}", selectCount);

            maxRows = selectCount.fetchOne(0, int.class);

            if (maxRows == 0) return new PagedResult(0, new ArrayList<Path>());
        }

        SelectQuery<?> select = getSearchSelect(context, start, props);

        select.addSelect(JooqDomainPathHandler.PATH, TARGET);
        select.addOrderBy(JooqDomainPathHandler.PATH);

        if (limit  > 0) select.addLimit(limit);
        if (offset > 0) select.addOffset(offset);

        log.trace("search(props) - SQL:\n{}", select);

        return new PagedResult(maxRows, domains.getListOfPath(select.fetch()));
    }

    @Override
    public Iterator<Path> search(Path start, PropertyDescriptionList props, TransactionKey transactionKey) {
        return search(start, props, 0, 0, transactionKey).rows.iterator();
    }

    @Override
    public PagedResult search(Path start, PropertyDescriptionList props, int offset, int limit, TransactionKey transactionKey) {
        //FIXME: UNIMPLEMENTED search(PropertyDescriptionList)
        throw new RuntimeException("InMemoryLookup.search(PropertyDescriptionList) - UNIMPLEMENTED start:"+start);
    }

    @Override
    public RolePath createRole(RolePath role, TransactionKey transactionKey) throws ObjectAlreadyExistsException, ObjectCannotBeUpdated {
        log.debug("createRole() - role:"+role);

        if (exists(role, transactionKey)) throw new ObjectAlreadyExistsException("Role:"+role);

        try {
            DSLContext context = retrieveContext(transactionKey);
            role.getParent(transactionKey);
            roles.insert(context, role, null);
            permissions.insert(context, role.getStringPath(), role.getPermissionsList());

            return role;
        }
        catch (Exception e) {
            log.error("createRole()", e);
            throw new ObjectCannotBeUpdated("Parent role for '"+role+"' does not exists");
        }
    }

    @Override
    public void addRole(AgentPath agent, RolePath role, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        if (!exists(role, transactionKey))  throw new ObjectNotFoundException("Role:"+role);
        if (!exists(agent, transactionKey)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            DSLContext context = retrieveContext(transactionKey);
            RolePath finalRole = roles.fetch(context, role);
            int rows = roles.insert(context, finalRole, agent);
            if (rows != 1) throw new ObjectCannotBeUpdated("Updated rows must be 1 but it was '"+rows+"'");
       }
        catch (Exception e) {
            log.error("addRole()", e);
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
    public AgentPath[] getAgents(RolePath role, TransactionKey transactionKey) throws ObjectNotFoundException {
        return getAgents(role, -1, -1, transactionKey).rows.toArray(new AgentPath[0]);
    }

    @Override
    public PagedResult getAgents(RolePath role, int offset, int limit, TransactionKey transactionKey) throws ObjectNotFoundException {
        int maxRows = -1;

        DSLContext context;
        try {
            context = retrieveContext(transactionKey);
        }
        catch (PersistencyException e) {
            log.error("getAgents()", e);
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
                    log.error("getAgents()", e);
                    throw new ObjectNotFoundException(e.getMessage());
                }
            }
        }

        return pResult;
    }

    @Override
    public RolePath[] getRoles(AgentPath agent, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);
            return roles.findRolesOfAgent(context, agent, permissions).toArray(new RolePath[0]);
        }
        catch (PersistencyException e) {
            log.error("getRoles()", e);
        }
        return new RolePath[0];
    }

    @Override
    public PagedResult getRoles(AgentPath agent, int offset, int limit, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);
            return new PagedResult(
                    roles.countRolesOfAgent(context, agent),
                    roles.findRolesOfAgent(context, agent, offset, limit, permissions));
        }
        catch (PersistencyException e) {
            log.error("getRoles()", e);
        }
        return new PagedResult();
    }

    @Override
    public boolean hasRole(AgentPath agent, RolePath role, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);
            return roles.exists(context, role, agent);
        }
        catch (PersistencyException e) {
            log.error("hasRole()", e);
        }
        return false;
    }

    @Override
    public void removeRole(AgentPath agent, RolePath role, TransactionKey transactionKey) throws ObjectCannotBeUpdated, ObjectNotFoundException {
        if (!exists(role, transactionKey))  throw new ObjectNotFoundException("Role:"+role);
        if (!exists(agent, transactionKey)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            DSLContext context = retrieveContext(transactionKey);
            int rows = roles.delete(context, role, agent);
            if (rows == 0)
                throw new ObjectCannotBeUpdated("Role:"+role+" Agent:"+agent + " are not related.");

        }
        catch (Exception e) {
            throw new ObjectCannotBeUpdated("Role:"+role+" Agent:"+agent + " error:" + e.getMessage());
        }
    }

    @Override
    public void setAgentPassword(AgentPath agent, String newPassword, boolean temporary, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated, NoSuchAlgorithmException {
        if (!exists(agent, transactionKey)) throw new ObjectNotFoundException("Agent:"+agent);

        try {
            DSLContext context = retrieveContext(transactionKey);
            int rows = items.updatePassword(context, agent, passwordHasher.hashPassword(newPassword.toCharArray()), temporary);
            if (rows != 1) throw new ObjectCannotBeUpdated("Agent:"+agent);
        }
        catch (Exception e) {
            log.error("setAgentPassword()", e);
            throw new ObjectCannotBeUpdated("Agent:"+agent + " error:" + e.getMessage());
        }
    }

    @Override
    public void setHasJobList(RolePath role, boolean hasJobList, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(role, transactionKey)) throw new ObjectNotFoundException("Role:"+role);

        role.setHasJobList(hasJobList);

        try {
            DSLContext context = retrieveContext(transactionKey);
            roles.update(context, role);
        }
        catch (Exception e) {
            log.error("setHasJobList()", e);
            throw new ObjectCannotBeUpdated("Role:"+role + " error:" + e.getMessage());
        }
    }

    @Override
    public Iterator<Path> searchAliases(ItemPath itemPath, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);
            return domains.find(context, itemPath).iterator();
        }
        catch (PersistencyException e) {
            log.error("searchAliases()", e);
        }
        return new ArrayList<Path>().iterator();
    }

    @Override
    public PagedResult searchAliases(ItemPath itemPath, int offset, int limit, TransactionKey transactionKey) {
        try {
            DSLContext context = retrieveContext(transactionKey);
            return new PagedResult(
                    domains.countFind(context, itemPath),
                    domains.find(context, itemPath, offset, limit) );
        }
        catch (PersistencyException e) {
            log.error("searchAliases()", e);
        }
        return new PagedResult();
    }

    @Override
    public void setPermission(RolePath role, String permission, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        ArrayList<String> permissions = new ArrayList<>();

        if (StringUtils.isNotBlank(permission)) permissions.add(permission);

        //empty permission list shall clear the permissions of Role
        setPermissions(role, permissions, transactionKey);
    }

    @Override
    public void setPermissions(RolePath role, List<String> permissions, TransactionKey transactionKey) throws ObjectNotFoundException, ObjectCannotBeUpdated {
        if (!exists(role, transactionKey)) throw new ObjectNotFoundException("Role:"+role);

        role.setPermissions(permissions);

        try {
            DSLContext context = retrieveContext(transactionKey);
              //empty permission list shall clear the permissions of Role
            if (this.permissions.exists(context, role.getStringPath())) {
                this.permissions.delete(context, role.getStringPath());
            }
            this.permissions.insert(context, role.getStringPath(), role.getPermissionsList());
        }
        catch (Exception e) {
            log.error("setPermissions()", e);
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
