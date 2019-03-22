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
package org.cristalise.storage.jooqdb.lookup;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.table;

import java.util.ArrayList;
import java.util.List;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertQuery;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;

public class JooqPermissionHandler {
    static public final Table<?> ROLE_PERMISSION_TABLE = table(name("ROLE_PERMISSION"));

    static public final Field<String>  ROLE_PATH  = field(name("ROLE_PATH"),  String.class);
    static public final Field<String>  PERMISSION = field(name("PERMISSION"), String.class);

    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(ROLE_PERMISSION_TABLE)
        .column(ROLE_PATH,  JooqHandler.STRING_TYPE.nullable(false))
        .column(PERMISSION, JooqHandler.TEXT_TYPE  .nullable(true))
        .execute();
    }

    public boolean exists(DSLContext context, String role) {
        return context.fetchExists( select().from(ROLE_PERMISSION_TABLE).where(ROLE_PATH.equal(role)) );
    }

    public int delete(DSLContext context, String role) throws PersistencyException {
        return context.delete(ROLE_PERMISSION_TABLE).where(ROLE_PATH.equal(role)).execute();
    }

    public int insert(DSLContext context, String role, List<String> permissions) {
        InsertQuery<?> insertInto = context.insertQuery(ROLE_PERMISSION_TABLE);

        if (permissions.size() > 0) {
            for (String permission: permissions) {
                insertInto.addValue(ROLE_PATH, role);
                insertInto.addValue(PERMISSION, permission);
                insertInto.newRecord();
            }

            return insertInto.execute();
        }
        else 
            return 0;
    }

    public List<String> fetch(DSLContext context, String role) {
        Result<Record> result = context
                .select().from(ROLE_PERMISSION_TABLE)
                .where(ROLE_PATH.equal(role))
                .fetch();

        List<String> permissions = new ArrayList<>();

        if (result != null) {
            for (Record record : result) permissions.add(record.get(PERMISSION));
        }

        return permissions;
    }
}
