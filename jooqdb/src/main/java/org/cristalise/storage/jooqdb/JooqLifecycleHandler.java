package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.io.IOException;
import java.util.UUID;

import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;

public class JooqLifecycleHandler implements JooqHandler {
    public static final String tableName = "OUTCOME";

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        C2KLocalObject o = fetch(context, uuid);

        if (o == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) {
        try {
            return context
                    .update(table(tableName))
                    .set(field("NAME"), obj.getName())
                    .set(field("XML"),  Gateway.getMarshaller().marshall(obj))
                    .where(field("UUID").equal(uuid))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
        }
        return 0;
    }

    @Override
    public void delete(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        try {
            return context
                    .insertInto(
                        table(tableName), 
                            field("UUID", UUID.class),
                            field("NAME", String.class),
                            field("XML",  String.class)
                     )
                    .values(uuid, obj.getName(), Gateway.getMarshaller().marshall(obj))
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
        }
        return 0;
    }

    @Override
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) {
        Record result = context
                .select().from(table(tableName))
                .where(field("UUID").equal(uuid))
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
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) {
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
