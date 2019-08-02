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

import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_AUTOCOMMIT;
import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_DISABLE_DOMAIN_CREATE;
import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_DOMAIN_HANDLERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
import org.jooq.impl.DataSourceConnectionProvider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

/**
 * Implementation of the {@link TransactionalClusterStorage} based on <a>http://www.jooq.org/</a>}
 */
public class JooqClusterStorage extends TransactionalClusterStorage {

    protected Boolean autoCommit = Gateway.getProperties().getBoolean(JOOQ_AUTOCOMMIT, false);

    protected HashMap<ClusterType, JooqHandler> jooqHandlers   = new HashMap<ClusterType, JooqHandler>();
    protected List<JooqDomainHandler>           domainHandlers = new ArrayList<JooqDomainHandler>();
    protected ConcurrentHashMap<Object, DSLContext> contextMap = new ConcurrentHashMap<Object, DSLContext>(); 

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
        
        if(Gateway.getProperties().containsKey(JOOQ_AUTOCOMMIT)){

            Gateway.getProperties().put(JOOQ_AUTOCOMMIT, false);
            HikariConfig config = new HikariConfig();
            JooqHandler.ds.copyStateTo(config);
            config.setAutoCommit(false);
            config.addDataSourceProperty( "autoCommit",false);
            HikariDataSource newDs = new HikariDataSource(config);    
            JooqHandler.ds = newDs;
           
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
        if(Gateway.getProperties().containsKey(JOOQ_AUTOCOMMIT)){
            Gateway.getProperties().put(JOOQ_AUTOCOMMIT, true);
        }
        JooqHandler.connect().transaction(nested ->{
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.postConnect(DSL.using(nested));
        });
        
    }

    @Override
    public void begin(Object locker) {
        try {
            DSLContext context = JooqHandler.connect();
            
            if(!Objects.isNull(locker)){
                contextMap.put(locker, context);
            }
            
            if (Logger.doLog(5)) JooqHandler.logConnectionCount("JooqClusterStorage.begin()", context);
        }
        catch (PersistencyException e) {
            Logger.error(e);
        }
    }

    @SuppressWarnings("resource")
    @Override
    public void commit(Object locker) throws PersistencyException {
        DSLContext context = null;
        
        autoCommit =  Gateway.getProperties().getBoolean(JOOQ_AUTOCOMMIT);
        
        if(!autoCommit && contextMap.get(locker) == null){
            throw new PersistencyException("JooqClusterStorage.commit(DISABLED) - Unable to get connection");
        }
        
        context = (contextMap.get(locker)) == null ? JooqHandler.connect() : (DSLContext) contextMap.get(locker);
      
        context.transaction(nested -> {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.commit(DSL.using(nested), locker);
        });
      
        if (autoCommit) {
            Logger.msg(1, "JooqClusterStorage.commit(DISABLED) - autoCommit:"+autoCommit);
            return;
        }
        
        Logger.msg(1, "JooqClusterStorage.commit()");
        try {
            ((DataSourceConnectionProvider)context.configuration().connectionProvider()).acquire().commit();

            if (Logger.doLog(5)) JooqHandler.logConnectionCount("JooqClusterStorage.commit()", context);
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @SuppressWarnings("resource")
    @Override
    public void abort(Object locker) throws PersistencyException {
        DSLContext context = null;
        
        autoCommit =  Gateway.getProperties().getBoolean(JOOQ_AUTOCOMMIT);
        if(!autoCommit && contextMap.get(locker) == null){
            throw new PersistencyException("JooqClusterStorage.commit(DISABLED) - Unable to get connection");
        }
        
        context = (contextMap.get(locker)) == null ? JooqHandler.connect() : (DSLContext) contextMap.get(locker);
        
        context.transaction(nested -> {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.abort(DSL.using(nested), locker);
        });
        
        if (autoCommit) {
            Logger.msg(1, "JooqClusterStorage.abort(DISABLED) - autoCommit:"+autoCommit);
            return;
        }

        Logger.msg(1, "JooqClusterStorage.abort()");
        try {
            ((DataSourceConnectionProvider)context.configuration().connectionProvider()).acquire().rollback();
        }
        catch (Exception e) {
            Logger.error(e);
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

    @SuppressWarnings("resource")
    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();
        ClusterType cluster = obj.getClusterType();

        JooqHandler handler = jooqHandlers.get(cluster);
        
         DSLContext context =  null;
         
         if(Objects.isNull(locker) || contextMap.get(locker) == null ){
             context = JooqHandler.connect();
         } else {
             context =  (DSLContext) contextMap.get(locker);
         }
        
        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.put() - uuid:"+uuid+" cluster:"+cluster+" path:"+obj.getClusterPath());
            
            context.transaction(nested -> {
                handler.put(DSL.using(nested), uuid, obj);
            });  
        }
        else {
            throw new PersistencyException("Write is not supported for cluster:'"+cluster+"'");
        }
        // Trigger all registered handlers to update domain specific tables
        context.transaction(nested -> {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.put(DSL.using(nested), uuid, obj, locker);
        });
        
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        delete(itemPath, path, null);
    }

    @SuppressWarnings("resource")
    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        String[]    pathArray   = path.split("/");
        String[]    primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        ClusterType cluster     = ClusterType.getValue(pathArray[0]);

        DSLContext context  = null;
        JooqHandler handler = jooqHandlers.get(cluster);
        
        if(Objects.isNull(locker) || contextMap.get(locker) == null ){
            context = JooqHandler.connect();
        } else {
            context =  (DSLContext) contextMap.get(locker);
        }
        
        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.delete() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));
            context.transaction(nested -> {
                handler.delete(DSL.using(nested), uuid, primaryKeys);
            });
        }
        else {
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
        }

        // Trigger all registered handlers to update domain specific tables
        context.transaction(nested -> {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.delete(DSL.using(nested), uuid, locker, primaryKeys);
        });
       
    }
}
