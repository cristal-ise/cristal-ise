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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.Logger;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

public class JooqHistoryHandler implements JooqHandler {
    public static final String tableName = "EVENT";

    private List<Condition> getPKConditions(UUID uuid, String... primaryKeys) {
        List<Condition> conditions = new ArrayList<>();

        switch (primaryKeys.length) {
            case 0: 
                conditions.add(field("UUID").equal(uuid));
                break;
            case 1:
                conditions.add(field("UUID").equal(uuid));
                conditions.add(field("ID"  ).equal(primaryKeys[0]));
                break;

            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 4):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Field<?>[] fields = { field("ID") };

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

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
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        C2KLocalObject e = fetch(context, uuid, obj.getName());

        if (e == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        throw new PersistencyException("Event must not be updated uuid:"+uuid+" id:"+obj.getName());
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Event event = (Event)obj;
        GTimeStamp ts = event.getTimeStamp();
        AgentPath delegate = event.getDelegatePath();

        return context
                .insertInto(
                    table(tableName), 
                    field("UUID",                 UUID.class),
                    field("ID",                   Integer.class),
                    field("AGENT_UUID",           UUID.class),
                    field("DELEGATE_UUID",        UUID.class),
                    field("AGENT_ROLE",           String.class),
                    field("SCHEMA_NAME",          String.class),
                    field("SCHEMA_VERSION",       String.class),
                    field("STATEMACHINE_NAME",    String.class),
                    field("STATEMACHINE_VERSION", String.class),
                    field("STEP_NAME",            String.class),
                    field("STEP_PATH",            String.class),
                    field("STEP_TYPE",            String.class),
                    field("ORIGIN_STATE_ID",      Integer.class),
                    field("TARGET_STATE_ID",      Integer.class),
                    field("TRANSITION_ID",        Integer.class),
                    field("VIEW_NAME",            String.class),
                    field("TIMESTAMP",            Timestamp.class)
//                    field("TIMESTAMP",            OffsetDateTime.class)
                 )
                .values(uuid, 
                        event.getID(),
                        event.getAgentPath().getUUID(),
                        delegate == null ?  null : delegate.getUUID(),
                        event.getAgentRole(),
                        event.getSchemaName(),
                        event.getSchemaVersion().toString(),
                        event.getStateMachineName(),
                        event.getStateMachineVersion().toString(),
                        event.getStepName(),
                        event.getStepPath(),
                        event.getStepType(),
                        event.getOriginState(),
                        event.getTargetState(),
                        event.getTransition(),
                        event.getViewName(),
                        DateUtility.toSqlTimestamp(ts))
//                        DateUtility.toOffsetDateTime(ts))
                .execute();
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(table(tableName))
                .where(conditions)
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Integer id = Integer.parseInt(primaryKeys[0]);
        
        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
                  .and(field("ID").equal(id))
                .fetchOne();

        if (result != null) {
            UUID agent    = result.get(field("AGENT_UUID",    UUID.class));
            UUID delegate = result.get(field("DELEGATE_UUID", UUID.class));

            GTimeStamp ts = DateUtility.fromSqlTimestamp( result.get(field("TIMESTAMP", Timestamp.class)));
            //GTimeStamp ts = DateUtility.fromOffsetDateTime( result.get(field("TIMESTAMP", OffsetDateTime.class)));

            try {
                return new Event(
                        result.get(field("ID", Integer.class)),
                        new ItemPath(uuid),
                        new AgentPath(agent),
                        (delegate == null) ? null : new AgentPath(delegate),
                        result.get(field("AGENT_ROLE",           String.class)),
                        result.get(field("STEP_NAME",            String.class)),
                        result.get(field("STEP_PATH",            String.class)),
                        result.get(field("STEP_TYPE",            String.class)),
                        result.get(field("STATEMACHINE_NAME",    String.class)),
                        result.get(field("STATEMACHINE_VERSION", Integer.class)),
                        result.get(field("TRANSITION_ID",        Integer.class)),
                        result.get(field("ORIGIN_STATE_ID",      Integer.class)),
                        result.get(field("TARGET_STATE_ID",      Integer.class)),
                        result.get(field("SCHEMA_NAME",          String.class)),
                        result.get(field("SCHEMA_VERSION",       Integer.class)),
                        result.get(field("VIEW_NAME",            String.class)),
                        ts);
            }
            catch (InvalidAgentPathException | IllegalArgumentException ex) {
                Logger.error(ex);
                throw new PersistencyException(ex.getMessage());
            }
        }
        else
            return null;
    }

    @Override
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",                 UUID.class),           UUID_TYPE      .nullable(false))
            .column(field("ID",                   Integer.class),        ID_TYPE        .nullable(false))
            .column(field("AGENT_UUID",           UUID.class),           UUID_TYPE      .nullable(false))
            .column(field("DELEGATE_UUID",        UUID.class),           UUID_TYPE      .nullable(true))
            .column(field("AGENT_ROLE",           String.class),         NAME_TYPE      .nullable(false))
            .column(field("SCHEMA_NAME",          String.class),         NAME_TYPE      .nullable(false))
            .column(field("SCHEMA_VERSION",       Integer.class),        VERSION_TYPE   .nullable(false))
            .column(field("STATEMACHINE_NAME",    String.class),         NAME_TYPE      .nullable(false))
            .column(field("STATEMACHINE_VERSION", Integer.class),        VERSION_TYPE   .nullable(false))
            .column(field("STEP_NAME",            String.class),         NAME_TYPE      .nullable(false))
            .column(field("STEP_PATH",            String.class),         NAME_TYPE      .nullable(false))
            .column(field("STEP_TYPE",            String.class),         NAME_TYPE      .nullable(false))
            .column(field("ORIGIN_STATE_ID",      Integer.class),        ID_TYPE        .nullable(false))
            .column(field("TARGET_STATE_ID",      Integer.class),        ID_TYPE        .nullable(false))
            .column(field("TRANSITION_ID",        Integer.class),        ID_TYPE        .nullable(false))
            .column(field("VIEW_NAME",            String.class),         NAME_TYPE      .nullable(false))
            .column(field("TIMESTAMP",            Timestamp.class),      TIMESTAMP_TYPE .nullable(false))
//          .column(field("TIMESTAMP",            OffsetDateTime.class), TIMESTAMP_TYPE .nullable(false))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID"), field("ID")))
        .execute();
    }
}
