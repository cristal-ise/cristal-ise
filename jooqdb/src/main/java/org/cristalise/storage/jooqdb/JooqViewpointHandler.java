package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.utils.Logger;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
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
                .set(field("SCHEMA_VERSION", Integer.class), view.getSchemaVersion())
                .set(field("EVENT_ID",       Integer.class), view.getEventId())
                .where(field("UUID",        UUID.class  ).equal(uuid))
                  .and(field("SCHEMA_NAME", String.class).equal(view.getSchemaName()))
                  .and(field("NAME",        String.class).equal(view.getName()))
                .execute();
    }

    @Override
    public void delete(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Viewpoint view = (Viewpoint)obj;
        return context
                .insertInto(
                    table(tableName), 
                        field("UUID",           UUID.class),
                        field("SCHEMA_NAME",    String.class),
                        field("NAME",           String.class),
                        field("SCHEMA_VERSION", Integer.class),
                        field("EVENT_ID",       Integer.class)
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
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) {
        Field<?>[] fields = new Field[1];

        List<Condition> conditions = new ArrayList<>();
        conditions.add(field("UUID").equal(uuid));

        switch (primaryKeys.length) {
            case 0: 
                fields[0] = field("SCHEMA_NAME");
                break;
            case 1:
                fields[0] = field("NAME");
                conditions.add(field("SCHEMA_NAME").equal(primaryKeys[0]));
                break;
            case 2:
                fields[0] = field("NAME");
                conditions.add(field("SCHEMA_NAME").equal(primaryKeys[0]));
                conditions.add(field("NAME"       ).equal(primaryKeys[1]));
                break;

            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }

//        Logger.msg(DSL.selectDistinct(fields).from(table(tableName)).where(conditions).getSQL());

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
            .column(field("UUID",           UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field("SCHEMA_NAME",    String.class),  SQLDataType.VARCHAR.length(50).nullable(true))
            .column(field("NAME",           String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("SCHEMA_VERSION", Integer.class), SQLDataType.INTEGER.nullable(true))
            .column(field("EVENT_ID",       Integer.class), SQLDataType.INTEGER.nullable(true))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID"), field("SCHEMA_NAME"), field("NAME")))
        .execute();
    }
}
