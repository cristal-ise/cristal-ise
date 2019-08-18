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
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionalClusterStorage;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;
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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * Implementation of the {@link TransactionalClusterStorage} based on <a>http://www.jooq.org/</a>}
 */
public class JooqClusterStorage extends TransactionalClusterStorage {

    protected HashMap<ClusterType, JooqHandler>     jooqHandlers   = new HashMap<ClusterType, JooqHandler>();
    protected List<JooqDomainHandler>               domainHandlers = new ArrayList<JooqDomainHandler>();
    //protected ConcurrentHashMap<Object, DSLContext> contextMap     = new ConcurrentHashMap<Object, DSLContext>();
    protected ConcurrentHashMap<Object, Connection> connectionMap  = new ConcurrentHashMap<Object, Connection>();

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        initialiseHandlers();
    }

    /**
     * Initialise internal handlers for all ClusterTypes and all the DomainHandlers-
     *
     * @throws PersistencyException Error during initialise ...
     */
    public void initialiseHandlers() throws PersistencyException {
        Logger.msg(1, "JooqClusterStorage.initialiseHandlers() - Starting with standard hadlers.");

        jooqHandlers.put(ClusterType.PROPERTY,   new JooqItemPropertyHandler());
        jooqHandlers.put(ClusterType.OUTCOME,    new JooqOutcomeHandler());
        jooqHandlers.put(ClusterType.VIEWPOINT,  new JooqViewpointHandler());
        jooqHandlers.put(ClusterType.LIFECYCLE,  new JooqLifecycleHandler());
        jooqHandlers.put(ClusterType.COLLECTION, new JooqCollectionHadler());
        jooqHandlers.put(ClusterType.HISTORY,    new JooqHistoryHandler());
        jooqHandlers.put(ClusterType.JOB,        new JooqJobHandler());
        jooqHandlers.put(ClusterType.ATTACHMENT, new JooqOutcomeAttachmentHandler());

        DSLContext context = JooqHandler.connect();

        context.transaction(nested -> {
            for (JooqHandler handler: jooqHandlers.values()) handler.createTables(DSL.using(nested));
        });

        try {
            String handlers = Gateway.getProperties().getString(JOOQ_DOMAIN_HANDLERS, "");

            for(String handlerClass: StringUtils.split(handlers, ",")) {
                if (!handlerClass.contains(".")) handlerClass = "org.cristalise.storage."+handlerClass;

                Logger.msg(1, "JooqClusterStorage.initialiseHandlers() - Instantiate domain handler:"+handlerClass);

                domainHandlers.add( (JooqDomainHandler) Class.forName(handlerClass).newInstance());
            }
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.error("JooqClusterStorage could not instantiate domain handler");
            Logger.error(ex);
            throw new PersistencyException("JooqClusterStorage could not instantiate domain handler:"+ex.getMessage());
        }

        if (! Gateway.getProperties().getBoolean(JOOQ_DISABLE_DOMAIN_CREATE, false)) {
            context.transaction(nested -> {
                for (JooqDomainHandler handler: domainHandlers) handler.createTables(DSL.using(nested));
            });
        }
    }

    public void dropHandlers() throws PersistencyException {
        DSLContext context = JooqHandler.connect();
        context.transaction(nested -> {
            for (JooqHandler handler: jooqHandlers.values()) handler.dropTables(DSL.using(nested));
        });
        
    }
    
    private void recreateDS(boolean autoCommit) {
        HikariConfig config = new HikariConfig();
        JooqHandler.ds.copyStateTo(config);
        config.setAutoCommit(autoCommit);
        config.addDataSourceProperty("autoCommit", autoCommit);
        HikariDataSource newDs = new HikariDataSource(config);
        JooqHandler.ds = newDs;
    }

    @Override
    public void close() throws PersistencyException {
        Logger.msg(1, "JooqClusterStorage.close()");
        try {
            HikariPoolMXBean poolBean = JooqHandler.ds.getHikariPoolMXBean();
            while (poolBean.getActiveConnections() > 0) {
              poolBean.softEvictConnections();
            }
            JooqHandler.ds.close();
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void postBoostrap() throws PersistencyException {
        JooqHandler.connect().transaction(nested ->{
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postBoostrap(DSL.using(nested));
        });

        // after the the bootstrap the DataSOurce needs to be reset to its original config
        if (!JooqHandler.autoCommit) {
            //Close current DS and create a new one
            close();
            recreateDS(JooqHandler.autoCommit);
        }
    }

    @Override
    public void postStartServer() throws PersistencyException {
        JooqHandler.connect().transaction(nested ->{
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postStartServer(DSL.using(nested));
        });
    }

    @Override
    public void postConnect() throws PersistencyException {
        // the DataSource need to be set to autocommit for the the bootstrap to work
        if (!JooqHandler.autoCommit) {
            //Close current DS and create a new one
            close();
            recreateDS(true);
        }

        JooqHandler.connect().transaction(nested -> {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postConnect(DSL.using(nested));
        });
        
    }

    @Override
    public void begin(Object locker)  throws PersistencyException {
        if (!JooqHandler.ds.isAutoCommit() && locker == null) {
            throw new PersistencyException("locker cannot be null when autoCommit is false");
        }

        Connection conn = JooqHandler.connect().configuration().connectionProvider().acquire();

        if (!JooqHandler.ds.isAutoCommit()) connectionMap.put(locker, conn);
    }

    private DSLContext retrieveContext(Object locker) throws PersistencyException {
        if (JooqHandler.ds.isAutoCommit()) return JooqHandler.connect();
        else                               return JooqHandler.connect(connectionMap.get(locker));
    }

    @Override
    public void commit(Object locker) throws PersistencyException {
        if (!JooqHandler.ds.isAutoCommit() && locker == null) {
            throw new PersistencyException("locker cannot be null when autoCommit is false");
        }

        DSLContext context = retrieveContext(locker);

        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.commit(context, locker);

        if (JooqHandler.ds.isAutoCommit()) {
            Logger.msg(1, "JooqClusterStorage.commit(DISABLED) - autoCommit:"+JooqHandler.ds.isAutoCommit());
            return;
        }

        Logger.msg(1, "JooqClusterStorage.commit()");

        try {
            Connection conn = connectionMap.remove(locker);
            conn.commit();
            conn.close();
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void abort(Object locker) throws PersistencyException {
        if (!JooqHandler.ds.isAutoCommit() && locker == null) {
            throw new PersistencyException("locker cannot be null when autoCommit is false");
        }

        DSLContext context = retrieveContext(locker);

        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.abort(context, locker);

        if (JooqHandler.ds.isAutoCommit()) {
            Logger.msg(1, "JooqClusterStorage.abort(DISABLED) - autoCommit:"+JooqHandler.ds.isAutoCommit());
            return;
        }

        Logger.msg(1, "JooqClusterStorage.abort()");
        try {
            Connection conn = connectionMap.remove(locker);
            conn.rollback();
            conn.close();
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public short queryClusterSupport(String type) {
        return queryClusterSupport(ClusterType.getValue(type));
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
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UnImplemented");
    }

    @Override
    public ClusterType[] getClusters(ItemPath itemPath) throws PersistencyException {
        ArrayList<ClusterType> result = new ArrayList<ClusterType>();
 
        for (ClusterType type:jooqHandlers.keySet()) {
            if (jooqHandlers.get(type).exists(JooqHandler.connect(), itemPath.getUUID())) result.add(type);
        }

        return result.toArray(new ClusterType[0]);
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        if (StringUtils.isBlank(path)) {
            ArrayList<String> result = new ArrayList<String>();

            for (ClusterType k: getClusters(itemPath)) { result.add(k.toString()); }

            return result.toArray(new String[0]);
        }

        UUID uuid = itemPath.getUUID();

        String[]    pathArray   = path.split("/");
        String[]    primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        ClusterType cluster     = ClusterType.getValue(pathArray[0]);

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.getClusterContents() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));

            return handler.getNextPrimaryKeys(JooqHandler.connect(), uuid, primaryKeys);
        }
        else
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        String[]    pathArray   = path.split("/");
        String[]    primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        ClusterType cluster     = ClusterType.getValue(pathArray[0]);

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.get() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys:"+Arrays.toString(primaryKeys));

            C2KLocalObject obj = handler.fetch(JooqHandler.connect(), uuid, primaryKeys);

            if (obj == null && Logger.doLog(8)) {
                Logger.warning(("JooqClusterStorage.get() - Could NOT fetch '"+itemPath+"/"+path+"'"));
            }
            return obj;
        }
        else
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        put(itemPath, obj, null);
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        if (!JooqHandler.ds.isAutoCommit() && locker == null) {
            throw new PersistencyException("locker cannot be null when autoCommit is false");
        }

        UUID uuid = itemPath.getUUID();
        ClusterType cluster = obj.getClusterType();

        JooqHandler handler = jooqHandlers.get(cluster);

        DSLContext context = retrieveContext(locker);
        if (Logger.doLog(5)) JooqHandler.logConnectionCount("JooqClusterStorage.put(before)", context);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.put() - uuid:"+uuid+" cluster:"+cluster+" path:"+obj.getClusterPath());

            handler.put(context, uuid, obj);
        }
        else {
            throw new PersistencyException("Write is not supported for cluster:'"+cluster+"'");
        }
        // Trigger all registered handlers to update domain specific tables
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.put(context, uuid, obj, locker);
        if (Logger.doLog(5)) JooqHandler.logConnectionCount("JooqClusterStorage.put(after) ", context);
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        delete(itemPath, path, null);
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        if (!JooqHandler.ds.isAutoCommit() && locker == null) {
            throw new PersistencyException("locker cannot be null when autoCommit is false");
        }

        UUID uuid = itemPath.getUUID();

        String[]    pathArray   = path.split("/");
        String[]    primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        ClusterType cluster     = ClusterType.getValue(pathArray[0]);

        JooqHandler handler = jooqHandlers.get(cluster);

        DSLContext context = retrieveContext(locker);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.delete() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));
            handler.delete(context, uuid, primaryKeys);
        }
        else {
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
        }

        // Trigger all registered handlers to update domain specific tables
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.delete(context, uuid, locker, primaryKeys);
       
    }
}
