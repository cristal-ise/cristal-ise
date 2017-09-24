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
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

public class JooqOutcomeHandler extends JooqHandler {
    static final Table<?> OUTCOME_TABLE = table(name("OUTCOME"));

    static final Field<UUID>    UUID            = field(name("UUID"),           UUID.class);
    static final Field<String>  SCHEMA_NAME     = field(name("SCHEMA_NAME"),    String.class);
    static final Field<Integer> SCHEMA_VERSION  = field(name("SCHEMA_VERSION"), Integer.class);
    static final Field<Integer> EVENT_ID        = field(name("EVENT_ID"),       Integer.class);
    static final Field<String>  XML             = field(name("XML"),            String.class);

    @Override
    protected Table<?> getTable() {
        return OUTCOME_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        switch (primaryKeys.length) {
            case 0: return SCHEMA_NAME;
            case 1: return SCHEMA_VERSION;
            case 2: return EVENT_ID;
            case 3: return null;
            default:
                throw new PersistencyException("Invalid number of primary keys:"+Arrays.toString(primaryKeys));
        }
    }

    @Override
    protected List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException {
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
                conditions.add(UUID          .equal(uuid));
                conditions.add(SCHEMA_NAME   .equal(primaryKeys[0]));
                conditions.add(SCHEMA_VERSION.equal(Integer.valueOf(primaryKeys[1])));
                break;
            case 3:
                conditions.add(UUID          .equal(uuid));
                conditions.add(SCHEMA_NAME   .equal(primaryKeys[0]));
                conditions.add(SCHEMA_VERSION.equal(Integer.valueOf(primaryKeys[1])));
                conditions.add(EVENT_ID      .equal(Integer.valueOf(primaryKeys[2])));
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys:"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        throw new IllegalArgumentException("Outcome must not be updated uuid:"+uuid+" name:"+obj.getName());
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Outcome outcome = (Outcome)obj;
        return context
                .insertInto(OUTCOME_TABLE)
                .set(UUID,           uuid)
                .set(SCHEMA_NAME,    outcome.getSchema().getName())
                .set(SCHEMA_VERSION, outcome.getSchema().getVersion())
                .set(EVENT_ID,       outcome.getID())
                .set(XML,            outcome.getData())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        Record result = fetchRecord(context, uuid, primaryKeys);

        if(result != null) {
            try {
                String xml = result.get(XML);
                return new Outcome(result.get(EVENT_ID), xml, LocalObjectLoader.getSchema(result.get(SCHEMA_NAME), result.get(SCHEMA_VERSION)));
            }
            catch (Exception e) {
                Logger.error(e);
                throw new PersistencyException(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(OUTCOME_TABLE)
        .column(UUID,           UUID_TYPE   .nullable(false))
        .column(SCHEMA_NAME,    NAME_TYPE   .nullable(false))
        .column(SCHEMA_VERSION, VERSION_TYPE.nullable(false))
        .column(EVENT_ID,       ID_TYPE     .nullable(false))
        .column(XML,            XML_TYPE    .nullable(false))
        .constraints(
                constraint("PK_"+OUTCOME_TABLE).primaryKey(UUID, SCHEMA_NAME, SCHEMA_VERSION, EVENT_ID))
        .execute();
    }
}
