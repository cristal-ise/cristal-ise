package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.jooq.DSLContext;
import org.jooq.impl.SQLDataType;

public class JooqLifecycleHandler implements JooqHandler {
    public static final String tableName = "OUTCOME";

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public C2KLocalObject delete(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getClusterContent(DSLContext context, String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID", UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field("NAME", String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("XML",  String.class),  SQLDataType.CLOB.nullable(false))
            .constraints(constraint("PK_UUID").primaryKey(field("UUID")))
        .execute();
    }

}
