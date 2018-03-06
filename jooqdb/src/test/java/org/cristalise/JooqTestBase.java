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
package org.cristalise;

import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

public class JooqTestBase {
    public enum H2Modes {PostgreSQL, MYSQL}

    
    /**
     * Use this if testing needs to be done with postgres. Make sure that 'integtest' database is created.
     * This is not the default setup because postgres cannot be run in travis as it has no in-memory/embedded mode.
     * 
     * @throws Exception throw anything that could happen
     */
    public static DSLContext initPostrgres() throws Exception {
        String userName = "postgres";
        String password = "cristal";
        String url      = "jdbc:postgresql://localhost:5432/integtest";

        Connection conn = DriverManager.getConnection(url, userName, password);
        return using(conn, SQLDialect.POSTGRES);
    }

    /**
     * 
     * @param c2kProps
     */
    public static void setUpPostgres(Properties c2kProps) {
        c2kProps.put(JooqHandler.JOOQ_URI,        "jdbc:postgresql://localhost:5432/integtest");
        c2kProps.put(JooqHandler.JOOQ_USER,       "postgres");
        c2kProps.put(JooqHandler.JOOQ_PASSWORD,   "cristal");
        c2kProps.put(JooqHandler.JOOQ_DIALECT,    "POSTGRES");
        c2kProps.put(JooqHandler.JOOQ_AUTOCOMMIT, true);
    }

    /**
     * Use this if testing needs to be done with mysql. Make sure that 'integtest' database is created.
     * This is not the default setup because mysql cannot be run in travis as it has no in-memory/embedded mode.
     * 
     * @throws Exception throw anything that could happen
     */
    public static DSLContext initMySQLContext() throws Exception {
        String userName = "root";
        String password = "cristal";
        String url      = "jdbc:mysql://localhost:3306/integtest";

        Connection conn = DriverManager.getConnection(url, userName, password);
        return using(conn, SQLDialect.MYSQL);
    }

    /**
     * 
     * @param c2kProps
     */
    public static void setUpMySQL(Properties c2kProps) {
        c2kProps.put(JooqHandler.JOOQ_URI,        "jdbc:mysql://localhost:3306/integtest");
        c2kProps.put(JooqHandler.JOOQ_USER,       "root");
        c2kProps.put(JooqHandler.JOOQ_PASSWORD,   "cristal");
        c2kProps.put(JooqHandler.JOOQ_DIALECT,    "MYSQL");
        c2kProps.put(JooqHandler.JOOQ_AUTOCOMMIT, true);
    }

    public static DSLContext initH2Context() throws Exception {
        return initH2Context(null);
    }

    /**
     * 
     * @param mode
     * @return
     * @throws Exception
     */
    public static DSLContext initH2Context(H2Modes mode) throws Exception {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        if (mode != null) url += ";MODE=" + mode.name(); 

        Connection conn = DriverManager.getConnection(url, userName, password);
        return using(conn, SQLDialect.H2);
    }

    /**
     * 
     * @param c2kProps
     */
    public static void setUpH2(Properties c2kProps) {
        setUpH2(c2kProps, null);
    }

    /**
     * 
     * @param c2kProps
     * @param mode
     */
    public static void setUpH2(Properties c2kProps, H2Modes mode) {
        c2kProps.put(JooqHandler.JOOQ_URI,      "jdbc:h2:mem:");
        c2kProps.put(JooqHandler.JOOQ_USER,     "sa");
        c2kProps.put(JooqHandler.JOOQ_PASSWORD, "sa");
        c2kProps.put(JooqHandler.JOOQ_DIALECT,  "H2");
        c2kProps.put(JooqHandler.JOOQ_AUTOCOMMIT, true);

        if (mode != null) c2kProps.put(JooqHandler.JOOQ_URI, "jdbc:h2:mem:;MODE=" + mode.name());

    }

}
