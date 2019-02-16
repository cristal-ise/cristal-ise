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

import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_DOMAIN_HANDLERS;
import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_AUTOCOMMIT;
import static org.cristalise.storage.jooqdb.JooqHandler.JOOQ_DISABLE_DOMAIN_CREATE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
import org.jooq.impl.DefaultConnectionProvider;

/**
 * Implementation of the {@link TransactionalClusterStorage} based on <a>http://www.jooq.org/</a>}
 */
public class JooqClusterStorage extends TransactionalClusterStorage {

    protected DSLContext context;
    protected Boolean autoCommit;

    protected HashMap<ClusterType, JooqHandler> jooqHandlers   = new HashMap<ClusterType, JooqHandler>();
    protected List<JooqDomainHandler>           domainHandlers = new ArrayList<JooqDomainHandler>();

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        context = JooqHandler.connect();

        autoCommit = Gateway.getProperties().getBoolean(JOOQ_AUTOCOMMIT, false);

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

        for (JooqHandler handler: jooqHandlers.values()) handler.createTables(context);

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
            for (JooqDomainHandler handler: domainHandlers) handler.createTables(context);
        }
    }

    @Override
    public void close() throws PersistencyException {
        Logger.msg(1, "JooqClusterStorage.close()");
        try {
            context.close();
        }
        catch (Exception e) {
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
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.commit(context, locker);

        if (autoCommit) {
            Logger.msg(1, "JooqClusterStorage.commit(DISABLED) - autoCommit:"+autoCommit);
            return;
        }

        Logger.msg(1, "JooqClusterStorage.commit()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).commit();
        }
        catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public void abort(Object locker) {
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.abort(context, locker);

        if (autoCommit) {
            Logger.msg(1, "JooqClusterStorage.abort(DISABLED) - autoCommit:"+autoCommit);
            return;
        }

        Logger.msg(1, "JooqClusterStorage.abort()");
        try {
            ((DefaultConnectionProvider)context.configuration().connectionProvider()).rollback();
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
    public ClusterType[] getClusters(ItemPath itemPath) throws PersistencyException {
        ArrayList<ClusterType> result = new ArrayList<ClusterType>();

        for (ClusterType type:jooqHandlers.keySet()) {
            if (jooqHandlers.get(type).exists(context, itemPath.getUUID())) result.add(type);
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

            return handler.getNextPrimaryKeys(context, uuid, primaryKeys);
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
        ClusterType cluster = obj.getClusterType();

        JooqHandler handler = jooqHandlers.get(cluster);

        if (handler != null) {
            Logger.msg(5, "JooqClusterStorage.put() - uuid:"+uuid+" cluster:"+cluster+" path:"+obj.getClusterPath());
            handler.put(context, uuid, obj);
        }
        else {
            throw new PersistencyException("Write is not supported for cluster:'"+cluster+"'");
        }

        // Trigger all registered handlers to update domain specific tables
        for (JooqDomainHandler domainHandler : domainHandlers) domainHandler.put(context, uuid, obj, locker);
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        delete(itemPath, path, null);
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        UUID uuid = itemPath.getUUID();

        String[]    pathArray   = path.split("/");
        String[]    primaryKeys = Arrays.copyOfRange(pathArray, 1, pathArray.length);
        ClusterType cluster     = ClusterType.getValue(pathArray[0]);

        JooqHandler handler = jooqHandlers.get(cluster);

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
