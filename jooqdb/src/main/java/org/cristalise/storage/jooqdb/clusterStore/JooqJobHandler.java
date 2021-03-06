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
import static org.jooq.impl.DSL.max;
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
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.storage.jooqdb.JooqDataSourceHandler;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqJobHandler extends JooqHandler {
    static final Table<?> JOB_TABLE = table(name("JOB"));

    static final Field<UUID>      UUID              = field(name("UUID"),              UUID.class);
    static final Field<Integer>   ID                = field(name("ID"),                Integer.class);
//    static final Field<UUID>      DELEGATE_UUID     = field(name("DELEGATE_UUID"),     UUID.class);
    static final Field<UUID>      ITEM_UUID         = field(name("ITEM_UUID"),         UUID.class);
    static final Field<String>    STEP_NAME         = field(name("STEP_NAME"),         String.class);
    static final Field<String>    STEP_PATH         = field(name("STEP_PATH"),         String.class);
    static final Field<String>    STEP_TYPE         = field(name("STEP_TYPE"),         String.class);
    static final Field<String>    TRANSITION        = field(name("TRANSITION"),        String.class);
    static final Field<String>    ORIGIN_STATE_NAME = field(name("ORIGIN_STATE_NAME"), String.class);
    static final Field<String>    TARGET_STATE_NAME = field(name("TARGET_STATE_NAME"), String.class);
    static final Field<String>    AGENT_ROLE        = field(name("AGENT_ROLE"),        String.class);
    static final Field<String>    ACT_PROPERTIES    = field(name("ACT_PROPERTIES"),    String.class);
    static final Field<Timestamp> CREATION_TS       = field(name("CREATION_TS"),       Timestamp.class);

    //static final Field<OffsetDateTime> CREATION_TS = field(name("CREATION_TS"), OffsetDateTime.class);

    @Override
    protected Table<?> getTable() {
        return JOB_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        if (primaryKeys.length == 0) return ID;
        else                         return null;
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
                conditions.add(ID  .equal(Integer.valueOf(primaryKeys[0])));
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys:"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        throw new PersistencyException("Job must not be updated - uuid:"+uuid+" id:"+obj.getName());
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Job job = (Job)obj;

        String transXML, actPropsXML;
        try {
            transXML    = Gateway.getMarshaller().marshall(job.getTransition());
            actPropsXML = Gateway.getMarshaller().marshall(job.getActProps());
        }
        catch (Exception ex) {
            log.error("", ex);
            throw new PersistencyException(ex.getMessage());
        }

        return context
                .insertInto(JOB_TABLE)
                .set(UUID,              uuid)
                .set(ID,                job.getId())
                .set(ITEM_UUID,         job.getItemPath().getUUID())
                .set(STEP_NAME,         job.getStepName())
                .set(STEP_PATH,         job.getStepPath())
                .set(STEP_TYPE,         job.getStepType())
                .set(TRANSITION,        transXML)
                .set(ORIGIN_STATE_NAME, job.getOriginStateName())
                .set(TARGET_STATE_NAME, job.getTargetStateName())
                .set(AGENT_ROLE,        job.getAgentRole())
                .set(ACT_PROPERTIES,    actPropsXML)
                .set(CREATION_TS,       DateUtility.toSqlTimestamp(job.getCreationDate()))
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Record result = fetchRecord(context, uuid, primaryKeys);

        if (result != null) {
            try {
                Transition trans       = (Transition)   Gateway.getMarshaller().unmarshall(result.get(TRANSITION));
                CastorHashMap actProps = (CastorHashMap)Gateway.getMarshaller().unmarshall(result.get(ACT_PROPERTIES));

                GTimeStamp ts = DateUtility.fromSqlTimestamp( result.get(CREATION_TS));
                //GTimeStamp ts = DateUtility.fromOffsetDateTime( result.get(CREATION_TS));

                return new Job(
                        result.get(ID),
                        new ItemPath(getUUID(result, ITEM_UUID)),
                        result.get(STEP_NAME),
                        result.get(STEP_PATH),
                        result.get(STEP_TYPE),
                        trans,
                        result.get(ORIGIN_STATE_NAME),
                        result.get(TARGET_STATE_NAME),
                        result.get(AGENT_ROLE),
                        new AgentPath(getUUID(result, UUID)),
                        actProps,
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
        DataType<String> xmlType = JooqDataSourceHandler.getStringXmlType();

        context.createTableIfNotExists(JOB_TABLE)
        .column(UUID,               UUID_TYPE     .nullable(false))
        .column(ID,                 ID_TYPE       .nullable(false))
//        .column(DELEGATE_UUID,      UUID_TYPE     .nullable(true))
        .column(ITEM_UUID,          UUID_TYPE     .nullable(false))
        .column(STEP_NAME,          NAME_TYPE     .nullable(false))
        .column(STEP_PATH,          STRING_TYPE   .nullable(false))
        .column(STEP_TYPE,          NAME_TYPE     .nullable(false))
        .column(TRANSITION,         xmlType       .nullable(false))
        .column(ORIGIN_STATE_NAME,  NAME_TYPE     .nullable(false))
        .column(TARGET_STATE_NAME,  NAME_TYPE     .nullable(false))
        .column(AGENT_ROLE,         NAME_TYPE     .nullable(false))
        .column(ACT_PROPERTIES,     xmlType       .nullable(false))
        .column(CREATION_TS,        TIMESTAMP_TYPE.nullable(false))
        .constraints(constraint("PK_"+JOB_TABLE).primaryKey(UUID, ID))
        .execute();
    }

    @Override
    public void dropTables(DSLContext context) throws PersistencyException {
        context.dropTableIfExists(JOB_TABLE).execute();
    }

    public int getLastJobId(DSLContext context, UUID uuid) {
        Field<Integer> maxID = max(ID);
        SelectConditionStep<Record1<Integer>> query = context.select(maxID).from(JOB_TABLE).where(UUID.equal(uuid));
        Integer value = query.fetchOne(maxID);
        if (value == null) {
            return -1;
        }
        return value;
    }
}
