package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.Logger;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class JooqItemPropertyHandler implements JooqHandler {
    public static final String tableName = "ITEM_PROPERTY";

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
            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        C2KLocalObject p = fetch(context, uuid, obj.getName());

        if (p == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) {
        return context
                .update(table(tableName))
                .set(field("VALUE"),   ((Property)obj).getValue())
                .set(field("MUTABLE"), ((Property)obj).isMutable())
                .where(field("UUID").equal(uuid))
                  .and(field("NAME").equal(obj.getName()))
                .execute();
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
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        return context
                .insertInto(
                    table(tableName), 
                        field("UUID"),
                        field("NAME"),
                        field("VALUE"),
                        field("MUTABLE")
                 )
                .values(uuid, obj.getName(),  ((Property)obj).getValue(),  ((Property)obj).isMutable())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) {
        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
                  .and(field("NAME").equal(primaryKeys[0]))
                .fetchOne();

        if(result != null) return new Property(result.get(field("NAME",    String.class)),
                                               result.get(field("VALUE",   String.class)),
                                               result.get(field("MUTABLE", Boolean.class)));
        else               return null;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) {
        Field<?>[] fields = { field("NAME") };

        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

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
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",    UUID.class),    UUID_TYPE.nullable(false))
            .column(field("NAME",    String.class),  NAME_TYPE.nullable(false))
            .column(field("VALUE",   String.class),  STRING_TYPE.nullable(true))
            .column(field("MUTABLE", Boolean.class), SQLDataType.BOOLEAN.nullable(false))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID"), field("NAME")))
        .execute();
    }
}
