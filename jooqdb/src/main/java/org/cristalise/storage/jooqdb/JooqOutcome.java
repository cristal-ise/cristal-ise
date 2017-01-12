package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.util.UUID;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.SQLDataType;

public class JooqOutcome {
    public static final String tableName = "OUTCOME";

    public int put(DSLContext context, UUID uuid, Outcome outcome) {
        Outcome o = fetch(context, uuid, outcome.getSchema().getName(), outcome.getSchema().getVersion(), outcome.getID());

        if (o == null) return insert(context, uuid, outcome);
        else           return update(context, uuid, outcome);
    }

    public int update(DSLContext context, UUID uuid, Outcome o) {
        throw new IllegalArgumentException("Outcome must not be updated");
    }

    public int insert(DSLContext context, UUID uuid, Outcome outcome) {
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

    public Outcome fetch(DSLContext context, UUID uuid, String schemaName, Integer schemaVersion, Integer eventID) {
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
                return new Outcome(eventID, xml, null);
            }
            catch (InvalidDataException e) {
                Logger.error(e);
            }
        }

        return null;
    }

    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",           UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field("SCHEMA_NAME",    String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("SCHEMA_VERSION", String.class),  SQLDataType.VARCHAR.length(50).nullable(false))
            .column(field("EVENT_ID",       Integer.class), SQLDataType.INTEGER.nullable(false))
            .column(field("XML",            String.class),  SQLDataType.CLOB.nullable(false))
            .constraints(constraint("PK_UUID").primaryKey(field("UUID"), 
                                                          field("SCHEMA_NAME"), 
                                                          field("SCHEMA_VERSION"), 
                                                          field("EVENT_ID")))
        .execute();
    }
}
