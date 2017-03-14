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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;

public class JooqLifecycleHandler extends JooqHandler {
    static final Table<?> LIFECYCLE_TABLE = table(name("LIFECYCLE"));

    static final Field<UUID>   UUID = field(name("UUID"), UUID.class);
    static final Field<String> NAME = field(name("NAME"), String.class);
    static final Field<String> XML  = field(name("XML"),  String.class);

    @Override
    protected Table<?> getTable() {
        return LIFECYCLE_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        return null;
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
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys:"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        try {
            return context
                    .update(LIFECYCLE_TABLE)
                        .set(NAME, obj.getName())
                        .set(XML,  Gateway.getMarshaller().marshall(obj))
                        .where(UUID.equal(uuid))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        try {
            return context
                    .insertInto(LIFECYCLE_TABLE)
                        .set(UUID,  uuid)
                        .set(NAME,  obj.getName())
                        .set(XML,   Gateway.getMarshaller().marshall(obj))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Record result = fetchRecord(context, uuid, primaryKeys);

        if(result != null) {
            try {
                String xml = result.get(XML);
                return (C2KLocalObject)Gateway.getMarshaller().unmarshall(xml);
            }
            catch (MarshalException | ValidationException | IOException | MappingException e) {
                Logger.error(e);
                throw new PersistencyException(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        try {
            Record result = context
                    .select().from(LIFECYCLE_TABLE)
                    .where(getPKConditions(uuid, primaryKeys))
                    .fetchOne();

            if(result != null) { String[] keys = new String[1]; keys[0] = ClusterStorage.LIFECYCLE; return keys;}
            else               return null;
        }
        catch ( DataAccessException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(LIFECYCLE_TABLE)
            .column(UUID, UUID_TYPE.nullable(false))
            .column(NAME, NAME_TYPE.nullable(false))
            .column(XML,  XML_TYPE. nullable(false))
            .constraints(constraint("PK_"+LIFECYCLE_TABLE).primaryKey(UUID))
        .execute();
    }
}
