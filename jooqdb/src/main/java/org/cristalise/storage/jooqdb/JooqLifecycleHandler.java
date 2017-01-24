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
    public int delete(DSLContext context, UUID uuid, String... primaryKeys) {
        return context
                .delete(table(tableName))
                .where(field("UUID").equal(uuid))
                .execute();
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
            .column(field("UUID", UUID.class),    UUID_TYPE.nullable(false))
            .column(field("NAME", String.class),  NAME_TYPE.nullable(false))
            .column(field("XML",  String.class),  XML_TYPE. nullable(false))
            .constraints(constraint("PK_"+tableName).primaryKey(field("UUID")))
        .execute();
    }

}
