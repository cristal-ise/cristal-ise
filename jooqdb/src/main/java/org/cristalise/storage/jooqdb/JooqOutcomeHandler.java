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
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

public class JooqOutcomeHandler implements JooqHandler {

    public static final String tableName = "OUTCOME";

    private List<Condition> getPKConditions(UUID uuid, String... primaryKeys) {
        List<Condition> conditions = new ArrayList<>();

        switch (primaryKeys.length) {
            case 0: 
                conditions.add(field("UUID").equal(uuid));
                break;
            case 1:
                conditions.add(field("UUID"       ).equal(uuid));
                conditions.add(field("SCHEMA_NAME").equal(primaryKeys[0]));
                break;
            case 2:
                conditions.add(field("UUID"          ).equal(uuid));
                conditions.add(field("SCHEMA_NAME"   ).equal(primaryKeys[0]));
                conditions.add(field("SCHEMA_VERSION").equal(primaryKeys[1]));
                break;
            case 3:
                conditions.add(field("UUID"          ).equal(uuid));
                conditions.add(field("SCHEMA_NAME"   ).equal(primaryKeys[0]));
                conditions.add(field("SCHEMA_VERSION").equal(primaryKeys[1]));
                conditions.add(field("EVENT_ID"      ).equal(primaryKeys[2]));
                break;

            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 4):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Outcome outcome = (Outcome)obj;

        String schemaName    = outcome.getSchema().getName();
        String schemaVersion = outcome.getSchema().getVersion().toString();
        String eventID       = outcome.getID().toString();

        C2KLocalObject o = fetch(context, uuid, schemaName, schemaVersion, eventID);

        if (o == null) return insert(context, uuid, outcome);
        else           return update(context, uuid, outcome);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject o) {
        throw new IllegalArgumentException("Outcome must not be updated");
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(table(tableName))
                .where(conditions)
                .execute();
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Outcome outcome = (Outcome)obj;
        return context
                .insertInto(
                    table(tableName), 
                        field("UUID"),
                        field("SCHEMA_NAME"),
                        field("SCHEMA_VERSION"),
                        field("EVENT_ID"),
                        field("XML")
                 )
                .values(uuid, outcome.getSchema().getName(), outcome.getSchema().getVersion(), outcome.getID(), outcome.getData())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) {
        String  schemaName    = primaryKeys[0];
        Integer schemaVersion = Integer.parseInt(primaryKeys[1]);
        Integer eventID       = Integer.parseInt(primaryKeys[2]);

        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
                  .and(field("SCHEMA_NAME").equal(schemaName))
                  .and(field("SCHEMA_VERSION").equal(schemaVersion))
                  .and(field("EVENT_ID").equal(eventID))
                .fetchOne();

        if(result != null) {
            try {
                String xml = result.get(field("XML", String.class));
                return new Outcome(eventID, xml, LocalObjectLoader.getSchema(schemaName, schemaVersion));
            }
            catch (InvalidDataException | ObjectNotFoundException e) {
                Logger.error(e);
            }
        }
        return null;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) {
        Field<?>[] fields = new Field[1];

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        switch (primaryKeys.length) {
            case 0: 
                fields[0] = field("SCHEMA_NAME");
                break;
            case 1:
                fields[0] = field("SCHEMA_VERSION");
                break;
            case 2:
                fields[0] = field("EVENT_ID");
                break;
            case 3:
                fields[0] = field("EVENT_ID");
                break;

            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 4):"+Arrays.toString(primaryKeys));
        }

        Logger.msg(DSL.selectDistinct(fields).from(table(tableName)).where(conditions).getSQL());

        Result<Record> result = context
                .selectDistinct(fields)
                .from(table(tableName))
                .where(conditions)
                .fetch();

        String[] returnValue = new String[result.size()];

        int i = 0;
        for (Record rec : result) returnValue[i++] = rec.get(0).toString();

        return returnValue;
    }

    @Override
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",           UUID.class),    UUID_TYPE.   nullable(false))
            .column(field("SCHEMA_NAME",    String.class),  NAME_TYPE.   nullable(false))
            .column(field("SCHEMA_VERSION", String.class),  VERSION_TYPE.nullable(false))
            .column(field("EVENT_ID",       Integer.class), EVENTID_TYPE.nullable(false))
            .column(field("XML",            String.class),  XML_TYPE.    nullable(false))
            .constraints(constraint("PK_UUID").primaryKey(field("UUID"), 
                                                          field("SCHEMA_NAME"), 
                                                          field("SCHEMA_VERSION"), 
                                                          field("EVENT_ID")))
        .execute();
    }
}
