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

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.BOOLEAN;

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
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqHistoryHandler extends JooqHandler {
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
    static final Field<Boolean>   HAS_ATTACHMENT        = field(name("HAS_ATTACHMENT"),       Boolean.class);
    static final Field<Timestamp> TIMESTAMP             = field(name("TIMESTAMP"),            Timestamp.class);

    //static final Field<OffsetDateTime> TIMESTAMP = field(name("TIMESTAMP"), Timestamp.class);

    @Override
    protected Table<?> getTable() {
        return EVENT_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        if (primaryKeys.length == 0) return ID;
        else return null;
    }

    @Override
    protected List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException {
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
                throw new PersistencyException("Invalid number of primary keys (max 1):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        throw new PersistencyException("Event must not be updated - uuid:"+uuid+" id:"+obj.getName());
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Event event = (Event)obj;
        AgentPath delegate = event.getDelegatePath();

        InsertSetMoreStep<?> insert = context
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
                .set(TIMESTAMP,             DateUtility.toSqlTimestamp(event.getTimeStamp()));

        if (Gateway.getProperties().getBoolean("JOOQ.Event.enableHasAttachment", true)) {
            insert.set(HAS_ATTACHMENT, event.getHasAttachment());
        }

        return insert.execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Record result = fetchRecord(context, uuid, primaryKeys);

        if (result != null) {
            UUID agent    = getUUID(result, AGENT_UUID);
            UUID delegate = getUUID(result, DELEGATE_UUID);

            GTimeStamp ts = DateUtility.fromSqlTimestamp( result.get(TIMESTAMP));
            //GTimeStamp ts = DateUtility.fromOffsetDateTime( result.get(TIMESTAMP", OffsetDateTime.class)));

            Boolean hasAttachment = false;
            if (Gateway.getProperties().getBoolean("JOOQ.Event.enableHasAttachment", true)) {
                hasAttachment = result.get(HAS_ATTACHMENT.getName(), Boolean.class);
            }

            try {
                return new Event(
                        result.get(ID),
                        new ItemPath(uuid),
                        new AgentPath(agent),
                        (delegate == null ? null : new AgentPath(delegate)),
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
                        hasAttachment,
                        ts);
            }
            catch (Exception ex) {
                log.error("", ex);
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
        .column(STEP_PATH,            STRING_TYPE    .nullable(false))
        .column(STEP_TYPE,            NAME_TYPE      .nullable(true))
        .column(ORIGIN_STATE_ID,      ID_TYPE        .nullable(false))
        .column(TARGET_STATE_ID,      ID_TYPE        .nullable(false))
        .column(TRANSITION_ID,        ID_TYPE        .nullable(false))
        .column(VIEW_NAME,            NAME_TYPE      .nullable(true))
        .column(HAS_ATTACHMENT,       BOOLEAN        .nullable(false).defaultValue(false))
        .column(TIMESTAMP,            TIMESTAMP_TYPE .nullable(false))
        .constraints(constraint("PK_"+EVENT_TABLE).primaryKey(UUID, ID))
        .execute();
    }

    @Override
    public void dropTables(DSLContext context) throws PersistencyException {
        context.dropTableIfExists(EVENT_TABLE).execute();
    }

    public int getLastEventId(DSLContext context, UUID uuid){
        Field<Integer> maxID = max(ID);
        SelectConditionStep<Record1<Integer>> query = context.select(maxID).from(EVENT_TABLE).where(UUID.equal(uuid));
        Integer value = query.fetchOne(maxID);
        if(value == null){
            return -1;
        }
        return value;
    }
}
