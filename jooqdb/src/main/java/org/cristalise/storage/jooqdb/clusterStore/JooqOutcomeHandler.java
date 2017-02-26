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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
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
import org.jooq.Result;
import org.jooq.Table;

public class JooqOutcomeHandler implements JooqHandler {
    static final Table<?> OUTCOME_TABLE = table(name("OUTCOME"));

    static final Field<UUID>    UUID            = field(name("UUID"),           UUID.class);
    static final Field<String>  SCHEMA_NAME     = field(name("SCHEMA_NAME"),    String.class);
    static final Field<Integer> SCHEMA_VERSION  = field(name("SCHEMA_VERSION"), Integer.class);
    static final Field<Integer> EVENT_ID        = field(name("EVENT_ID"),       Integer.class);
    static final Field<String>  XML             = field(name("XML"),            String.class);

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
                throw new PersistencyException("Invalid number of primary keys (max 4):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Outcome outcome = (Outcome)obj;

        String schemaName    = outcome.getSchema().getName();
        String schemaVersion = outcome.getSchema().getVersion().toString();
        String eventID       = outcome.getID().toString();

        C2KLocalObject o = fetch(context, uuid, schemaName, schemaVersion, eventID);

        if (o == null) return insert(context, uuid, outcome);
        else           return update(context, uuid, outcome);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        throw new IllegalArgumentException("Outcome must not be updated uuid:"+uuid+" name:"+obj.getName());
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(OUTCOME_TABLE)
                .where(conditions)
                .execute();
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
        String  schemaName    = primaryKeys[0];
        Integer schemaVersion = Integer.parseInt(primaryKeys[1]);
        Integer eventID       = Integer.parseInt(primaryKeys[2]);

        Record result = context
                .select().from(OUTCOME_TABLE)
                .where(UUID          .equal(uuid))
                  .and(SCHEMA_NAME   .equal(schemaName))
                  .and(SCHEMA_VERSION.equal(schemaVersion))
                  .and(EVENT_ID      .equal(eventID))
                .fetchOne();

        if(result != null) {
            try {
                String xml = result.get(XML);
                return new Outcome(eventID, xml, LocalObjectLoader.getSchema(schemaName, schemaVersion));
            }
            catch (InvalidDataException | ObjectNotFoundException e) {
                Logger.error(e);
                throw new PersistencyException(e.getMessage());
            }
        }
        return null;
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
                fields[0] = SCHEMA_VERSION;
                break;
            case 2:
                fields[0] = EVENT_ID;
                break;
            case 3:
                fields[0] = EVENT_ID;
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys (max 4):"+Arrays.toString(primaryKeys));
        }

        Result<Record> result = context
                .selectDistinct(fields)
                .from(OUTCOME_TABLE)
                .where(conditions)
                .fetch();

        String[] returnValue = new String[result.size()];

        int i = 0;
        for (Record rec : result) returnValue[i++] = rec.get(0).toString();

        return returnValue;
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
