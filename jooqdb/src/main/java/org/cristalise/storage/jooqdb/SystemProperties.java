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

import org.cristalise.kernel.utils.SystemPropertyOperations;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import lombok.Getter;

public enum SystemProperties implements SystemPropertyOperations {

    /**
     * Value to configure Hikari ConnectionPool JdbcUrl
     */
    JOOQ_URI("JOOQ.URI"),
    /**
     * Value to configure Hikari ConnectionPool userName
     */
    JOOQ_user("JOOQ.user"),
    /**
     * Value to configure Hikari ConnectionPool password
     */
    JOOQ_password("JOOQ.password"),
    /**
     * Value to configure JOOQ database dialect. Default is 'POSTGRES'
     */
    JOOQ_dialect("JOOQ.dialect", SQLDialect.POSTGRES.name()),
    /**
     * Value to configure Hikari ConnectionPool to autoCommit. Default is 'false'
     */
    JOOQ_autoCommit("JOOQ.autoCommit", false),
    /**
     * Value to configure Hikari ConnectionPool to readOnly. Default is 'false'
     */
    JOOQ_readOnlyDataSource("JOOQ.readOnlyDataSource", false),
    /**
     * Value to configure Hikari ConnectionPool maximumPoolSize. Default value is 50.
     */
    JOOQ_maximumPoolSize("JOOQ.maximumPoolSize", 50),
    /**
     * Value to configure Hikari ConnectionPool maxLifetime. Default value is 60000 ms.
     */
    JOOQ_maxLifetime("JOOQ.maxLifetime", 60000),
    /**
     * Value to configure Hikari ConnectionPool minimumIdle. Default value is 10.
     */
    JOOQ_minimumIdle("JOOQ.minimumIdle", 10),
    /**
     * Value to configure Hikari ConnectionPool idleTimeout. Default value is 30000 ms.
     */
    JOOQ_idleTimeout("JOOQ.idleTimeout", 30000),
    /**
     * Defines the prefix key to retrieve String entries to add as additional DataSourceProperties
     */
    JOOQ_DataSourceProperty("JOOQ.DataSourceProperty."),
    /**
     * Comma separated list of fully qualified class names implementing the {@link JooqDomainHandler} interface.
     * Default value is blank string.
     */
    JOOQ_domainHandlers("JOOQ.domainHandlers", ""),
    /**
     * Disable the invocation of {@link JooqDomainHandler#createTables(DSLContext)}. Default is 'false'
     */
    JOOQ_disableDomainCreateTables("JOOQ.disableDomainCreateTables", false),
    /**
     * Defines the VARCHAR size of NAME_TYPE type declaration {@link JooqDomainHandler#createTables(DSLContext)}. 
     * Default is '64'
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
     *   - JOB.AGENT_ROLE
     *   - LIFECYCLE.NAME
     *   - OUTCOME.SCHEMA_NAME
     *   - OUTCOME_ATTACHMENT.SCHEMA_NAME
     *   - VIEWPOINT.SCHEMA_NAME
     * </pre>
     */
    JOOQ_NameType_length("JOOQ.NameType.length", 64),
    /**
     * Defines the key (value:{@value}) to retrieve the integer value for VARCHAR size of PASSWORD_TYPE type declaration
     * {@link JooqDomainHandler#createTables(DSLContext)}. Default is '800'
     * 
     * <p>It is used for these columns:
     * <pre>
     *   - ITEM.PASSWORD
     * </pre>
     */
    JOOQ_PasswordType_length("JOOQ.PasswordType.length", 800),
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
    JOOQ_StringType_length("JOOQ.StringType.length", 800),
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
    JOOQ_TextType_length("JOOQ.TextType.length", 800),
    JooqAuth_Argon2_type("JooqAuth.Argon2.type", "ARGON2id"),
    JooqAuth_Argon2_iterations("JooqAuth.Argon2.iterations", 2),
    JooqAuth_Argon2_memory("JooqAuth.Argon2.memory", 65536),
    JooqAuth_Argon2_parallelism("JooqAuth.Argon2.parallelism", 1),

    JooqLookupManager_getChildrenPattern_specialCharsToReplace("JooqLookupManager.getChildrenPattern.specialCharsToReplace", "[^a-zA-Z0-9 ]")
    ;

    @Getter
    private final Object defaultValue;
    @Getter
    private final String systemPropertyName;

    private SystemProperties(String name) {
        this(name, null);
    }

    private SystemProperties(String name, Object value) {
        systemPropertyName = name;
        defaultValue = value;
    }

    @Override
    public String toString() {
        return systemPropertyName;
    }
}
