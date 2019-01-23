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
package jooqdb;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.SQLDataType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class JooqNameTest {
    static final String TABLE_NAME = "TEST";
    DSLContext context;

    @After
    public void after() {
        dropTable();
        context.close();
    }

    public void openH2() throws Exception {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);
    }

    public void openPostgres() throws Exception {
        String userName = "postgres";
        String password = "cristal";
        String url      = "jdbc:postgresql://localhost:5432/integtest";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);
    }

    public int createTable() {
        return context.createTableIfNotExists(table(name(TABLE_NAME)))
            .column(field(name("UUID"),  UUID.class),    SQLDataType.UUID.nullable(false))
            .column(field(name("NAME"),  String.class),  SQLDataType.VARCHAR.length(128).nullable(false))
            .column(field(name("VALUE"), String.class),  SQLDataType.VARCHAR.length(4096).nullable(true))
            .constraints(constraint("PK_"+TABLE_NAME).primaryKey(field(name("UUID")), field(name("NAME"))))
        .execute();
    }

    public int dropTable() {
        return context.dropTableIfExists(table(name(TABLE_NAME))).execute();
    }

    public int insert(UUID uuid, String name, String value) {
        return context
                .insertInto(table(name(TABLE_NAME)))
                    .set(field(name("UUID")),  uuid)
                    .set(field(name("NAME")),  name)
                    .set(field(name("VALUE")), value)
                .execute();
    }

    public String fetch(UUID uuid, String name) {
        Record result = context
                .select().from(table(name(TABLE_NAME)))
                .where(field(name("UUID")).equal(uuid))
                  .and(field(name("NAME")).equal(name))
                .fetchOne();

        if(result != null) return result.get(field(name("VALUE")), String.class);
        return null;
    }
    
    @Test
    public void testWithH2() throws Exception {
        openH2();
        testLogic();
    }

    @Test @Ignore("Postgres test cannot run in Travis")
    public void testWithPostgres() throws Exception {
        openPostgres();
        testLogic();
    }

    /**
     * 
     */
    private void testLogic() {
        UUID uuid = UUID.randomUUID();
        createTable();
        assert insert(uuid, "Type", "Serious") == 1;
        Assert.assertEquals("Serious", fetch(uuid, "Type"));

        assert insert(uuid, "NullValue", null) == 1;
        Assert.assertEquals(null, fetch(uuid, "NullValue"));
    }
}
