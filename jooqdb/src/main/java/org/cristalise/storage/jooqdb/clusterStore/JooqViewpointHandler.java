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
package org.cristalise.storage.jooqdb.clusterStore;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;

public class JooqViewpointHandler implements JooqHandler {
    static final Table<?> VIEWPOINT_TABLE = table(name("VIEWPOINT"));

    static final Field<UUID>    UUID            = field(name("UUID"),           UUID.class);
    static final Field<String>  SCHEMA_NAME     = field(name("SCHEMA_NAME"),    String.class);
    static final Field<String>  NAME            = field(name("NAME"),           String.class);
    static final Field<Integer> SCHEMA_VERSION  = field(name("SCHEMA_VERSION"), Integer.class);
    static final Field<Integer> EVENT_ID        = field(name("EVENT_ID"),       Integer.class);

    private List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = new ArrayList<>();

        switch (primaryKeys.length) {
            case 0: 
                conditions.add(UUID.equal(uuid));
                break;
            case 1:
                conditions.add(UUID       .equal(uuid));
                conditions.add(SCHEMA_NAME.equal(primaryKeys[0]));
                break;
            case 2:
                conditions.add(UUID       .equal(uuid));
                conditions.add(SCHEMA_NAME.equal(primaryKeys[0]));
                conditions.add(NAME       .equal(primaryKeys[1]));
                break;

            default:
                throw new PersistencyException("Invalid number of primary keys (max 3):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        C2KLocalObject v = fetch(context, uuid, ((Viewpoint)obj).getSchemaName(), ((Viewpoint)obj).getName());

        if (v == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Viewpoint view = (Viewpoint)obj;
        return context
                .update(VIEWPOINT_TABLE)
                .set(SCHEMA_VERSION, view.getSchemaVersion())
                .set(EVENT_ID,       view.getEventId())
                .where(UUID       .equal(uuid))
                  .and(SCHEMA_NAME.equal(view.getSchemaName()))
                  .and(NAME       .equal(view.getName()))
                .execute();
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(VIEWPOINT_TABLE)
                .where(conditions)
                .execute();
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Viewpoint view = (Viewpoint)obj;
        return context
                .insertInto(VIEWPOINT_TABLE) 
                        .set(UUID,           uuid)
                        .set(SCHEMA_NAME,    view.getSchemaName())
                        .set(NAME,           view.getName())
                        .set(SCHEMA_VERSION, view.getSchemaVersion())
                        .set(EVENT_ID,       view.getEventId())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        String shcemaName = primaryKeys[0];
        String name       = primaryKeys[1];

        Record result = context
                .select().from(VIEWPOINT_TABLE)
                .where(UUID       .equal(uuid))
                  .and(SCHEMA_NAME.equal(shcemaName))
                  .and(NAME       .equal(name))
                .fetchOne();

        if(result != null) return new Viewpoint(new ItemPath(uuid),
                                                result.get(SCHEMA_NAME),
                                                result.get(NAME),
                                                result.get(SCHEMA_VERSION),
                                                result.get(EVENT_ID));
        else return null;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        Field<?>[] fields = new Field[1];

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        switch (primaryKeys.length) {
            case 0: 
                fields[0] = SCHEMA_NAME;
                break;
            case 1:
                fields[0] = NAME;
                break;
            case 2:
                fields[0] = NAME;
                break;

            default:
                throw new PersistencyException("Invalid number of primary keys (max 3):"+Arrays.toString(primaryKeys));
        }

        Result<Record> result = context
                .selectDistinct(fields)
                .from(VIEWPOINT_TABLE)
                .where(conditions)
                .fetch();

        String[] returnValue = new String[result.size()];

        int i = 0;
        for (Record rec : result) returnValue[i++] = rec.get(0).toString();

        return returnValue;
    }

    @Override
    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(VIEWPOINT_TABLE)
            .column(UUID,           UUID_TYPE.nullable(false))
            .column(SCHEMA_NAME,    NAME_TYPE.nullable(false))
            .column(NAME,           NAME_TYPE.nullable(false))
            .column(SCHEMA_VERSION, VERSION_TYPE.nullable(true))
            .column(EVENT_ID,       ID_TYPE.nullable(true))
            .constraints(
                    constraint("PK_"+VIEWPOINT_TABLE).primaryKey(UUID, SCHEMA_NAME, NAME))
        .execute();
    }
}
