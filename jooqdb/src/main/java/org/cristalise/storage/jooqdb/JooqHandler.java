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

import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.using;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DefaultConnectionProvider;
import org.jooq.impl.SQLDataType;

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
     * Defines the key (value:{@value}) to retrieve the string value of the comma separated list of 
     * fully qualified class names implementing the {@link JooqDomainHandler} interface.
     */
    public static final String JOOQ_DOMAIN_HANDLERS = "JOOQ.domainHandlers";

    public static final DataType<UUID>           UUID_TYPE      = SQLDataType.UUID;
    public static final DataType<String>         NAME_TYPE      = SQLDataType.VARCHAR.length(64);
    public static final DataType<Integer>        VERSION_TYPE   = SQLDataType.INTEGER;
    public static final DataType<String>         STRING_TYPE    = SQLDataType.VARCHAR.length(4096);
    public static final DataType<Integer>        ID_TYPE        = SQLDataType.INTEGER;
    public static final DataType<Timestamp>      TIMESTAMP_TYPE = SQLDataType.TIMESTAMP;
//    public static final DataType<OffsetDateTime> TIMESTAMP_TYPE = SQLDataType.TIMESTAMPWITHTIMEZONE;
    public static final DataType<String>         XML_TYPE       = SQLDataType.CLOB;

    public static DSLContext connect() throws PersistencyException {
        String uri  = Gateway.getProperties().getString(JooqHandler.JOOQ_URI);
        String user = Gateway.getProperties().getString(JooqHandler.JOOQ_USER); 
        String pwd  = Gateway.getProperties().getString(JooqHandler.JOOQ_PASSWORD);

        if (StringUtils.isAnyBlank(uri, user, pwd)) {
            throw new IllegalArgumentException("JOOQ (uri, user, password) config values must not be blank");
        }

        SQLDialect dialect = SQLDialect.valueOf(Gateway.getProperties().getString(JooqHandler.JOOQ_DIALECT, "POSTGRES"));

        Logger.msg(1, "JooqHandler.open() - uri:'"+uri+"' user:'"+user+"' dialect:'"+dialect+"'");

        try {
            DSLContext context = using(DriverManager.getConnection(uri, user, pwd), dialect);

            boolean autoCommit = Gateway.getProperties().getBoolean(JooqHandler.JOOQ_AUTOCOMMIT, false);

            ((DefaultConnectionProvider)context.configuration().connectionProvider()).setAutoCommit(autoCommit);

            return context;
        }
        catch (Exception ex) {
            Logger.error("JooqHandler could not connect to URI '"+uri+"' with user '"+user+"'");
            Logger.error(ex);
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

    protected List<Condition> getPKConditions(UUID uuid, C2KLocalObject obj) throws PersistencyException {
        String[] pathArray   = obj.getClusterPath().split("/");
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        return getPKConditions(uuid, primaryKeys);
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

    abstract public void createTables(DSLContext context) throws PersistencyException;

    abstract public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    abstract public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    abstract public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException;
}
