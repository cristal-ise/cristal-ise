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
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertQuery;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

public class JooqDomainPathHandler {
    static final Table<?> DOMAIN_PATH_TABLE = table(name("DOMAIN_PATH"));

    static final Field<String> PATH   = field(name("PATH"),   String.class);
    static final Field<UUID>   TARGET = field(name("TARGET"), UUID.class);

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

    private DomainPath getDomainPath(Record record) {
        UUID uuid = record.get(TARGET);

        if(uuid == null) return new DomainPath(record.get(PATH));
        else             return new DomainPath(record.get(PATH), new ItemPath(uuid));
    }

    private List<Path> getListOfPath(Result<?> result) {
        List<Path> foundPathes = new ArrayList<>();

        if (result != null) {
            for (Record record : result) foundPathes.add(getDomainPath(record));
        }

        return foundPathes;
    }

    public int delete(DSLContext context, String path) throws PersistencyException {
        return context
                .delete(DOMAIN_PATH_TABLE)
                .where(PATH.equal(path))
                .execute();
    }

    public int insert(DSLContext context, DomainPath path) throws PersistencyException {
        InsertQuery<?> insertInto = context.insertQuery(DOMAIN_PATH_TABLE);

        DomainPath root = new DomainPath();

        if (!exists(context, root)) {
            insertInto.addValue(PATH, root.getStringPath());
            insertInto.addValue(TARGET, (UUID)null);
            insertInto.newRecord();
        }

        StringBuffer newPath = new StringBuffer(Path.delim + path.getRoot());

        for (String name: path.getPath()) {
            newPath.append(Path.delim + name);

            if (!exists(context, new DomainPath(newPath.toString()))) {
                insertInto.addValue(PATH, newPath.toString());

                if (path.getStringPath().equals(newPath.toString())) insertInto.addValue(TARGET, path.getTarget() == null ? null: path.getTarget().getUUID());
                else                                                 insertInto.addValue(TARGET, (UUID)null);

                insertInto.newRecord();
            }
        }

        Logger.msg(8, "JooqDomainPathHandler.insert() - SQL:\n"+insertInto.toString());

        return insertInto.execute();
   }

    public boolean exists(DSLContext context, DomainPath path) throws PersistencyException {
        return context.fetchExists( select().from(DOMAIN_PATH_TABLE).where(PATH.equal(path.getStringPath())) );
    }

    public DomainPath fetch(DSLContext context, DomainPath path) throws PersistencyException {
        Record result = context
                .select().from(DOMAIN_PATH_TABLE)
                .where(PATH.equal(path.getStringPath()))
                .fetchOne();

        return getDomainPath(result);
    }

    public List<Path> findByRegex(DSLContext context, String pattern) {
        Result<Record> result = context
                .select().from(DOMAIN_PATH_TABLE)
                .where(PATH.likeRegex(pattern))
                .fetch();

        return getListOfPath(result);
    }

    public List<Path> find(DSLContext context, DomainPath startPath, String name, List<UUID> uuids) {
        String pattern;

        if (StringUtils.isBlank(name)) pattern = startPath.getStringPath() + "%";
        else                           pattern = startPath.getStringPath() + "/%" + name;

        SelectConditionStep<?> select = context.select().from(DOMAIN_PATH_TABLE).where(PATH.like(pattern));

        if (uuids != null && uuids.size() != 0) select.and(TARGET.in(uuids));
        
        Logger.msg(8, "JooqDomainPathHandler.find() - SQL:\n"+select);

        return getListOfPath(select.fetch());
    }

    public List<Path> find(DSLContext context, ItemPath item) {
        Result<Record> result = context
                .select().from(DOMAIN_PATH_TABLE)
                .where(TARGET.equal(item.getUUID()))
                .fetch();

        return getListOfPath(result);
    }
}
