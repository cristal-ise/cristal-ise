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
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.storage.jooqdb.JooqDataSourceHandler;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqJobHandler extends JooqHandler {
    static final Table<?> JOB_TABLE = table(name("JOB"));

    static final Field<UUID>      UUID              = field(name("UUID"),              UUID.class);
    static final Field<String>    STEP_NAME         = field(name("STEP_NAME"),         String.class);
    static final Field<String>    STEP_PATH         = field(name("STEP_PATH"),         String.class);
    static final Field<String>    STEP_TYPE         = field(name("STEP_TYPE"),         String.class);
    static final Field<String>    TRANSITION        = field(name("TRANSITION"),        String.class);
    static final Field<String>    ROLE_OVERRIDE     = field(name("ROLE_OVERRIDE"),     String.class);
    static final Field<String>    ACT_PROPERTIES    = field(name("ACT_PROPERTIES"),    String.class);

    //static final Field<OffsetDateTime> CREATION_TS = field(name("CREATION_TS"), OffsetDateTime.class);

    @Override
    protected Table<?> getTable() {
        return JOB_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        switch (primaryKeys.length) {
            case 0: return STEP_NAME;
            case 1: return TRANSITION;
            case 2: return null;
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
                conditions.add(UUID.equal(uuid));
                conditions.add(STEP_NAME.equal(primaryKeys[0]));
                break;
            case 2:
                conditions.add(UUID.equal(uuid));
                conditions.add(STEP_NAME.equal(primaryKeys[0]));
                conditions.add(TRANSITION.equal(primaryKeys[1]));
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

        String actPropsXML;
        try {
            actPropsXML = Gateway.getMarshaller().marshall(job.getActProps());
        }
        catch (Exception ex) {
            log.error("insert()", ex);
            throw new PersistencyException(ex.getMessage());
        }

        return context
                .insertInto(JOB_TABLE)
                .set(UUID,              uuid)
                .set(STEP_NAME,         job.getStepName())
                .set(STEP_PATH,         job.getStepPath())
                .set(STEP_TYPE,         job.getStepType())
                .set(TRANSITION,        job.getTransitionName())
                .set(ROLE_OVERRIDE,     job.getRoleOverride())
                .set(ACT_PROPERTIES,    actPropsXML)
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Record result = fetchRecord(context, uuid, primaryKeys);

        if (result != null) {
            try {
                CastorHashMap actProps = (CastorHashMap)Gateway.getMarshaller().unmarshall(result.get(ACT_PROPERTIES));

                return new Job(
                        new ItemPath(uuid),
                        result.get(STEP_NAME),
                        result.get(STEP_PATH),
                        result.get(STEP_TYPE),
                        result.get(TRANSITION),
                        result.get(ROLE_OVERRIDE),
                        actProps);
            }
            catch (Exception ex) {
                log.error("fetch()", ex);
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
        .column(STEP_NAME,          NAME_TYPE     .nullable(false))
        .column(STEP_PATH,          STRING_TYPE   .nullable(false))
        .column(STEP_TYPE,          NAME_TYPE     .nullable(false))
        .column(TRANSITION,         STRING_TYPE   .nullable(false))
        .column(ROLE_OVERRIDE,      NAME_TYPE     .nullable(true))
        .column(ACT_PROPERTIES,     xmlType       .nullable(false))
        .constraints(constraint("PK_"+JOB_TABLE).primaryKey(UUID, STEP_NAME, TRANSITION))
        .execute();
    }

    @Override
    public void dropTables(DSLContext context) throws PersistencyException {
        context.dropTableIfExists(JOB_TABLE).execute();
    }
}
