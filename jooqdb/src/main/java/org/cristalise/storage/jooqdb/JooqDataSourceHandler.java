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

import static org.jooq.SQLDialect.MYSQL;
import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.exception.ConfigurationException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqDataSourceHandler {

    private static final ConcurrentHashMap<Object, Connection> connectionMap  = new ConcurrentHashMap<Object, Connection>();

    /**
     * Defines the key (value:{@value}) to retrieve a string value to set JDBC URI
     */
    public static final String JOOQ_URI = "JOOQ.URI";
    /**
     * Defines the key (value:{@value}) to retrieve a string value to set the user
     */
    public static final String JOOQ_USER = "JOOQ.user";
    /**
     * Defines the key (value:{@value}) to retrieve a string value to set the password
     */
    public static final String JOOQ_PASSWORD = "JOOQ.password";
    /**
     * Defines the key (value:{@value}) to retrieve the database dialect. Default is 'POSTGRES'
     */
    public static final String JOOQ_DIALECT = "JOOQ.dialect";
    /**
     * Defines the key (value:{@value}) to retrieve a boolean value to set the JDBC connection 
     * with autocommit or not. Default is 'false'
     */
    public static final String JOOQ_AUTOCOMMIT = "JOOQ.autoCommit";
    /**
     * Defines the key (value:{@value}) to retrieve a boolean value to tell the data source to
     * create read only connections. Default is 'false'
     */
    public static final String JOOQ_READONLYDATASOURCE = "JOOQ.readOnlyDataSource";
    /**
     * Defines the key (value:{@value}) to retrieve a boolean value to setup JooqOutcomeHandler
     * to use SQLXML support of jdbc. Default is 'true'.
     */
    public static final String JOOQ_USESQLXML = "JOOQ.supportJdbcXmlType";

    /**
     * Defines the key (value:{@value}) to retrieve an integer value to set the maximumPoolSize
     */
    public static final String JOOQ_MAXIMUMPOOLSIZE = "JOOQ.maximumPoolSize";

    /**
     * Defines the key (value:{@value}) to retrieve a long value to set the maxLifetime
     */
    public static final String JOOQ_MAXLIFETIME = "JOOQ.maxLifetime";

    /**
     * Defines the key (value:{@value}) to retrieve an integer value to set the minimumIdle
     */
    public static final String JOOQ_MINIMUMIDLE = "JOOQ.minimumIdle";

    /**
     * Defines the key (value:{@value}) to retrieve a long value to set the idleTimeout
     */
    public static final String JOOQ_IDLETIMEOUT = "JOOQ.idleTimeout";

    /**
     * Defines the prefix key (value:{@value}) to retrieve String entries to add in DataSourceProperty
     */
    public static final String JOOQ_DATASOURCEPROPERTY = "JOOQ.DataSourceProperty.";

    public static String     uri;
    public static String     user;
    public static String     pwd ;
    public static Boolean    autoCommit;
    public static Boolean    readOnlyDataSource;
    public static SQLDialect dialect;
    public static Boolean    useSqlXml;
    public static int        maximumPoolSize;
    public static int        maxLifetime;
    public static int        minimumIdle;
    public static int        idleTimeout;

    public static Map<String, Object> dataSourceProperties;

    private static HikariDataSource ds = null;
    private static HikariConfig config;

    public static void readSystemProperties() {
        uri                    = Gateway.getProperties().getString(JOOQ_URI);
        user                   = Gateway.getProperties().getString(JOOQ_USER);
        pwd                    = Gateway.getProperties().getString(JOOQ_PASSWORD);
        autoCommit             = Gateway.getProperties().getBoolean(JOOQ_AUTOCOMMIT, false);
        readOnlyDataSource     = Gateway.getProperties().getBoolean(JOOQ_READONLYDATASOURCE, false);
        dialect                = SQLDialect.valueOf(Gateway.getProperties().getString(JOOQ_DIALECT, "POSTGRES"));
        maximumPoolSize        = Gateway.getProperties().getInt(JOOQ_MAXIMUMPOOLSIZE, 50);
        maxLifetime            = Gateway.getProperties().getInt(JOOQ_MAXLIFETIME, 60000);
        minimumIdle            = Gateway.getProperties().getInt(JOOQ_MINIMUMIDLE, 10);
        idleTimeout            = Gateway.getProperties().getInt(JOOQ_IDLETIMEOUT, 30000);
        dataSourceProperties   = Gateway.getProperties().entrySet()
                                  .stream().filter(e -> e.getKey().toString().startsWith(JOOQ_DATASOURCEPROPERTY))
                                  .collect(Collectors.toMap(e -> e.getKey().toString().trim().substring(JOOQ_DATASOURCEPROPERTY.length()), Entry::getValue));

        // setting default values for dataSourceProperties
        if (! dataSourceProperties.containsKey("cachePrepStmts"))        dataSourceProperties.put("cachePrepStmts", true);
        if (! dataSourceProperties.containsKey("prepStmtCacheSize"))     dataSourceProperties.put("prepStmtCacheSize", "250");
        if (! dataSourceProperties.containsKey("prepStmtCacheSqlLimit")) dataSourceProperties.put("prepStmtCacheSqlLimit", "2048");
        if (! dataSourceProperties.containsKey("autoCommit"))            dataSourceProperties.put("autoCommit", autoCommit);
    }

    public static synchronized HikariDataSource getDataSource() {
        if (ds == null) {
            if (StringUtils.isAnyBlank(uri, user, pwd)) {
                readSystemProperties();

                if (StringUtils.isAnyBlank(uri, user, pwd)) {
                    throw new IllegalArgumentException("JOOQ (uri, user, password) config values must not be blank");
                }
            }

            config = new HikariConfig();
            config.setPoolName("CRISTAL-iSE-HikariCP");
            config.setRegisterMbeans(true);
            config.setJdbcUrl(uri);
            config.setUsername(user);
            config.setPassword(pwd);
            config.setAutoCommit(autoCommit);
            config.setReadOnly(readOnlyDataSource);
            config.setMaximumPoolSize(maximumPoolSize);
            config.setMaxLifetime(maxLifetime);
            config.setMinimumIdle(minimumIdle);
            config.setIdleTimeout(idleTimeout);
            //config.setLeakDetectionThreshold(30000); // enable to see connection leak warning
            if (dataSourceProperties != null) {
                dataSourceProperties.forEach((k,v) -> config.addDataSourceProperty(k, v));
            }

            log.info("getDataSource() - uri:'{}' user:'{}' dialect:'{}'", uri, user, dialect);

            ds = new HikariDataSource(config);

            log.info("getDataSource() - create datasource {}", ds);
        }
        return ds;
    }

    public static synchronized void closeDataSource() throws PersistencyException {
        try {
            if (ds != null){
                HikariPoolMXBean poolBean = ds.getHikariPoolMXBean();
                log.debug("closeDataSource() - active connections={}", poolBean.getActiveConnections());

                while (poolBean.getActiveConnections() > 0) {
                    poolBean.softEvictConnections();
                }
                ds.close();
                ds = null;
            }
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException(e.getMessage());
        }
    }

    public static DSLContext connect() throws PersistencyException {
        try {
            return using(getDataSource(), dialect);
        }
        catch (Exception ex) {
            log.error("Could not connect to URI '"+uri+"' with user '"+user+"'", ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    public static DSLContext connect(Connection conn) throws PersistencyException {
        try {
            return using(conn);
        }
        catch (Exception ex) {
            log.error("Could not connect to URI '"+uri+"' with user '"+user+"'", ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    public static Connection createConnection(TransactionKey transactionKey) throws PersistencyException {
        if (!getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        log.debug("createConnection() - transactionKey:{}", transactionKey);

        if (transactionKey != null) {
            Connection conn = connect().configuration().connectionProvider().acquire();
            connectionMap.put(transactionKey, conn);
            return conn;
        }
        else {
            log.warn("createConnection() - called with a null transactionKey");
        }
        return null;
    }

    public static Connection removeConnection(TransactionKey transactionKey) throws PersistencyException {
        if (!getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        if (transactionKey == null) {
            log.warn("removeConnection() - Cannot retrieve connection because transactionKey is null");
            return null;
        }

        log.debug("removeConnection() - transactionKey:{}", transactionKey);

        return connectionMap.remove(transactionKey);
    }

    public static DSLContext retrieveContext(TransactionKey transactionKey) throws PersistencyException {
        log.trace("retrieveContext() - transactionKey:{}", transactionKey);

        if (getDataSource().isAutoCommit() || transactionKey == null) {
            return connect();
        }
        else {
            Connection conn = connectionMap.get(transactionKey);
            if (conn != null) return connect(conn);
        }

        String msg = "Could not find JDBC connection for transactionKey:"+transactionKey;
        log.error("retrieveContext() - {}", msg);
        throw new PersistencyException(msg);
    }

    public static void logConnectionCount(String text, DSLContext context) {
        if (context.dialect().equals(POSTGRES)) {
            Record rec = context.fetchOne("SELECT sum(numbackends) FROM pg_stat_database;");
            log.trace("{} ------- Number of POSTGRES connections:{}", text, rec.get(0, Integer.class));
        }
        else if (context.dialect().equals(MYSQL)) {
            Record rec = context.fetchOne("SHOW STATUS WHERE `variable_name` = 'Threads_connected';");
            log.trace("{} ------- Number of MSQL connections:{}", text, rec.get(1, String.class));
        }
        else {
            log.trace("{} ------- Printing number of connections not supported for dialect:{}", text, context.dialect());
        }
    }

    /**
     * Return the string DataType for the given dialect
     * 
     * @return the string DataType for the given dialect
     */
    public static DataType<String> getStringXmlType() {
        //There a bug in jooq supporting CLOB with MySQL: check issue #23
        if (dialect.equals(MYSQL)) return JooqHandler.XML_TYPE_MYSQL;
        else                       return JooqHandler.XML_TYPE;
    }

    public static boolean checkSqlXmlSupport() {
        switch (dialect) {
            case POSTGRES: 
                return true;
            default:
                throw new ConfigurationException("NativeXML is not supported for " + dialect);
        }
    }

}
