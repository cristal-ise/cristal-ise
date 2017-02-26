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

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
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
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;

public class JooqCollectionHadler implements JooqHandler {
    static final Table<?> COLLECTION_TABLE = table(name("COLLECTION"));

    static final Field<UUID>   UUID    = field(name("UUID"),    UUID.class);
    static final Field<String> NAME    = field(name("NAME"),    String.class);
    static final Field<String> VERSION = field(name("VERSION"), String.class);
    static final Field<String> XML     = field(name("XML"),     String.class);

    private List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException {
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
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Field<?>[] fields = new Field[1];

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        switch (primaryKeys.length) {
            case 0: 
                fields[0] = NAME;
                break;
            case 1:
                fields[0] = VERSION;
                break;
            case 2:
                fields[0] = VERSION;
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }

        Result<Record> result = context
                .selectDistinct(fields)
                .from(COLLECTION_TABLE)
                .where(conditions)
                .fetch();

        String[] returnValue = new String[result.size()];

        int i = 0;
        for (Record rec : result) returnValue[i++] = rec.get(0).toString();

        return returnValue;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Collection<?> collection = ((Collection<?>)obj);

        C2KLocalObject c = fetch(context, uuid, collection.getName(), collection.getVersionName());

        if (c == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        Collection<?> collection = ((Collection<?>)obj);
        try {
            return context
                    .update(COLLECTION_TABLE)
                    .set(XML, Gateway.getMarshaller().marshall(obj))
                    .where(UUID   .equal(uuid))
                      .and(NAME   .equal(collection.getName()))
                      .and(VERSION.equal(collection.getVersionName()))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
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
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(COLLECTION_TABLE)
                .where(conditions)
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        Record result = context
                .select().from(COLLECTION_TABLE)
                .where(conditions)
                .fetchOne();

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
    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(COLLECTION_TABLE)
            .column(UUID,    UUID_TYPE.nullable(false))
            .column(NAME,    NAME_TYPE.nullable(false))
            .column(VERSION, NAME_TYPE.nullable(false))
            .column(XML,     XML_TYPE.nullable(false))
            .constraints(
                    constraint("PK_"+COLLECTION_TABLE).primaryKey(UUID, NAME, VERSION))
        .execute();
  }
}
