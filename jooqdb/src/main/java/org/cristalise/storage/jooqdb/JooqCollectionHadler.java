package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;

public class JooqCollectionHadler implements JooqHandler {
    public static final String tableName = "COLLECTION";

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
            case 2:
                conditions.add(field("UUID"   ).equal(uuid));
                conditions.add(field("NAME"   ).equal(primaryKeys[0]));
                conditions.add(field("VERSION").equal(primaryKeys[1]));
                break;
            default:
                throw new IllegalArgumentException("Invalid number of primary keys (max 2):"+Arrays.toString(primaryKeys));
        }
        return conditions;
    }

    @Override
    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String... primaryKeys) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) {
        Collection<?> collection = ((Collection<?>)obj);

        C2KLocalObject c = fetch(context, uuid, collection.getName(), collection.getVersionName());

        if (c == null) return insert(context, uuid, obj);
        else           return update(context, uuid, obj);
    }

    @Override
    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) {
        List<Condition> conditions = getPKConditions(uuid, obj.getClusterPath().split("/"));
        try {
            return context
                    .update(table(tableName))
                    .set(field("XML"),  Gateway.getMarshaller().marshall(obj))
                    .where(conditions)
                    .execute();
        }
        catch (MarshalException | ValidationException | DataAccessException | IOException | MappingException e) {
            Logger.error(e);
        }
        return 0;
    }

    @Override
    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) {
        // TODO Auto-generated method stub
        return 0;
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
    public C2KLocalObject fetch(DSLContext context, UUID uuid, String... primaryKeys) {
        List<Condition> conditions = getPKConditions(uuid, primaryKeys);

        Record result = context
                .select().from(table(tableName))
                .where(conditions)
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
    public void createTables(DSLContext context) {
        context.createTableIfNotExists(table(tableName))
            .column(field("UUID",    UUID.class),   UUID_TYPE.   nullable(false))
            .column(field("NAME",    String.class), NAME_TYPE.   nullable(false))
            .column(field("VERSION", String.class), VERSION_TYPE.nullable(false))
            .column(field("XML",     String.class), XML_TYPE.    nullable(false))
            .constraints(constraint("PK_UUID").primaryKey(field("UUID"), field("NAME"), field("VERSION")))
        .execute();
    }
}
