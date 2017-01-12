package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.UUID;

import org.cristalise.kernel.property.Property;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;

public class JOOQItemProperty {
    public static final String tableName = "ITEM_PROPERTY";

    public int put(DSLContext context, UUID uuid, Property prop) {
        Property p = fetch(context, uuid, prop.getName());

        if (p == null) return insert(context, uuid, prop);
        else           return update(context, uuid, prop);
    }

    public int update(DSLContext context, UUID uuid, Property prop) {
        return context
                .update(table(tableName))
                .set(field("VALUE"),   prop.getValue())
                .set(field("MUTABLE"), prop.isMutable())
                .where(field("UUID").equal(uuid))
                  .and(field("NAME").equal(prop.getName()))
                .execute();
    }

    public int insert(DSLContext context, UUID uuid, Property prop) {
        return context
                .insertInto(
                    table(tableName), 
                        field("UUID"),
                        field("NAME"),
                        field("VALUE"),
                        field("MUTABLE")
                 )
                .values(uuid, prop.getName(), prop.getValue(), prop.isMutable())
                .execute();
    }

    public Property fetch(DSLContext context, UUID uuid, String name) {
        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
                  .and(field("NAME").equal(name))
                .fetchOne();

        if(result != null) return new Property(result.get(field("NAME",    String.class)),
                                               result.get(field("VALUE",   String.class)),
                                               result.get(field("MUTABLE", Boolean.class)));
        else               return null;
    }

    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",    UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field("NAME",    String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("VALUE",   String.class),  SQLDataType.VARCHAR.length(4000).nullable(true))
            .column(field("MUTABLE", Boolean.class), SQLDataType.BOOLEAN.nullable(false))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID"), field("NAME")))
        .execute();
    }
}
