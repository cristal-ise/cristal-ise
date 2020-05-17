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

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.querying.Query;

public interface TransactionManager {

    public ClusterStorageManager getDb();

    /**
     * Closing will abort all transactions
     */
    public void close();

    /**
     * 
     * @param query
     * @return
     * @throws PersistencyException
     */
    public String executeQuery(Query query) throws PersistencyException;

    /**
     * Retrieves the ids of the root level of a cluster
     * 
     * @param itemPath the item 
     * @param type the type of the cluster
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, ClusterType type) throws PersistencyException;

    /**
     * Retrieves the ids of the root level of a cluster
     * 
     * @param itemPath the item 
     * @param type the type of the cluster
     * @param locker the transaction key
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, ClusterType type, Object locker) throws PersistencyException;

    /**
     * Retrieves the ids of the next level of a cluster
     * 
     * @param itemPath the item 
     * @param path the cluster path
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException;

    /**
     * Retrieves the ids of the next level of a cluster
     * Checks the transaction table first to see if the caller has uncommitted changes
     * 
     * @param itemPath the item 
     * @param path the cluster path
     * @param locker the transaction key
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, String path, Object locker) throws PersistencyException;

    /**
     * Public get method. Required a 'locker' object for a transaction key.
     * Checks the transaction table first to see if the caller has uncommitted changes
     */
    public C2KLocalObject get(ItemPath itemPath, String path, Object locker)
            throws PersistencyException, ObjectNotFoundException;

    /**
     * 
     * @param itemPath
     * @param obj
     * @param locker
     * @throws PersistencyException
     */
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException;

    /**
     * Uses the put method, with null as the object value.
     */
    public void remove(ItemPath itemPath, String path, Object locker) throws PersistencyException;
    
    /**
     * Removes all child objects from the given path
     *
     * @param itemPath - Item to delete from
     * @param path - root path to delete
     * @param locker - locking object
     *
     * @throws PersistencyException - when deleting fails
     */
    public void removeCluster(ItemPath itemPath, String path, Object locker) throws PersistencyException;

    /**
     * Informs backends about the begining of transacaion
     * 
     * @param locker transaction key
     * @throws PersistencyException
     */
    public void begin(Object locker) throws PersistencyException;

    /**
     * Writes all pending changes to the backends.
     * 
     * @param locker transaction key
     * @throws PersistencyException 
     */
    public void commit(Object locker) throws PersistencyException;

    /**
     * Rolls back all changes sent in the name of 'locker' and unlocks the sysKeys
     * 
     * @param locker transaction key
     */
    public void abort(Object locker);

    /**
     * 
     * @param itemPath
     * @param path
     */
    public void clearCache(ItemPath itemPath, String path);

    /**
     * 
     * @param logLevel
     */
    public void dumpPendingTransactions(int logLevel);

    /**
     * Propagate Gateway connect has finished hook to the storages
     */
    public void postConnect() throws PersistencyException;

    /**
     * Propagate Bootstrap has finished hook to the storages
     */
    public void postBoostrap() throws PersistencyException;

    /**
     * Propagate start server has finished hook to the storages
     */
    public void postStartServer() throws PersistencyException;

    /**
     * History and JobList based on a integer id that is incremented each tome a new Event or Job is stored
     * 
     * @param itemPath The ItemPath (UUID) of the containing Item
     * @param path the cluster patch, either equals to 'AuditTrail' or 'Job'
     * @return returns the last found integer id (zero based), or -1 if not found
     * @throws PersistencyException When storage fails
     */
    public int getLastIntegerId(ItemPath itemPath, String path) throws PersistencyException;
}
