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

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

public class JooqRolePathHandler {
    static final Table<?> ROLE_PATH_TABLE = table(name("ROLE_PATH"));

    static final Field<String>  PATH    = field(name("PATH"),    String.class);
    static final Field<Boolean> JOBLIST = field(name("JOBLIST"), Boolean.class);
    static final Field<UUID>    AGENT   = field(name("AGENT"),   UUID.class);
    
    static final UUID NO_AGENT = new UUID(0,0);

    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(ROLE_PATH_TABLE)
            .column(PATH,    JooqHandler.STRING_TYPE .nullable(false))
            .column(AGENT,   JooqHandler.UUID_TYPE   .nullable(false))
            .column(JOBLIST, SQLDataType.BOOLEAN     .nullable(false))
            .constraints(
                    constraint("PK_"+ROLE_PATH_TABLE).primaryKey(PATH, AGENT)
//                    constraint("FK_"+ROLE_PATH_TABLE).foreignKey(AGENT).references(JooqItemHandler.ITEM_TABLE, JooqItemHandler.UUID)
            )
        .execute();
    }

    public static RolePath getRolePath(Record record) {
        if (record != null) return new RolePath(record.get(PATH), record.get(JOBLIST));
        else                return null;
    }

    public static List<Path> getLisOfPaths(Result<Record> result) {
        ArrayList<Path> roles = new ArrayList<>();

        if(result != null) {
            for (Record record: result) roles.add(getRolePath(record));
        }
        return roles;
    }

    public int update(DSLContext context, RolePath role) throws PersistencyException {
        return context
                .update(ROLE_PATH_TABLE)
                    .set(JOBLIST, role.hasJobList())
                 .where(PATH.equal(role.getStringPath()))
                   .and(AGENT.equal(NO_AGENT))
                .execute();
    }

    public int delete(DSLContext context, RolePath role, AgentPath agent) throws PersistencyException {
        return context
                .delete(ROLE_PATH_TABLE)
                .where(PATH.equal(role.getStringPath()))
                .execute();
    }

    public int insert(DSLContext context, RolePath role, AgentPath agent) throws PersistencyException {
        UUID uuid = NO_AGENT;
        if (agent != null) uuid = agent.getUUID();

        return context
                .insertInto(ROLE_PATH_TABLE)
                    .set(PATH,    role.getStringPath())
                    .set(JOBLIST, role.hasJobList())
                    .set(AGENT,   uuid)
                .execute();
    }

    public boolean exists(DSLContext context, RolePath role, AgentPath agent) throws PersistencyException {
        UUID uuid = NO_AGENT;
        if (agent != null) uuid = agent.getUUID();

        Record1<Integer> count = context
                .selectCount().from(ROLE_PATH_TABLE)
                .where(PATH.equal(role.getStringPath()))
                  .and(AGENT.equal(uuid))
                .fetchOne();

        return count != null && count.get(0, Integer.class) == 1;
    }

    public RolePath fetch(DSLContext context, RolePath role) throws PersistencyException {
        Record result = context
                .select().from(ROLE_PATH_TABLE)
                .where(PATH.equal(role.getStringPath()))
                  .and(AGENT.equal(NO_AGENT))
                .fetchOne();

        return getRolePath(result);
    }

    public List<Path> findByRegex(DSLContext context, String pattern) {
        Result<Record> result = context
                .select().from(ROLE_PATH_TABLE)
                .where(PATH.likeRegex(pattern))
                .fetch();

        return getLisOfPaths(result);
    }

    public List<Path> find(DSLContext context, RolePath startPath, String name) {
        String pattern = "" + startPath.getStringPath() + "/%" + name;

        return find(context, pattern);
    }

    public List<Path> find(DSLContext context, String pattern) {
        Result<Record> result = context
                .select().from(ROLE_PATH_TABLE)
                .where(PATH.like(pattern))
                .fetch();

        return getLisOfPaths(result);
    }
}
