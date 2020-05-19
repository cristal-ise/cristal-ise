/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.persistency;

import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.JobList;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;

import lombok.extern.slf4j.Slf4j;

/**
 * Delegates the transaction management to the underlying backends. No local cashes are required
 */
@Slf4j
public class DelegatingTransactionManager implements TransactionManager {

    ClusterStorageManager storage;

    public DelegatingTransactionManager(Authenticator auth) throws PersistencyException {
        storage = new ClusterStorageManager(auth);
    }

    public boolean hasPendingTransactions() {
        return false;
    }

    public ClusterStorageManager getDb() {
        return storage;
    }

    /**
     * Closing will abort all transactions
     */
    public void close() {
        log.info("close() - Closing storages");
        storage.close();
    }

    public String executeQuery(Query query) throws PersistencyException {
        return executeQuery(query, null);
    }

    /**
     * 
     * @param query
     * @return
     * @throws PersistencyException
     */
    public String executeQuery(Query query, Object locker) throws PersistencyException {
        return storage.executeQuery(query, locker);
    }

    /**
     * Retrieves the ids of the root level of a cluster
     * 
     * @param itemPath the item 
     * @param type the type of the cluster
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, ClusterType type) throws PersistencyException {
        return getClusterContents(itemPath, type, null);
    }

    /**
     * Retrieves the ids of the root level of a cluster
     * 
     * @param itemPath the item 
     * @param type the type of the cluster
     * @param locker the transaction key
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, ClusterType type, Object locker) throws PersistencyException {
        return getClusterContents(itemPath, type.getName(), locker);
    }

    /**
     * Retrieves the ids of the next level of a cluster
     * 
     * @param itemPath the item 
     * @param path the cluster path
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        return getClusterContents(itemPath, path, null);
    }

    /**
     * Retrieves the ids of the next level of a cluster
     * 
     * @param itemPath the item 
     * @param path the cluster path
     * @param locker the transaction key
     * @return array of ids
     * @throws PersistencyException
     */
    synchronized public String[] getClusterContents(ItemPath itemPath, String path, Object locker) 
            throws PersistencyException
    {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);
        return storage.getClusterContents(itemPath, path, locker);
    }

    /**
     * Public get method. Required a 'locker' object for a transaction key.
     */
    synchronized public C2KLocalObject get(ItemPath itemPath, String path, Object locker)
            throws PersistencyException, ObjectNotFoundException
    {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        // deal out top level remote maps first
        if (path.indexOf('/') == -1) {
            if (path.equals(HISTORY.getName())) {
                return new History(itemPath, locker);
            }
            else if (path.equals(JOB.getName())) {
                if (itemPath instanceof AgentPath) {
                    return new JobList((AgentPath)itemPath, locker);
                }
                else{
                    throw new ObjectNotFoundException("get() - Items do not have job lists");
                }
            }
        }
        return storage.get(itemPath, path);
    }

    synchronized public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        storage.put(itemPath, obj);
    }

    /**
     * 
     */
    synchronized public void remove(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        storage.remove(itemPath, path);
    }

    /**
     * Removes all child objects from the given path
     *
     * @param itemPath - Item to delete from
     * @param path - root path to delete
     * @param locker - locking object
     *
     * @throws PersistencyException - when deleting fails
     */
    public void removeCluster(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        String[] children = getClusterContents(itemPath, path);

        for (String element : children) {
            removeCluster(itemPath, path+(path.length() > 0 ? "/" : "") + element, locker);
        }

        if (children.length==0 && path.indexOf("/") > -1) {
            remove(itemPath, path, locker);
        }
    }

    @Override
    synchronized public void begin(Object locker) throws PersistencyException {
        try {
            storage.begin(locker);
        }
        catch (Exception e) {
            storage.abort(locker);
            log.error("begin() - Database may be in an inconsistent state, calling shutdown()", e);
            dumpPendingTransactions(0);
            AbstractMain.shutdown(1);
        }
    }

    /**
     * Trigger commit of backends
     * 
     * @param locker transaction locker
     * @throws PersistencyException 
     */
    synchronized public void commit(Object locker) throws PersistencyException {
        try {
            storage.commit(locker);
        }
        catch (Exception e) {
            storage.abort(locker);
            log.error("commit() - Problems during transaction commit of locker " + locker
                    + ". Database may be in an inconsistent state.");
            dumpPendingTransactions(0);
            AbstractMain.shutdown(1);
        }
    }

    /**
     * Triggers abort of backends
     * 
     * @param locker transaction locker
     */
    synchronized public void abort(Object locker) {
        try {
            storage.abort(locker);
        }
        catch (Exception e) {
            log.error("abort() - Problems during transaction abort of locker " + locker
                    + ". Database may be in an inconsistent state.");
            dumpPendingTransactions(0);
            AbstractMain.shutdown(1);
        }
    }

    public void clearCache(ItemPath itemPath, String path) {
        if (itemPath == null)  storage.clearCache();
        else if (path == null) storage.clearCache(itemPath);
        else                   storage.clearCache(itemPath, path);
    }

    public void dumpPendingTransactions(int logLevel) {
        log.warn("dumpPendingTransactions() - NOTHING to dump");
    }

    /**
     * Propagate Gateway connect has finished hook to the storages
     */
    public void postConnect() throws PersistencyException {
        storage.postConnect();
    }

    /**
     * Propagate Bootstrap has finished hook to the storages
     */
    public void postBoostrap() throws PersistencyException{
        storage.postBoostrap();
    }

    /**
     * Propagate start server has finished hook to the storages
     */
    public void postStartServer() throws PersistencyException {
        storage.postStartServer();
    }

    @Override
    public int getLastIntegerId(ItemPath itemPath, String path) throws PersistencyException {
        return storage.getLastIntegerId(itemPath, path);
    }
}
