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
import org.cristalise.kernel.property.BuiltInItemProperties;
import org.cristalise.kernel.property.Property;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertQuery;
import org.jooq.JoinType;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqItemPropertyHandler extends JooqHandler {
    static public final Table<?> ITEM_PROPERTY_TABLE = table(name("ITEM_PROPERTY"));

    static public final Field<UUID>    UUID    = field(name("UUID"),    UUID.class);
    static public final Field<String>  NAME    = field(name("NAME"),    String.class);
    static public final Field<String>  VALUE   = field(name("VALUE"),   String.class);
    static public final Field<Boolean> MUTABLE = field(name("MUTABLE"), Boolean.class);

    @Override
    protected Table<?> getTable() {
        return ITEM_PROPERTY_TABLE;
    }

    @Override
    protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException {
        if (primaryKeys.length == 0) return NAME;
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
                conditions.add(NAME.equal(primaryKeys[0]));
                break;
            default:
                throw new PersistencyException("Invalid number of primary keys:"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        return context
                .update(ITEM_PROPERTY_TABLE)
                .set(VALUE,   ((Property)obj).getValue())
                .set(MUTABLE, ((Property)obj).isMutable())
                .where(getPKConditions(uuid, obj))
                .execute();
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        return insert(context, uuid, new C2KLocalObject[] {obj});
    }

    public int insert(DSLContext context, UUID uuid, C2KLocalObject...objs) throws PersistencyException {
        InsertQuery<?> insertInto = context.insertQuery(ITEM_PROPERTY_TABLE);

        for (C2KLocalObject obj : objs) {
            insertInto.addValue(UUID,    uuid);
            insertInto.addValue(NAME,    obj.getName());
            insertInto.addValue(VALUE,   ((Property)obj).getValue());
            insertInto.addValue(MUTABLE, ((Property)obj).isMutable());

            insertInto.newRecord();
        }

        log.trace("insert() - SQL:\n"+insertInto);

        return insertInto.execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        Record result = fetchRecord(context, uuid, primaryKeys);

        //Reading MUTABLE boolean flag is done this way because of a bug in jooq supporting MySQL: check issue #23
        if(result != null) return new Property(result.get(NAME), result.get(VALUE), result.get(MUTABLE.getName(), Boolean.class));
        else               return null;
    }

    public List<UUID> findItemsByName(DSLContext context, String name) {
        return findItems(context, new Property(BuiltInItemProperties.NAME, name));
    }

    public List<UUID> findItems(DSLContext context, Property...properties) {
        return findItems(context, -1, -1, properties);
    }

    public List<UUID> findItems(DSLContext context, int offset, int limit, Property...properties) {
        log.trace("findItems() - properties:"+Arrays.toString(properties));

        SelectQuery<?> select = context.selectQuery();

        Field<UUID> firstJoinField = null;

        for (Property p : properties) {
            Field<UUID> currJoinField = field(name(p.getName(), "UUID"), UUID.class);

            if (firstJoinField == null) {
                select.addSelect(currJoinField);
                select.addFrom(ITEM_PROPERTY_TABLE.as(p.getName()));

                firstJoinField = currJoinField;
            }
            else {
                select.addJoin(ITEM_PROPERTY_TABLE.as(p.getName()), JoinType.LEFT_OUTER_JOIN, firstJoinField.equal(currJoinField));
            }

            Condition actualCondition =
                    field(name(p.getName(), "NAME"),  String.class).equal(p.getName())
                    .and(field(name(p.getName(), "VALUE"), String.class).equal(p.getValue()));

            select.addConditions(Operator.AND, actualCondition);
        }

        if (limit > 0) select.addLimit(limit);
        if (offset > 0) select.addOffset(offset);

        log.trace("findItems() - SQL:\n"+select);

        Result<?> result =  select.fetch();

        List<UUID> returnValue = new ArrayList<UUID>();

        for (Record record : result) returnValue.add(record.get(0, UUID.class));

        return returnValue;
    }

    @Override
    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(ITEM_PROPERTY_TABLE)
        .column(UUID,    UUID_TYPE.nullable(false))
        .column(NAME,    NAME_TYPE.nullable(false))
        .column(VALUE,   TEXT_TYPE.nullable(true))
        .column(MUTABLE, SQLDataType.BOOLEAN.nullable(false))
        .constraints(
                constraint("PK_"+ITEM_PROPERTY_TABLE.getName()).primaryKey(UUID, NAME))
        .execute();
    }

    @Override
    public void dropTables(DSLContext context) throws PersistencyException {
        context.dropTableIfExists(ITEM_PROPERTY_TABLE).execute();
    }
}
