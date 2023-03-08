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
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import lombok.val;

public class JooqOnDuplicateKeyUpdateTest {
    static final String TABLE_NAME = "TEST";
    DSLContext context;
    
    static Table<Record>         TEST  = table(name(TABLE_NAME));
    static Field<java.util.UUID> UUID  = field(name("UUID"), java.util.UUID.class);
    static Field<String>         NAME  = field(name("NAME"), String.class);
    static Field<String>         VALUE = field(name("VALUE"), String.class);

    @After
    public void after() {
        dropTable();
        context.close();
    }

    public void openH2() throws Exception {
        String userName = "sa";
        String password = "sa";
//        String url      = "jdbc:h2:mem:";  //this settings does not work
//        String url      = "jdbc:h2:mem:;MODE=PostgreSQL"; //this settings does not work
        String url      = "jdbc:h2:mem:;MODE=MYSQL";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);
    }

    public void openPostgres() throws Exception {
        String userName = "postgres";
        String password = "cristal";
        String url      = "jdbc:postgresql://localhost:5432/integtest";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.POSTGRES);
    }

    public int createTable() {
        return context.createTableIfNotExists(TEST)
            .column(UUID,  SQLDataType.UUID.nullable(false))
            .column(NAME,  SQLDataType.VARCHAR.length(128).nullable(false))
            .column(VALUE, SQLDataType.VARCHAR.length(4096).nullable(true))
            .constraints(constraint("PK_"+TABLE_NAME).primaryKey(UUID, NAME))
        .execute();
    }

    public int dropTable() {
        return context.dropTableIfExists(TEST).execute();
    }

    public int set(java.util.UUID uuid, String name, String value) {
        val insertQuery = context.insertQuery(TEST);

        insertQuery.addValue(UUID, uuid);
        insertQuery.addValue(NAME, name);
        insertQuery.addValue(VALUE, value);
        insertQuery.onDuplicateKeyUpdate(true);
        insertQuery.onConflict(UUID, NAME);
        insertQuery.addValueForUpdate(VALUE, value);

        System.out.println("-------\n" + insertQuery.toString()+"\n-------");

        return insertQuery.execute();
    }

    public String fetch(java.util.UUID uuid, String name) {
        Record result = context
                .select().from(TEST)
                .where(UUID.equal(uuid))
                  .and(NAME.equal(name))
                .fetchOne();

        if(result != null) return result.get(field(name("VALUE")), String.class);
        return null;
    }
    
    @Test
    public void testWithH2() throws Exception {
        openH2();
        testLogic();
    }

    @Test @Ignore("Postgres test cannot run on Travis")
    public void testWithPostgres() throws Exception {
        openPostgres();
        testLogic();
    }

    /**
     * 
     */
    private void testLogic() {
        java.util.UUID uuid = java.util.UUID.randomUUID();
        createTable();

        assertEquals(1, set(uuid, "Type", "Serious"));
        assertEquals("Serious", fetch(uuid, "Type"));

        assertEquals(1, set(uuid, "Type", "Ridiculous"));
        assertEquals("Ridiculous", fetch(uuid, "Type"));
    }
}
