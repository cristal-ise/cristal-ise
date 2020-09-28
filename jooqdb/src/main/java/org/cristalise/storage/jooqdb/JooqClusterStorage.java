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

import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_DISABLE_DOMAIN_CREATE;
import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_DOMAIN_HANDLERS;
import static org.cristalise.storage.jooqdb.JooqHandler.getPrimaryKeys;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.storage.jooqdb.clusterStore.JooqCollectionHadler;
import org.cristalise.storage.jooqdb.clusterStore.JooqHistoryHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqJobHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqLifecycleHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqOutcomeAttachmentHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqOutcomeHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqViewpointHandler;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link TransactionalClusterStorage} based on <a>http://www.jooq.org/</a>}
 */
@Slf4j
public class JooqClusterStorage extends ClusterStorage {

    protected HashMap<ClusterType, JooqHandler>     jooqHandlers   = new HashMap<ClusterType, JooqHandler>();
    protected List<JooqDomainHandler>               domainHandlers = new ArrayList<JooqDomainHandler>();
    protected ConcurrentHashMap<Object, Connection> connectionMap  = new ConcurrentHashMap<Object, Connection>();

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        JooqHandler.readSystemProperties();
        initialiseHandlers();
    }

    /**
     * Initialise internal handlers for all ClusterTypes and all the DomainHandlers-
     *
     * @throws PersistencyException Error during initialise ...
     */
    public void initialiseHandlers() throws PersistencyException {
        log.info("initialiseHandlers() - Starting with standard hadlers.");

        jooqHandlers.put(ClusterType.PROPERTY,   new JooqItemPropertyHandler());
        jooqHandlers.put(ClusterType.OUTCOME,    new JooqOutcomeHandler());
        jooqHandlers.put(ClusterType.VIEWPOINT,  new JooqViewpointHandler());
        jooqHandlers.put(ClusterType.LIFECYCLE,  new JooqLifecycleHandler());
        jooqHandlers.put(ClusterType.COLLECTION, new JooqCollectionHadler());
        jooqHandlers.put(ClusterType.HISTORY,    new JooqHistoryHandler());
        jooqHandlers.put(ClusterType.JOB,        new JooqJobHandler());
        jooqHandlers.put(ClusterType.ATTACHMENT, new JooqOutcomeAttachmentHandler());

        DSLContext context = retrieveContext(null);

        if (!JooqHandler.readOnlyDataSource) {
            context.transaction(nested -> {
                for (JooqHandler handler : jooqHandlers.values())
                    handler.createTables(DSL.using(nested));
            });
            initialiseDomainHandlers(context);
        }
    }

    /**
     * Initialise internal domain handlers handlers
     *
     * @throws PersistencyException Error during initialise ...
     */
    private void initialiseDomainHandlers(DSLContext context) throws PersistencyException {
        try {
            String handlers = Gateway.getProperties().getString(JOOQ_DOMAIN_HANDLERS, "");

            for(String handlerClass: StringUtils.split(handlers, ",")) {
                if (!handlerClass.contains(".")) handlerClass = "org.cristalise.storage."+handlerClass;

                log.info("initialiseHandlers() - Instantiate domain handler:"+handlerClass);

                domainHandlers.add( (JooqDomainHandler) Class.forName(handlerClass).newInstance());
            }
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            log.error("JooqClusterStorage could not instantiate domain handler", ex);
            throw new PersistencyException("JooqClusterStorage could not instantiate domain handler:"+ex.getMessage());
        }

        if (! Gateway.getProperties().getBoolean(JOOQ_DISABLE_DOMAIN_CREATE, false)) {
            context.transaction(nested -> {
                for (JooqDomainHandler handler: domainHandlers) handler.createTables(DSL.using(nested));
            });
        }
    }

    public void dropHandlers() throws PersistencyException {
        DSLContext context = retrieveContext(null);
        context.transaction(nested -> {
            for (JooqHandler handler: jooqHandlers.values()) handler.dropTables(DSL.using(nested));
        });

    }

    @Override
    public void close() throws PersistencyException {
        log.info("close()");
        JooqHandler.closeDataSource();
    }

    @Override
    public void postBoostrap() throws PersistencyException {
        retrieveContext(null).transaction(nested ->{
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postBoostrap(DSL.using(nested));
        });

        // after the the bootstrap the DataSource needs to be reset to its original config
        if (!JooqHandler.readOnlyDataSource && !JooqHandler.autoCommit) {
            //Restore data source with original auto-commit setting
            JooqHandler.recreateDataSource(JooqHandler.autoCommit);
        }
    }

    @Override
    public void postStartServer() throws PersistencyException {
        retrieveContext(null).transaction(nested ->{
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postStartServer(DSL.using(nested));
        });
    }

    @Override
    public void postConnect() throws PersistencyException {
        // the DataSource need to be set to autocommit for the the bootstrap to work
        if (!JooqHandler.readOnlyDataSource && !JooqHandler.autoCommit) {
            //recreate a new DS with auto-commit forced to true
            JooqHandler.recreateDataSource(true);
        }

        retrieveContext(null).transaction(nested -> {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postConnect(DSL.using(nested));
        });

    }

    @Override
    public void begin(Object transactionKey)  throws PersistencyException {
        if (!JooqHandler.getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        log.info("begin() - transactionKey:{}", transactionKey);

        if (transactionKey != null) {
            Connection conn = JooqHandler.connect().configuration().connectionProvider().acquire();
            connectionMap.put(transactionKey, conn);
        }
        else {
            log.warn("begin() called with a null transactionKey");
        }
    }

    private DSLContext retrieveContext(Object transactionKey) throws PersistencyException {
        log.trace("retrieveContext() - transactionKey:{}", transactionKey);

        if (JooqHandler.getDataSource().isAutoCommit() || transactionKey == null) {
            return JooqHandler.connect();
        }
        else {
            Connection conn = connectionMap.get(transactionKey);
            if (conn != null) return JooqHandler.connect(conn);
        }

        String msg = "Could not find JDBC connection for transactionKey:"+transactionKey;
        log.error("retrieveContext() - {}", msg);
        throw new PersistencyException(msg);
    }

    @Override
    public void commit(Object transactionKey) throws PersistencyException {
        if (!JooqHandler.getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        if (transactionKey == null) {
            log.warn("commit() - Cannot retrieve connection because transactionKey is null");
            return;
        }

        log.info("commit()");

        DSLContext context = retrieveContext(transactionKey);

        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.commit(context, transactionKey);

        try {
            Connection conn = connectionMap.remove(transactionKey);
            if (!JooqHandler.getDataSource().isAutoCommit()) {
              conn.commit();
            }
            conn.close();
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void abort(Object transactionKey) throws PersistencyException {
        if (!JooqHandler.getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        if (transactionKey == null) {
            log.warn("abort() - Cannot retrieve connection because transactionKey is null");
            return;
        }

        log.info("abort()");

        DSLContext context = retrieveContext(transactionKey);

        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.abort(context, transactionKey);

        try {
            Connection conn = connectionMap.remove(transactionKey);
            if (!JooqHandler.getDataSource().isAutoCommit()) {
                conn.rollback();
            }
            conn.close();
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public short queryClusterSupport(ClusterType type) {
        if (type == ClusterType.PATH) {
            return NONE;
        }

        return READWRITE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        String lang = language.trim().toUpperCase();
        return "SQL".equals(lang) || ("SQL:"+JooqHandler.dialect).equals(lang);
    }

    @Override
    public String getName() {
        return "JOOQ:"+JooqHandler.dialect+" ClusterStorage";
    }

    @Override
    public String getId() {
        return "JOOQ:"+JooqHandler.dialect;
    }
    
    @Override
    public int getLastIntegerId(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        DSLContext  context = retrieveContext(transactionKey);
        ClusterType cluster = getClusterType(path);
        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler == null) {
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
        }
        else if (cluster == ClusterType.HISTORY) {
            return ((JooqHistoryHandler)handler).getLastEventId(context, itemPath.getUUID());
        }
        else if (cluster == ClusterType.JOB) {
            return ((JooqJobHandler)handler).getLastJobId(context, itemPath.getUUID());
        }
        else {
            String msg = "Invalid ClusterType! Must be either HISTORY or JOB. Actual cluster:" + cluster;
            log.error("getLastIntegerId() - {}", msg);
            throw new PersistencyException(msg);
        }
    }

    @Override
    public String executeQuery(Query query, Object transactionKey) throws PersistencyException {
        throw new PersistencyException("UnImplemented");
    }

    @Override
    public ClusterType[] getClusters(ItemPath itemPath, Object transactionKey) throws PersistencyException {
        ArrayList<ClusterType> result = new ArrayList<ClusterType>();

        for (ClusterType type:jooqHandlers.keySet()) {
            if (jooqHandlers.get(type).exists(retrieveContext(transactionKey), itemPath.getUUID())) result.add(type);
        }

        return result.toArray(new ClusterType[0]);
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        if (StringUtils.isBlank(path)) {
            ArrayList<String> result = new ArrayList<String>();

            for (ClusterType k: getClusters(itemPath, transactionKey)) { result.add(k.toString()); }

            return result.toArray(new String[0]);
        }

        UUID        uuid        = itemPath.getUUID();
        ClusterType cluster     = getClusterType(path);
        JooqHandler handler     = jooqHandlers.get(cluster);
        String[]    primaryKeys = getPrimaryKeys(path);

        if (handler != null) {
            log.debug("getClusterContents() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));
            return handler.getNextPrimaryKeys(retrieveContext(transactionKey), uuid, primaryKeys);
        }
        else
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        ClusterType cluster     = getClusterType(path);
        JooqHandler handler     = jooqHandlers.get(cluster);
        String[]    primaryKeys = getPrimaryKeys(path);

        if (handler != null) {
            log.debug("get() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys:"+Arrays.toString(primaryKeys));

            C2KLocalObject obj = handler.fetch(retrieveContext(transactionKey), uuid, primaryKeys);

            if (obj == null) {
                log.trace(("JooqClusterStorage.get() - Could NOT fetch '"+itemPath+"/"+path+"'"));
            }
            return obj;
        }
        else
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object transactionKey) throws PersistencyException {
        if (!JooqHandler.getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        UUID        uuid    = itemPath.getUUID();
        ClusterType cluster = obj.getClusterType();
        JooqHandler handler = jooqHandlers.get(cluster);
        DSLContext  context = retrieveContext(transactionKey);

        JooqHandler.logConnectionCount("JooqClusterStorage.put(before)", context);

        if (handler != null) {
            log.debug("put() - uuid:"+uuid+" cluster:"+cluster+" path:"+obj.getClusterPath());
            handler.put(context, uuid, obj);
        }
        else {
            throw new PersistencyException("Write is not supported for cluster:'"+cluster+"'");
        }
        // Trigger all registered handlers to update domain specific tables
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.put(context, uuid, obj, transactionKey);

        JooqHandler.logConnectionCount("JooqClusterStorage.put(after) ", context);
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        if (!JooqHandler.getDataSource().isAutoCommit() && transactionKey == null) {
            throw new PersistencyException("transactionKey cannot be null when autoCommit is false");
        }

        UUID        uuid        = itemPath.getUUID();
        ClusterType cluster     = getClusterType(path);
        JooqHandler handler     = jooqHandlers.get(cluster);
        String[]    primaryKeys = getPrimaryKeys(path);
        DSLContext  context     = retrieveContext(transactionKey);

        if (handler != null) {
            log.debug("delete() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));
            handler.delete(context, uuid, primaryKeys);
        }
        else {
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
        }

        // Trigger all registered handlers to update domain specific tables
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.delete(context, uuid, transactionKey, primaryKeys);
    }
}
