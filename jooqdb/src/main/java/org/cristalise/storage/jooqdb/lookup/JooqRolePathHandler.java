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

    public int update(DSLContext context, RolePath path) throws PersistencyException {
        throw new PersistencyException("Unimplemented");
    }

    public int delete(DSLContext context, String path) throws PersistencyException {
        return context
                .delete(ROLE_PATH_TABLE)
                .where(PATH.equal(path))
                .execute();
    }

    public int insert(DSLContext context, RolePath path) throws PersistencyException {
        return context
                .insertInto(ROLE_PATH_TABLE)
                    .set(PATH,     path.getStringPath())
                    .set(JOBLIST,  path.hasJobList())
                .execute();
   }

    public boolean exists(DSLContext context, String path) throws PersistencyException {
        Record1<Integer> count = context
                .selectCount().from(ROLE_PATH_TABLE)
                .where(PATH.equal(path))
                .fetchOne();
        return count != null && count.get(0, Integer.class) == 1;
    }

    public RolePath fetch(DSLContext context, String path) throws PersistencyException {
        Record result = context
                .select().from(ROLE_PATH_TABLE)
                .where(PATH.equal(path))
                .fetchOne();

        if(result != null) {
            return new RolePath(path, result.get(JOBLIST));
        }
        return null;
    }

    public List<Path> findByRegex(DSLContext context, String pattern) {
        Result<Record> result = context
                .select().from(ROLE_PATH_TABLE)
                .where(PATH.likeRegex(pattern))
                .fetch();

        List<Path> foundPathes = new ArrayList<>();

        if (result != null) {
            for (Record record : result) foundPathes.add(new RolePath(record.get(PATH), record.get(JOBLIST)));
        }

        return foundPathes;
    }

    public List<Path> find(DSLContext context, String startPath, String name) {
        String pattern;

        if (startPath.endsWith("/")) pattern = startPath + "%"  + name;
        else                         pattern = startPath + "/%" + name;

        return find(context, pattern);
    }

    public List<Path> find(DSLContext context, String pattern) {
        Result<Record> result = context
                .select().from(ROLE_PATH_TABLE)
                .where(PATH.like(pattern))
                .fetch();

        ArrayList<Path> roles = new ArrayList<>();

        if(result != null) {
            for (Record record: result) {
                roles.add(new RolePath(record.get(PATH), record.get(JOBLIST)));
            }
        }
        return roles;
    }

    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(ROLE_PATH_TABLE)
            .column(PATH,    JooqHandler.STRING_TYPE .nullable(false))
            .column(JOBLIST, SQLDataType.BOOLEAN     .nullable(false))
            .column(AGENT,   JooqHandler.UUID_TYPE   .nullable(true))
            .constraints(
                    constraint("PK_"+ROLE_PATH_TABLE).primaryKey(PATH),
                    constraint("FK_"+ROLE_PATH_TABLE).foreignKey(AGENT).references(JooqItemHandler.ITEM_TABLE, JooqItemHandler.UUID)
            )
        .execute();
    }
}
