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
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.using;

import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.process.Gateway;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Slf4j
public abstract class JooqHandler {
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
     * Defines the key (value:{@value}) to retrieve the string value of the comma separated list of 
     * fully qualified class names implementing the {@link JooqDomainHandler} interface.
     */
    public static final String JOOQ_DOMAIN_HANDLERS = "JOOQ.domainHandlers";
    /**
     * Defines the key (value:{@value}) to retrieve the boolean value to disable the invocation of
     * {@link JooqDomainHandler#createTables(DSLContext)}. Default is 'false'
     */
    public static final String JOOQ_DISABLE_DOMAIN_CREATE = "JOOQ.disableDomainCreateTables";
    /**
     * Defines the key (value:{@value}) to retrieve the integer value for VARCHAR size of NAME_TYPE type declaration
     * {@link JooqDomainHandler#createTables(DSLContext)}. Default is '64'
     * 
     * <p>It is used for these columns:
     * <pre>
     *   - COLLECTION.NAME
     *   - EVENT.AGENT_ROLE
     *   - EVENT.SCHEMA_NAME
     *   - EVENT.STATEMACHINE_NAME
     *   - EVENT.STEP_NAME
     *   - EVENT.STEP_TYPE
     *   - EVENT.VIEW_NAME
     *   - ITEM_PROPERTY.NAME
     *   - JOB.STEP_NAME
     *   - JOB.STEP_TYPE
     *   - JOB.ORIGIN_STATE_NAME
     *   - JOB.TARGET_STATE_NAME
     *   - JOB.AGENT_ROLE
     *   - LIFECYCLE.NAME
     *   - OUTCOME.SCHEMA_NAME
     *   - OUTCOME_ATTACHMENT.SCHEMA_NAME
     *   - VIEWPOINT.SCHEMA_NAME
     * </pre>
     */
    public static final String JOOQ_NAME_TYPE_LENGHT = "JOOQ.NameType.length";
    /**
     * Defines the key (value:{@value}) to retrieve the integer value for VARCHAR size of PASSWORD_TYPE type declaration
     * {@link JooqDomainHandler#createTables(DSLContext)}. Default is '800'
     * 
     * <p>It is used for these columns:
     * <pre>
     *   - ITEM.PASSWORD
     * </pre>
     */
    public static final String JOOQ_PASSWORD_TYPE_LENGHT = "JOOQ.PasswordType.length";
    /**
     * Defines the key (value:{@value}) to retrieve the integer value for VARCHAR size of STRING_TYPE type declaration
     * {@link JooqDomainHandler#createTables(DSLContext)}. Default is '800'
     * 
     * <p>It is used for these columns:
     * <pre>
     *   - EVENT.STEP_PATH
     *   - JOB.STEP_PATH
     *   - DOMAIN_PATH.PATH
     *   - ROLE_PATH.PATH
     *   - ROLE_PERMISSION.ROLE_PATH
     * </pre>
     */
    public static final String JOOQ_STRING_TYPE_LENGHT = "JOOQ.StringType.length";
    /**
     * Defines the key (value:{@value}) to retrieve the integer value for VARCHAR size of TEXT_TYPE type declaration
     * {@link JooqDomainHandler#createTables(DSLContext)}. Default is '800'
     * 
     * <p>It is used for these columns: 
     * <pre>
     *   - ITEM_PROPERTY.VALUE
     *   - ROLE_PERMISSION.PERMISSION
     * </pre>
     */
    public static final String JOOQ_TEXT_TYPE_LENGHT = "JOOQ.TextType.length";

    public static final DataType<UUID>           UUID_TYPE       = SQLDataType.UUID;

    public static final DataType<String>         NAME_TYPE       = SQLDataType.VARCHAR.length(Gateway.getProperties().getInt(JOOQ_NAME_TYPE_LENGHT, 64));
    public static final DataType<Integer>        VERSION_TYPE    = SQLDataType.INTEGER;
    public static final DataType<String>         PASSWORD_TYPE   = SQLDataType.VARCHAR.length(Gateway.getProperties().getInt(JOOQ_PASSWORD_TYPE_LENGHT, 800));
    public static final DataType<String>         STRING_TYPE     = SQLDataType.VARCHAR.length(Gateway.getProperties().getInt(JOOQ_STRING_TYPE_LENGHT, 800));
    public static final DataType<String>         TEXT_TYPE       = SQLDataType.VARCHAR.length(Gateway.getProperties().getInt(JOOQ_TEXT_TYPE_LENGHT, 800));
    public static final DataType<String>         IOR_TYPE        = SQLDataType.VARCHAR.length(800);
    public static final DataType<Integer>        ID_TYPE         = SQLDataType.INTEGER;
    public static final DataType<Timestamp>      TIMESTAMP_TYPE  = SQLDataType.TIMESTAMP;
//  public static final DataType<OffsetDateTime> TIMESTAMP_TYPE  = SQLDataType.TIMESTAMPWITHTIMEZONE;
    public static final DataType<String>         XML_TYPE        = SQLDataType.CLOB;
    // Use this declaration when generating MySQL tables: see issue #23
    public static final DataType<String>         XML_TYPE_MYSQL  = new DefaultDataType<String>(SQLDialect.MYSQL, SQLDataType.CLOB, "mediumtext", "char");
    public static final DataType<byte[]>         ATTACHMENT_TYPE = SQLDataType.BLOB;


    public static final String     uri                = Gateway.getProperties().getString(JooqHandler.JOOQ_URI);
    public static final String     user               = Gateway.getProperties().getString(JooqHandler.JOOQ_USER);
    public static final String     pwd                = Gateway.getProperties().getString(JooqHandler.JOOQ_PASSWORD);
    public static final Boolean    autoCommit         = Gateway.getProperties().getBoolean(JooqHandler.JOOQ_AUTOCOMMIT, false);

    public static final Boolean    readOnlyDataSource = Gateway.getProperties().getBoolean(JooqHandler.JOOQ_READONLYDATASOURCE, false);
    public static final SQLDialect dialect            = SQLDialect.valueOf(Gateway.getProperties().getString(JooqHandler.JOOQ_DIALECT, "POSTGRES"));

    private static HikariDataSource ds = null;
    private static HikariConfig config;
    

    static {
        System.out.println("++++++++++++++++++++++JooqHandler"+ uri +";" + user +";"+pwd);
        System.err.println("++++++++++++++++++++++JooqHandler"+ uri +";" + user +";"+pwd);
    }

    public static synchronized HikariDataSource getDataSource() {
        if (ds == null) {
            if (StringUtils.isAnyBlank(uri, user, pwd))
                throw new IllegalArgumentException("JOOQ (uri, user, password) config values must not be blank");

            config = new HikariConfig();
            config.setPoolName("CRISTAL-iSE-HikariCP");
            config.setRegisterMbeans(true);
            config.setJdbcUrl(uri);
            config.setUsername(user);
            config.setPassword(pwd);
            config.setAutoCommit(autoCommit);
            config.setReadOnly(readOnlyDataSource);
            config.setMaximumPoolSize(50);
            config.setMaxLifetime(60000);
            config.setMinimumIdle(10);
            config.setIdleTimeout(30000);
            //config.setLeakDetectionThreshold(30000); // enable to see connection leak warning
            config.addDataSourceProperty( "cachePrepStmts",        true);
            config.addDataSourceProperty( "prepStmtCacheSize",     "250");
            config.addDataSourceProperty( "prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty( "autoCommit",             autoCommit);

            log.info("getDataSource() - uri:'{}' user:'{}' dialect:'{}'", uri, user, dialect);

            config.setAutoCommit(autoCommit);
            ds = new HikariDataSource(config);

            log.info("getDataSource() create datasource {}", ds);
        }
        return ds;
    }

    public static synchronized void recreateDataSource(boolean forcedAutoCommit) throws PersistencyException {
        if (ds == null)
            throw new PersistencyException("Cannot recreate a null data source");

        log.info("recreateDataSource() autocommit={}", forcedAutoCommit);

        HikariConfig config = new HikariConfig();
        ds.copyStateTo(config);
        config.setAutoCommit(forcedAutoCommit);
        config.addDataSourceProperty("autoCommit", forcedAutoCommit);
        closeDataSource();
        HikariDataSource newDs = new HikariDataSource(config);
        ds = newDs;
    }

    public static synchronized void closeDataSource() throws PersistencyException {
        try {
            if (ds != null){
                HikariPoolMXBean poolBean = ds.getHikariPoolMXBean();
                log.debug("closeDataSource() active connections={}", poolBean.getActiveConnections());

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
            log.error("JooqHandler could not connect to URI '"+uri+"' with user '"+user+"'", ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    public static DSLContext connect(Connection conn) throws PersistencyException {
        try {
            return using(conn);
        }
        catch (Exception ex) {
            log.error("JooqHandler could not connect to URI '"+uri+"' with user '"+user+"'", ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    abstract protected Table<?> getTable();

    abstract protected Field<?> getNextPKField(String... primaryKeys) throws PersistencyException;

    abstract protected List<Condition> getPKConditions(UUID uuid, String... primaryKeys) throws PersistencyException;

    protected Record fetchRecord(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        return context.select().from(getTable()).where(getPKConditions(uuid, primaryKeys)).fetchOne();
    }

    protected Result<?> fetchDistinctResult(DSLContext context, Field<?> field, UUID uuid, String...primaryKeys) throws PersistencyException {
        return context.selectDistinct(field).from(getTable()).where(getPKConditions(uuid, primaryKeys)).fetch();
    }

    public static String[] getPrimaryKeys(String path) {
        String[] pathArray   = StringUtils.split(path, "/");
        return Arrays.copyOfRange(pathArray, 1, pathArray.length);
    }

    protected List<Condition> getPKConditions(UUID uuid, C2KLocalObject obj) throws PersistencyException {
        return getPKConditions(uuid, getPrimaryKeys(obj.getClusterPath()));
    }

    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        if (exists(context, uuid, obj)) return update(context, uuid, obj);
        else                            return insert(context, uuid, obj);
    }

    public int delete(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException {
        return context.delete(getTable()).where(getPKConditions(uuid, primaryKeys)).execute();
    }

    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String... primaryKeys) throws PersistencyException {
        Field<?> field = getNextPKField(primaryKeys);
        
        if (field == null) return new String[0];

        Result<?> result = fetchDistinctResult(context, field, uuid, primaryKeys);

        String[] returnValue = new String[result.size()];

        int i = 0;
        for (Record rec : result) returnValue[i++] = rec.get(0).toString();

        return returnValue;
    }

    public boolean exists(DSLContext context, UUID uuid) throws PersistencyException {
        return context.fetchExists( select().from(getTable()).where(getPKConditions(uuid)) );
    }

    public boolean exists(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException {
        return context.fetchExists( select().from(getTable()).where(getPKConditions(uuid, obj)) );
    }

    /**
     * Reads the record from the given field as UUID. 
     * 
     * @param record the record to read
     * @param field the field to be read
     * @return the UUID
     */
    protected UUID getUUID(Record record, Field<UUID> field) {
        //There a bug in jooq supporting UUID with MySQL: check issue #23
        if (record.get(field.getName()) instanceof String) return java.util.UUID.fromString(record.get(field.getName(), String.class));
        else                                               return record.get(field);
    }

    /**
     * Return the good XML type for the given dialect
     * 
     * @param context the context
     * @return XML type 
     */
    protected DataType<String> getXMLType(DSLContext context) {
        //There a bug in jooq supporting CLOB with MySQL: check issue #23
        if (context.dialect().equals(SQLDialect.MYSQL)) return XML_TYPE_MYSQL;
        else                                            return XML_TYPE;
    }

    abstract public void createTables(DSLContext context) throws PersistencyException;

    abstract public void dropTables(DSLContext context) throws PersistencyException;

    abstract public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    abstract public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    abstract public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException;

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
}
