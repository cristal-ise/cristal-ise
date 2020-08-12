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

import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.storage.jooqdb.bindings.PostgreSqlXmlBinding;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import lombok.val;

@Ignore("Postgres test cannot run on Travis")
public class JooqSqlXmlTest {
    static final String TABLE_NAME = "TEST";
    DSLContext context;

    static DataType<Document> XMLTYPE = new DefaultDataType<Document>(POSTGRES, Document.class, "xml").asConvertedDataType(new PostgreSqlXmlBinding());

    static Table<Record>      TEST  = table(name(TABLE_NAME));
    static Field<UUID>        ID    = field(name("UUID"), UUID.class);
    static Field<Document>    XML   = field(name("XML"),  Document.class);

    @Before
    public void before() throws Exception {
        openPostgres();
        createTable();
    }

    @After
    public void after() {
        dropTable();
        context.close();
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
            .column(ID, SQLDataType.UUID.nullable(false))
            .column(XML, XMLTYPE.nullable(true))
            .constraints(constraint("PK_"+TABLE_NAME).primaryKey(ID))
        .execute();
    }

    public int dropTable() {
        return context.dropTableIfExists(TEST).execute();
    }

    public int set(UUID uuid, Document xml) {
        val insertQuery = context.insertQuery(TEST);

        insertQuery.addValue(ID, uuid);
        insertQuery.addValue(XML, xml);

        return insertQuery.execute();
    }

    public Document fetch(UUID uuid) throws Exception {
        Record result = context
                .select().from(TEST)
                .where(ID.equal(uuid))
                .fetchOne();

        return result.get(XML);
    }
    
    @Test
    public void testWithPostgres() throws Exception {
        UUID uuid = UUID.randomUUID();

        assert set(uuid, Outcome.parse("<Outcome/>")) == 1;
        assert fetch(uuid) != null;
    }
}
