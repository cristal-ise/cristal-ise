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

/**
 * This class configures all jooqdb tests
 */
public class JooqTestConfigurationBase {
    
    /**
     * Specifies the supported DB configuration modes
     */
    public enum DBModes {
        /**
         * Run tests using in-memory H2 configured with PostgreSQL compatibility mode
         */
        H2_PostgreSQL,
        
        /**
         * Run tests using in-memory H2 configured with MYSQL compatibility mode
         */
        H2_MYSQL, 
        
        /**
         * Run tests using PostgreSQL integtest database
         */
        PostgreSQL,
        
        /**
         * Run tests using MYSQL integtest database
         */
        MYSQL,

        /**
         * Run tests using in-memory H2
         */
        H2
    }

    /**
     * Sets the database mode to run all jooqdb tests. For travis runs it should be set to H2
     */
    public static DBModes dbType = DBModes.H2;

    /**
     * Sets the database name used to run all jooqdb tests
     */
    public static String dbName = "integtest";

    /**
     * 
     * @return
     * @throws Exception
     */
    public static DSLContext initJooqContext() throws Exception {
        JooqHandler.readSystemProperties();

        switch (dbType) {
            case H2_PostgreSQL: return initH2Context("PostgreSQL");
            case H2_MYSQL:      return initH2Context("MYSQL");
            case H2:            return initH2Context(null);
            case PostgreSQL:    return initPostrgresContext();
            case MYSQL:         return initMySQLContext();
            default:            return initH2Context(null);
        }
    }

    /**
     * 
     * @param c2kProps
     */
    public static void setUpStorage(Properties c2kProps) {
        switch (dbType) {
            case H2_PostgreSQL: setUpH2(c2kProps, "PostgreSQL"); break;
            case H2_MYSQL:      setUpH2(c2kProps, "MYSQL");      break;
            case H2:            setUpH2(c2kProps, null);         break;
            case PostgreSQL:    setUpPostgres(c2kProps);         break;
            case MYSQL:         setUpMySQL(c2kProps);            break;
            default:            setUpH2(c2kProps, null);         break;
        }
    }

    /**
     * Use this if testing needs to be done with postgres. Make sure that 'integtest' database is created.
     * This is not the default setup because postgres cannot be run in travis as it has no in-memory/embedded mode.
     * 
     * @throws Exception throw anything that could happen
     */
    private static DSLContext initPostrgresContext() throws Exception {
        String userName = "postgres";
        String password = "cristal";
        String url      = "jdbc:postgresql://localhost:5432/" + dbName;

        Connection conn = DriverManager.getConnection(url, userName, password);
        return using(conn, SQLDialect.POSTGRES);
    }

    /**
     * 
     * @param c2kProps
     */
    private static void setUpPostgres(Properties c2kProps) {
        c2kProps.put(JooqHandler.JOOQ_URI,        "jdbc:postgresql://localhost:5432/" + dbName);
        c2kProps.put(JooqHandler.JOOQ_USER,       "postgres");
        c2kProps.put(JooqHandler.JOOQ_PASSWORD,   "cristal");
        c2kProps.put(JooqHandler.JOOQ_DIALECT,    SQLDialect.POSTGRES.toString());
        c2kProps.put(JooqHandler.JOOQ_AUTOCOMMIT, true);
        c2kProps.put(JooqHandler.JOOQ_USESQLXML,  true);
    }

    /**
     * Use this if testing needs to be done with mysql. Make sure that 'integtest' database is created.
     * This is not the default setup because mysql cannot be run in travis as it has no in-memory/embedded mode.
     * 
     * @throws Exception throw anything that could happen
     */
    private static DSLContext initMySQLContext() throws Exception {
        String userName = "root";
        String password = "cristal";
        String url      = "jdbc:mysql://localhost:3306/" + dbName;

        Connection conn = DriverManager.getConnection(url, userName, password);
        return using(conn, SQLDialect.MYSQL);
    }

    /**
     * 
     * @param c2kProps
     */
    private static void setUpMySQL(Properties c2kProps) {
        c2kProps.put(JooqHandler.JOOQ_URI,        "jdbc:mysql://localhost:3306/" + dbName);
        c2kProps.put(JooqHandler.JOOQ_USER,       "root");
        c2kProps.put(JooqHandler.JOOQ_PASSWORD,   "cristal");
        c2kProps.put(JooqHandler.JOOQ_DIALECT,    SQLDialect.MYSQL.toString());
        c2kProps.put(JooqHandler.JOOQ_AUTOCOMMIT, true);
        c2kProps.put(JooqHandler.JOOQ_USESQLXML,  false);
    }

    /**
     * 
     * @param mode
     * @return
     * @throws Exception
     */
    private static DSLContext initH2Context(String mode) throws Exception {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:"+dbName+";DB_CLOSE_DELAY=-1";

        if (mode != null) url += ";MODE=" + mode; 

        Connection conn = DriverManager.getConnection(url, userName, password);
        return using(conn, SQLDialect.H2);
    }

    /**
     * 
     * @param c2kProps
     * @param mode
     */
    private static void setUpH2(Properties c2kProps, String mode) {
        c2kProps.put(JooqHandler.JOOQ_URI,        "jdbc:h2:mem:"+dbName+";DB_CLOSE_DELAY=-1"+(mode!=null?";MODE="+mode:""));
        c2kProps.put(JooqHandler.JOOQ_USER,       "sa");
        c2kProps.put(JooqHandler.JOOQ_PASSWORD,   "sa");
        c2kProps.put(JooqHandler.JOOQ_DIALECT,    SQLDialect.H2.toString());
        c2kProps.put(JooqHandler.JOOQ_AUTOCOMMIT, true);
        c2kProps.put(JooqHandler.JOOQ_USESQLXML,  false);
    }
}
