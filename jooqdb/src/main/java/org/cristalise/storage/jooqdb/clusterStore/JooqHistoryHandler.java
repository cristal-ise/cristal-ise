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
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;

public class JooqHistoryHandler implements JooqHandler {
    static final Table<?> EVENT_TABLE = table(name("EVENT"));

    static final Field<UUID>      UUID                  = field(name("UUID"),                 UUID.class);
    static final Field<Integer>   ID                    = field(name("ID"),                   Integer.class);
    static final Field<UUID>      AGENT_UUID            = field(name("AGENT_UUID"),           UUID.class);
    static final Field<UUID>      DELEGATE_UUID         = field(name("DELEGATE_UUID"),        UUID.class);
    static final Field<String>    AGENT_ROLE            = field(name("AGENT_ROLE"),           String.class);
    static final Field<String>    SCHEMA_NAME           = field(name("SCHEMA_NAME"),          String.class);
    static final Field<Integer>   SCHEMA_VERSION        = field(name("SCHEMA_VERSION"),       Integer.class);
    static final Field<String>    STATEMACHINE_NAME     = field(name("STATEMACHINE_NAME"),    String.class);
    static final Field<Integer>   STATEMACHINE_VERSION  = field(name("STATEMACHINE_VERSION"), Integer.class);
    static final Field<String>    STEP_NAME             = field(name("STEP_NAME"),            String.class);
    static final Field<String>    STEP_PATH             = field(name("STEP_PATH"),            String.class);
    static final Field<String>    STEP_TYPE             = field(name("STEP_TYPE"),            String.class);
    static final Field<Integer>   ORIGIN_STATE_ID       = field(name("ORIGIN_STATE_ID"),      Integer.class);
    static final Field<Integer>   TARGET_STATE_ID       = field(name("TARGET_STATE_ID"),      Integer.class);
    static final Field<Integer>   TRANSITION_ID         = field(name("TRANSITION_ID"),        Integer.class);
    static final Field<String>    VIEW_NAME             = field(name("VIEW_NAME"),            String.class);
    static final Field<Timestamp> TIMESTAMP             = field(name("TIMESTAMP"),            Timestamp.class);

//    static final Field<OffsetDateTime> TIMESTAMP = field(name("TIMESTAMP"), Timestamp.class);

    private List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = new ArrayList<>();

        switch (primaryKeys.length) {
            case 0: 
                conditions.add(UUID.equal(uuid));
                break;
            case 1:
                conditions.add(UUID.equal(uuid));
                conditions.add(ID.equal(Integer.valueOf(primaryKeys[0])));
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys (max 4):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Field<?>[] fields = { ID };

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        //Logger.msg(DSL.selectDistinct(fields).from(TABLE_NAME).where(conditions).getSQL());

        Result<Record> result = context
                .selectDistinct(fields)
                .from(EVENT_TABLE)
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
        AgentPath delegate = event.getDelegatePath();

        return context
                .insertInto(EVENT_TABLE)
                    .set(UUID,                  uuid)
                    .set(ID,                    event.getID())
                    .set(AGENT_UUID,            event.getAgentPath().getUUID())
                    .set(DELEGATE_UUID,         delegate == null ?  null : delegate.getUUID())
                    .set(AGENT_ROLE,            event.getAgentRole())
                    .set(SCHEMA_NAME,           event.getSchemaName())
                    .set(SCHEMA_VERSION,        event.getSchemaVersion())
                    .set(STATEMACHINE_NAME,     event.getStateMachineName())
                    .set(STATEMACHINE_VERSION,  event.getStateMachineVersion())
                    .set(STEP_NAME,             event.getStepName())
                    .set(STEP_PATH,             event.getStepPath())
                    .set(STEP_TYPE,             event.getStepType())
                    .set(ORIGIN_STATE_ID,       event.getOriginState())
                    .set(TARGET_STATE_ID,       event.getTargetState())
                    .set(TRANSITION_ID,         event.getTransition())
                    .set(VIEW_NAME,             event.getViewName())
                    .set(TIMESTAMP,             DateUtility.toSqlTimestamp(event.getTimeStamp()))
                .execute();
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(EVENT_TABLE)
                .where(conditions)
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Integer id = Integer.parseInt(primaryKeys[0]);

        Record result = context
                .select().from(EVENT_TABLE)
                .where(UUID.equal(uuid))
                  .and(ID.equal(id))
                .fetchOne();

        if (result != null) {
            UUID agent    = result.get(AGENT_UUID);
            UUID delegate = result.get(DELEGATE_UUID);

            GTimeStamp ts = DateUtility.fromSqlTimestamp( result.get(TIMESTAMP));
            //GTimeStamp ts = DateUtility.fromOffsetDateTime( result.get(TIMESTAMP", OffsetDateTime.class)));

            try {
                return new Event(
                        result.get(ID),
                        new ItemPath(uuid),
                        new AgentPath(agent),
                        (delegate == null) ? null : new AgentPath(delegate),
                        result.get(AGENT_ROLE),
                        result.get(STEP_NAME),
                        result.get(STEP_PATH),
                        result.get(STEP_TYPE),
                        result.get(STATEMACHINE_NAME),
                        result.get(STATEMACHINE_VERSION),
                        result.get(TRANSITION_ID),
                        result.get(ORIGIN_STATE_ID),
                        result.get(TARGET_STATE_ID),
                        result.get(SCHEMA_NAME),
                        result.get(SCHEMA_VERSION),
                        result.get(VIEW_NAME),
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
        context.createTableIfNotExists(EVENT_TABLE)
            .column(UUID,                 UUID_TYPE      .nullable(false))
            .column(ID,                   ID_TYPE        .nullable(false))
            .column(AGENT_UUID,           UUID_TYPE      .nullable(false))
            .column(DELEGATE_UUID,        UUID_TYPE      .nullable(true))
            .column(AGENT_ROLE,           NAME_TYPE      .nullable(true))
            .column(SCHEMA_NAME,          NAME_TYPE      .nullable(true))
            .column(SCHEMA_VERSION,       VERSION_TYPE   .nullable(true))
            .column(STATEMACHINE_NAME,    NAME_TYPE      .nullable(false))
            .column(STATEMACHINE_VERSION, VERSION_TYPE   .nullable(false))
            .column(STEP_NAME,            NAME_TYPE      .nullable(false))
            .column(STEP_PATH,            NAME_TYPE      .nullable(false))
            .column(STEP_TYPE,            NAME_TYPE      .nullable(true))
            .column(ORIGIN_STATE_ID,      ID_TYPE        .nullable(false))
            .column(TARGET_STATE_ID,      ID_TYPE        .nullable(false))
            .column(TRANSITION_ID,        ID_TYPE        .nullable(false))
            .column(VIEW_NAME,            NAME_TYPE      .nullable(true))
            .column(TIMESTAMP,            TIMESTAMP_TYPE .nullable(false))
            .constraints(constraint("PK_"+EVENT_TABLE).primaryKey(UUID, ID))
        .execute();
    }
}
