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
package org.cristalise.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.TransactionalClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqDomainHandler;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqCollectionHadler;
import org.cristalise.storage.jooqdb.clusterStore.JooqHistoryHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqJobHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqLifecycleHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqOutcomeHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqViewpointHandler;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DefaultConnectionProvider;

public class JooqClusterStorage extends TransactionalClusterStorage {

    protected DSLContext context;
    protected Boolean autoCommit;

    HashMap<String, JooqHandler> jooqHandlers  = new HashMap<String, JooqHandler>();
    List<JooqDomainHandler>      domainHandlers= new ArrayList<JooqDomainHandler>();

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        context = JooqHandler.connect();

        autoCommit = Gateway.getProperties().getBoolean(JooqHandler.JOOQ_AUTOCOMMIT, false);

        initialiseHandlers();
    }

    /**
     * 
     */
    public void initialiseHandlers() throws PersistencyException {
        Logger.msg(1, "JooqClusterStorage.initialiseHandlers() - Starting with standard hadlers.");

        jooqHandlers.put(ClusterStorage.PROPERTY,   new JooqItemPropertyHandler());
        jooqHandlers.put(ClusterStorage.OUTCOME,    new JooqOutcomeHandler());
        jooqHandlers.put(ClusterStorage.VIEWPOINT,  new JooqViewpointHandler());
        jooqHandlers.put(ClusterStorage.LIFECYCLE,  new JooqLifecycleHandler());
        jooqHandlers.put(ClusterStorage.COLLECTION, new JooqCollectionHadler());
        jooqHandlers.put(ClusterStorage.HISTORY,    new JooqHistoryHandler());
        jooqHandlers.put(ClusterStorage.JOB,        new JooqJobHandler());

        for(JooqHandler handler: jooqHandlers.values()) handler.createTables(context);

        try {
            if(Gateway.getProperties().containsKey(JooqHandler.JOOQ_DOMAIN_HANDLERS)) {
                for(String handlerClass: Gateway.getProperties().getString(JooqHandler.JOOQ_DOMAIN_HANDLERS, "").split(",")) {
                    if (!handlerClass.contains(".")) handlerClass = "org.cristalise.storage."+handlerClass;

                    Logger.msg(1, "JooqClusterStorage.initialiseHandlers() - Instantiate domain handler:"+handlerClass);

                    domainHandlers.add( (JooqDomainHandler) Class.forName(handlerClass).newInstance());
                }
            }
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.error("JooqClusterStorage could not instantiate domain handler");
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }

        for(JooqDomainHandler handler: domainHandlers) handler.createTables(context);
    }

    @Override
    public void close() throws PersistencyException {
        Logger.msg(1, "JooqClusterStorage.close()");
        try {
            context.close();
        }
        catch (DataAccessException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void begin(Object locker) {
        Logger.msg(8, "JooqClusterStorage.begin() - Nothing DONE.");
    }

    @Override
    public void commit(Object locker) throws PersistencyException {
        if (autoCommit) {
            Logger.msg(1, "JooqClusterStorage.commit(DISABLED) - autoCommit:"+autoCommit);
            return;
        }

        Logger.msg(1, "JooqClusterStorage.commit()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).commit();
        } 
        catch (DataAccessException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void abort(Object locker) {
        if (autoCommit) {
            Logger.msg(1, "JooqClusterStorage.abort(DISABLED) - autoCommit:"+autoCommit);
            return;
        }

        Logger.msg(1, "JooqClusterStorage.abort()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).rollback();
        }
        catch (DataAccessException e) {
            Logger.error(e);
        }
    }

    @Override
    public short queryClusterSupport(String clusterType) {
        return READWRITE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        String lang = language.trim().toUpperCase();
        return "SQL".equals(lang) || ("SQL:"+context.dialect()).equals(lang);
    }

    @Override
    public String getName() {
        return "JOOQ:"+context.dialect()+" ClusterStorage";
    }

    @Override
    public String getId() {
        return "JOOQ:"+context.dialect();
    }

    @Override
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UnImplemented");
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        if (StringUtils.isBlank(path)) return jooqHandlers.keySet().toArray(new String[0]);

        UUID uuid = itemPath.getUUID();

        String[] pathArray   = path.split("/");
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        String   cluster     = pathArray[0];

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.getClusterContents() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));

            return handler.getNextPrimaryKeys(context, uuid, primaryKeys);
        }
        else
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        String[] pathArray   = path.split("/");
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        String   cluster     = pathArray[0];

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.get() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys:"+Arrays.toString(primaryKeys));

            C2KLocalObject obj = handler.fetch(context, uuid, primaryKeys);

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
        UUID uuid = itemPath.getUUID();
        String cluster = obj.getClusterType();

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.put() - uuid:"+uuid+" cluster:"+cluster+" path:"+obj.getClusterPath());
            handler.put(context, uuid, obj);
        }
        else
            throw new PersistencyException("Write is not supported for cluster:'"+cluster+"'");

        if (ClusterStorage.OUTCOME.equals(cluster)) {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.put(context, uuid, (Outcome)obj);
        }
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        delete(itemPath, path, null);
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        String[] pathArray   = path.split("/");
        String[] primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        String   cluster     = pathArray[0];

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.delete() - uuid:"+uuid+" cluster:"+cluster+" primaryKeys"+Arrays.toString(primaryKeys));
            handler.delete(context, uuid, primaryKeys);
        }
        else
            throw new PersistencyException("No handler found for cluster:'"+cluster+"'");

        if (ClusterStorage.OUTCOME.equals(cluster)) {
            for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.delete(context, uuid, primaryKeys);
        }
    }
}