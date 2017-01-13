package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;

public class JooqViewpointHandler implements JooqHandler {
    public static final String tableName = "VIEWPOINT";

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        C2KLocalObject v = fetch(context, uuid, ((Viewpoint)obj).getSchemaName(), ((Viewpoint)obj).getName());

        if (v == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Viewpoint view = (Viewpoint)obj;
        return context
                .update(table(tableName))
                .set(field("SCHEMA_VERSION"), view.getSchemaVersion())
                .set(field("EVENT_ID"),       view.getEventId())
                .where(field("UUID").equal(uuid))
                  .and(field("SCHEMA_NAME").equal(view.getSchemaName()))
                  .and(field("NAME").equal(view.getName()))
                .execute();
    }

    @Override
    public C2KLocalObject delete(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Viewpoint view = (Viewpoint)obj;
        return context
                .insertInto(
                    table(tableName), 
                        field("UUID"),
                        field("SCHEMA_NAME"),
                        field("NAME"),
                        field("SCHEMA_VERSION"),
                        field("EVENT_ID")
                 )
                .values(uuid, view.getSchemaName(), view.getName(), view.getSchemaVersion(), view.getEventId())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) {
        String shcemaName = primaryKeys[0];
        String name       = primaryKeys[1];

        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
                  .and(field("SCHEMA_NAME").equal(shcemaName))
                  .and(field("NAME").equal(name))
                .fetchOne();

        if(result != null) return new Viewpoint(new ItemPath(uuid),
                                                result.get(field("SCHEMA_NAME",    String.class)),
                                                result.get(field("NAME",           String.class)),
                                                result.get(field("SCHEMA_VERSION", Integer.class)),
                                                result.get(field("EVENT_ID",       Integer.class)));
        else               return null;
    }

    @Override
    public String[] getClusterContent(DSLContext context, String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",           UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field("SCHEMA_NAME",    String.class),  SQLDataType.VARCHAR.length(50).nullable(true))
            .column(field("NAME",           String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("SCHEMA_VERSION", Integer.class), SQLDataType.INTEGER.nullable(true))
            .column(field("EVENT_ID",       Integer.class), SQLDataType.INTEGER.nullable(true))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID"), field("SCHEMA_NAME"), field("NAME")))
        .execute();
    }
}
