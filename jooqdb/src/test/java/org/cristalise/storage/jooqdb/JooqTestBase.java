package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;

public class JooqTestBase {
    DSLContext context;
    UUID       uuid = UUID.randomUUID();

    public void before() throws Exception {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);
    }
}
