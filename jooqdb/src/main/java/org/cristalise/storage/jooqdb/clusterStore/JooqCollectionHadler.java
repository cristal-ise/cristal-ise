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

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqCollectionHadler extends JooqHandler {
    static final Table<?> COLLECTION_TABLE = table(name("COLLECTION"));

    static final Field<UUID>   UUID    = field(name("UUID"),    UUID.class);
    static final Field<String> NAME    = field(name("NAME"),    String.class);
    static final Field<String> VERSION = field(name("VERSION"), String.class);
    static final Field<String> XML     = field(name("XML"),     String.class);

    @Override
    protected Table<?> getTable() {
        return COLLECTION_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        switch (primaryKeys.length) {
            case 0: return NAME;
            case 1: return VERSION;
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
                conditions.add(NAME.equal(primaryKeys[0]));
                break;
            case 2:
                conditions.add(UUID   .equal(uuid));
                conditions.add(NAME   .equal(primaryKeys[0]));
                conditions.add(VERSION.equal(primaryKeys[1]));
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        try {
            return context
                    .update(COLLECTION_TABLE)
                    .set(XML, Gateway.getMarshaller().marshall(obj))
                    .where(getPKConditions(uuid, obj))
                    .execute();
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Collection<?> collection = ((Collection<?>)obj);
        try {
            return context
                    .insertInto(COLLECTION_TABLE)
                    .set(UUID,    uuid)
                    .set(NAME,    collection.getName())
                    .set(VERSION, collection.getVersionName())
                    .set(XML,     Gateway.getMarshaller().marshall(obj))
                    .execute();
        }
        catch (Exception e) {
            log.error("", e);
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
            catch (Exception e) {
                log.error("", e);
                throw new PersistencyException(e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void createTables(DSLContext context) throws PersistencyException {
        DataType<String> xmlType = getStringXmlType();

        context.createTableIfNotExists(COLLECTION_TABLE)
        .column(UUID,    UUID_TYPE.nullable(false))
        .column(NAME,    NAME_TYPE.nullable(false))
        .column(VERSION, NAME_TYPE.nullable(false))
        .column(XML,     xmlType  .nullable(false))
        .constraints(
                constraint("PK_"+COLLECTION_TABLE).primaryKey(UUID, NAME, VERSION))
        .execute();
    }

    @Override
    public void dropTables(DSLContext context) throws PersistencyException {
        context.dropTableIfExists(COLLECTION_TABLE).execute();
    }
}
