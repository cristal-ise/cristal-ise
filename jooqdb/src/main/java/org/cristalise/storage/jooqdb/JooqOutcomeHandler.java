package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.UUID;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;

public class JooqOutcomeHandler implements JooqHandler {

    public static final String tableName = "OUTCOME";

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Outcome outcome = (Outcome)obj;

        String schemaName    = outcome.getSchema().getName();
        String schemaVersion = outcome.getSchema().getVersion().toString();
        String eventID       = outcome.getID().toString();

        C2KLocalObject o = fetch(context, uuid, schemaName, schemaVersion, eventID);

        if (o == null) return insert(context, uuid, outcome);
        else           return update(context, uuid, outcome);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject o) {
        throw new IllegalArgumentException("Outcome must not be updated");
    }

    @Override
    public C2KLocalObject delete(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Outcome outcome = (Outcome)obj;
        return context
                .insertInto(
                    table(tableName), 
                        field("UUID"),
                        field("SCHEMA_NAME"),
                        field("SCHEMA_VERSION"),
                        field("EVENT_ID"),
                        field("XML")
                 )
                .values(uuid, outcome.getSchema().getName(), outcome.getSchema().getVersion(), outcome.getID(), outcome.getData())
                .execute();
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) {
        String  schemaName    = primaryKeys[0];
        Integer schemaVersion = Integer.parseInt(primaryKeys[1]);
        Integer eventID       = Integer.parseInt(primaryKeys[2]);

        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
                  .and(field("SCHEMA_NAME").equal(schemaName))
                  .and(field("SCHEMA_VERSION").equal(schemaVersion))
                  .and(field("EVENT_ID").equal(eventID))
                .fetchOne();

        if(result != null) {
            try {
                String xml = result.get(field("XML", String.class));
                return new Outcome(eventID, xml, LocalObjectLoader.getSchema(schemaName, schemaVersion));
            }
            catch (InvalidDataException | ObjectNotFoundException e) {
                Logger.error(e);
            }
        }

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
            .column(field("UUID",           UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field("SCHEMA_NAME",    String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("SCHEMA_VERSION", Integer.class), SQLDataType.INTEGER.nullable(false))
            .column(field("EVENT_ID",       Integer.class), SQLDataType.INTEGER.nullable(false))
            .column(field("XML",            String.class), SQLDataType.CLOB.nullable(false))
            .constraints(constraint("PK_UUID").primaryKey(field("UUID"), 
                                                          field("SCHEMA_NAME"), 
                                                          field("SCHEMA_VERSION"), 
                                                          field("EVENT_ID")))
        .execute();
    }
}
