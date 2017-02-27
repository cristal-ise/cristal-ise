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
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.property.BuiltInItemProperties;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

public class JooqItemPropertyHandler implements JooqHandler {
    static final Table<?> ITEM_PROPERTY_TABLE = table(name("ITEM_PROPERTY"));

    static final Field<UUID>    UUID    = field(name("UUID"),    UUID.class);
    static final Field<String>  NAME    = field(name("NAME"),    String.class);
    static final Field<String>  VALUE   = field(name("VALUE"),   String.class);
    static final Field<Boolean> MUTABLE = field(name("MUTABLE"), Boolean.class);

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
            default:
                throw new PersistencyException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        C2KLocalObject p = fetch(context, uuid, obj.getName());

        if (p == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        return context
                .update(ITEM_PROPERTY_TABLE)
                .set(VALUE,   ((Property)obj).getValue())
                .set(MUTABLE, ((Property)obj).isMutable())
                .where(UUID.equal(uuid))
                  .and(NAME.equal(obj.getName()))
                .execute();
    }

    @Override
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);
        return context
                .delete(ITEM_PROPERTY_TABLE)
                .where(conditions)
                .execute();
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        return context
                .insertInto(ITEM_PROPERTY_TABLE)
                    .set(UUID,    uuid)
                    .set(NAME,    obj.getName())
                    .set(VALUE,   ((Property)obj).getValue())
                    .set(MUTABLE, ((Property)obj).isMutable())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        Record result = context
                .select().from(ITEM_PROPERTY_TABLE)
                .where(UUID.equal(uuid))
                  .and(NAME.equal(primaryKeys[0]))
                .fetchOne();

        if(result != null) return new Property(result.get(NAME), result.get(VALUE), result.get(MUTABLE));
        else               return null;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        Field<?>[] fields = { NAME };

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        Result<Record> result = context
                .selectDistinct(fields)
                .from(ITEM_PROPERTY_TABLE)
                .where(conditions)
                .fetch();

        String[] returnValue = new String[result.size()];

        int i = 0;
        for (Record rec : result) returnValue[i++] = rec.get(0).toString();

        return returnValue;
    }

    public List<UUID> findItemsByName(DSLContext context, String name) {
        return findItems(context, new Property(BuiltInItemProperties.NAME, name));
    }

    public List<UUID> findItems(DSLContext context, Property...properties) {
        Logger.msg(5, "JooqItemPropertyHandler.findItems() - properties:"+Arrays.toString(properties));

        SelectQuery<?> select = context.selectQuery(ITEM_PROPERTY_TABLE);

        for (Property p : properties) {
            Condition actualCondition = NAME.equal(p.getName()) .and (VALUE.equal(p.getValue()));
            select.addConditions(Operator.OR, actualCondition);
        }

        Logger.msg(8, "JooqItemPropertyHandler.findItems() - SQL:"+select);

        Result<?> result =  select.fetch();

        Set<UUID> returnValue = new TreeSet<UUID>();

        for (Record record : result) returnValue.add(record.get(UUID));

        return new ArrayList<UUID>(returnValue);
    }

    @Override
    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(ITEM_PROPERTY_TABLE)
            .column(UUID,    UUID_TYPE.nullable(false))
            .column(NAME,    NAME_TYPE.nullable(false))
            .column(VALUE,   STRING_TYPE.nullable(true))
            .column(MUTABLE, SQLDataType.BOOLEAN.nullable(false))
            .constraints(
                    constraint("PK_"+ITEM_PROPERTY_TABLE).primaryKey(UUID, NAME))
        .execute();
    }

}
