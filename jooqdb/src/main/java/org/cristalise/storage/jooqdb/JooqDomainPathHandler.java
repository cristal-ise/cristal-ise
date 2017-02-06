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
package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.utils.Logger;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertQuery;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;

public class JooqDomainPathHandler {
    static final Table<?> DOMAIN_PATH_TABLE = table(name("DOMAIN_PATH"));

    static final Field<String> PATH   = field(name("PATH"),   String.class);
    static final Field<UUID>   TARGET = field(name("TARGET"), UUID.class);

//    public int update(DSLContext context, DomainPath path) throws PersistencyException {
//        throw new PersistencyException("Unimplemented");
//    }

    public int delete(DSLContext context, String path) throws PersistencyException {
        return context
                .delete(DOMAIN_PATH_TABLE)
                .where(PATH.equal(path))
                .execute();
    }

    public int insert(DSLContext context, DomainPath path) throws PersistencyException {
        InsertQuery<?> insertInto = context.insertQuery(DOMAIN_PATH_TABLE);

        StringBuffer newPath = new StringBuffer(Path.delim + path.getRoot());

        for (String name: path.getPath()) {
            newPath.append(Path.delim + name);

            if (!exists(context, newPath.toString())) {
                insertInto.addValue(PATH, newPath.toString());

                if (path.getStringPath().equals(newPath)) insertInto.addValue(TARGET, path.getUUID());
                else                                      insertInto.addValue(TARGET, (UUID)null);
                insertInto.newRecord();
            }
        }
        Logger.msg(insertInto.toString());
        return insertInto.execute();
   }

    public boolean exists(DSLContext context, String path) throws PersistencyException {
        Record1<Integer> count = context
                .selectCount().from(DOMAIN_PATH_TABLE)
                .where(PATH.equal(path))
                .fetchOne();

        return count != null && count.getValue(0, Integer.class) == 1;
    }

    public DomainPath fetch(DSLContext context, String path) throws PersistencyException {
        Record result = context
                .select().from(DOMAIN_PATH_TABLE)
                .where(PATH.equal(path))
                .fetchOne();

        if(result != null) {
            UUID uuid = result.get(TARGET);

            if(uuid == null) return new DomainPath(result.get(PATH));
            else             return new DomainPath(result.get(PATH), new ItemPath(uuid));
        }

        return null;
    }

    public List<Path> search(DSLContext context, String startPath, String name) {
        String pattern = "^$" + startPath+".*"+name;

        Result<Record> result = context
                .select().from(DOMAIN_PATH_TABLE)
                .where(PATH.likeRegex(pattern))
                .fetch();

        if (result != null) {
            List<Path> foundPathes = new ArrayList<Path>();
            for (Record record : result) {
                foundPathes.add(new DomainPath(record.getValue(PATH)));
            }
            return foundPathes;
        }
        return null;
    }

    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(DOMAIN_PATH_TABLE)
            .column(PATH,   JooqHandler.STRING_TYPE .nullable(false))
            .column(TARGET, JooqHandler.UUID_TYPE   .nullable(true))
            .constraints(
                    constraint("PK_"+DOMAIN_PATH_TABLE).primaryKey(PATH),
                    constraint("FK_"+DOMAIN_PATH_TABLE).foreignKey(TARGET).references(JooqItemHandler.ITEM_TABLE, JooqItemHandler.UUID)
             )
        .execute();
    }
}
