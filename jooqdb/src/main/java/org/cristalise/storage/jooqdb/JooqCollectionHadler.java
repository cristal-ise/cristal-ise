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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

public class JooqCollectionHadler implements JooqHandler {
    public static final String tableName = "COLLECTION";

    private List<Condition> getPKConditions(UUID uuid, String... primaryKeys) {
        List<Condition> conditions = new ArrayList<>();

        switch (primaryKeys.length) {
            case 0: 
                conditions.add(field("UUID").equal(uuid));
                break;
            case 1:
                conditions.add(field("UUID").equal(uuid));
                conditions.add(field("NAME").equal(primaryKeys[0]));
                break;
            case 2:
                conditions.add(field("UUID"   ).equal(uuid));
                conditions.add(field("NAME"   ).equal(primaryKeys[0]));
                conditions.add(field("VERSION").equal(primaryKeys[1]));
                break;
            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String... primaryKeys) {
        Field<?>[] fields = new Field[1];

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        switch (primaryKeys.length) {
            case 0: 
                fields[0] = field("NAME");
                break;
            case 1:
                fields[0] = field("VERSION");
                break;
            case 2:
                fields[0] = field("VERSION");
                break;

            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
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
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Collection<?> collection = ((Collection<?>)obj);

        C2KLocalObject c = fetch(context, uuid, collection.getName(), collection.getVersionName());

        if (c == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Collection<?> collection = ((Collection<?>)obj);
        try {
            return context
                    .update(table(tableName))
                    .set(field("XML"),  Gateway.getMarshaller().marshall(obj))
                    .where(field("UUID",    UUID.class  ).equal(uuid))
                      .and(field("NAME",    String.class).equal(collection.getName()))
                      .and(field("VERSION", String.class).equal(collection.getVersionName()))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
        }
        return 0;
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Collection<?> collection = ((Collection<?>)obj);
        try {
            return context
                    .insertInto(
                        table(tableName), 
                            field("UUID",    UUID.class),
                            field("NAME",    String.class),
                            field("VERSION", String.class),
                            field("XML",     String.class)
                     )
                    .values(uuid, collection.getName(), collection.getVersionName(), Gateway.getMarshaller().marshall(obj))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
        }
        return 0;
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
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        Record result = context
                .select().from(table(tableName))
                .where(conditions)
                .fetchOne();

        if(result != null) {
            try {
                String xml = result.get(field("XML", String.class));
                return (C2KLocalObject)Gateway.getMarshaller().unmarshall(xml);
            }
            catch (MarshalException | ValidationException | IOException | MappingException e) {
                Logger.error(e);
            }
        }
        return null;
    }

    @Override
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",    UUID.class),   UUID_TYPE.nullable(false))
            .column(field("NAME",    String.class), NAME_TYPE.nullable(false))
            .column(field("VERSION", String.class), NAME_TYPE.nullable(false))
            .column(field("XML",     String.class), XML_TYPE.nullable(false))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID"), field("NAME"), field("VERSION")))
        .execute();
    }
}
