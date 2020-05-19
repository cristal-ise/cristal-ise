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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
 * 
 */
@Slf4j
public class DelegatingTransactionManager implements TransactionManager {

    private HashMap<ItemPath, Object> locks;
    HashMap<Object, ArrayList<TransactionEntry>> pendingTransactions;
    ClusterStorageManager storage;

    public DelegatingTransactionManager(Authenticator auth) throws PersistencyException {
        storage = new ClusterStorageManager(auth);
        locks = new HashMap<ItemPath, Object>();
        pendingTransactions = new HashMap<Object, ArrayList<TransactionEntry>>();
    }

    public boolean hasPendingTransactions() {
        return pendingTransactions.size() > 0;
    }

    public ClusterStorageManager getDb() {
        return storage;
    }

    /**
     * Closing will abort all transactions
     */
    public void close() {
        if (pendingTransactions.size() != 0) {
            log.error("There were pending transactions on shutdown. All changes were lost.");
            dumpPendingTransactions(0);
        }

        log.info("Transaction Manager: Closing storages");
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
     * Checks the transaction table first to see if the caller has uncommitted changes
     * 
     * @param itemPath the item 
     * @param path the cluster path
     * @param locker the transaction key
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        List<String> uncomittedContents = new ArrayList<>();

        if (locks.containsKey(itemPath) && locks.get(itemPath).equals(locker)) {
            for (TransactionEntry thisEntry : pendingTransactions.get(locker)) {
                if (itemPath.equals(thisEntry.itemPath) && thisEntry.path.startsWith(path)) {
                    if (thisEntry.obj == null)
                        throw new PersistencyException("TransactionManager.get() - Cluster " + path + " has been deleted in " + itemPath +
                                " but not yet committed");
                    String content = StringUtils.substringAfterLast(thisEntry.path, "/");
                    uncomittedContents.add(content);
                }
            }
        }

        return ArrayUtils.addAll(
                storage.getClusterContents(itemPath, path, locker), 
                uncomittedContents.toArray(new String[uncomittedContents.size()])
        );
    }

    /**
     * Public get method. Required a 'locker' object for a transaction key.
     * Checks the transaction table first to see if the caller has uncommitted changes
     */
    public C2KLocalObject get(ItemPath itemPath, String path, Object locker)
            throws PersistencyException, ObjectNotFoundException
    {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        // deal out top level remote maps, if transactions aren't needed
        if (path.indexOf('/') == -1) {
            if (path.equals(ClusterType.HISTORY) && locker != null) {
                return new History(itemPath, locker);
            }
            else if (path.equals(ClusterType.JOB) && locker != null) {
                if (itemPath instanceof AgentPath) return new JobList((AgentPath)itemPath, locker);
                else                               throw new ObjectNotFoundException("TransactionManager.get() - Items do not have job lists");
            }
        }

        // check to see if the locker has been modifying this cluster
        if (locks.containsKey(itemPath) && locks.get(itemPath).equals(locker)) {
            ArrayList<TransactionEntry> lockerTransaction = pendingTransactions.get(locker);
            for (TransactionEntry thisEntry : lockerTransaction) {
                if (itemPath.equals(thisEntry.itemPath) && path.equals(thisEntry.path)) {
                    if (thisEntry.obj == null)
                        throw new PersistencyException("TransactionManager.get() - Cluster " + path + " has been deleted in " + itemPath +
                                " but not yet committed");
                    return thisEntry.obj;
                }
            }
        }
        return storage.get(itemPath, path);
    }

    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        ArrayList<TransactionEntry> lockingTransaction = getLockingTransaction(itemPath, locker);

        if (lockingTransaction == null) {
            storage.put(itemPath, obj);
            locks.remove(itemPath);
        }
        else
            createTransactionEntry(itemPath, obj, null, lockingTransaction);
    }

    /**
     * Uses the put method, with null as the object value.
     */
    public void remove(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        ArrayList<TransactionEntry> lockingTransaction = getLockingTransaction(itemPath, locker);

        if (lockingTransaction == null) {
            storage.remove(itemPath, path);
            locks.remove(itemPath);
        }
        else
            createTransactionEntry(itemPath, null, path, lockingTransaction);
    }

    /**
     * Manages the transaction table keyed by the object 'locker'.
     * If this object is null, transaction support is bypassed (so long as no lock exists on that object).
     * 
     * @param itemPath
     * @param locker
     * @return the list of transaction corresponds to that lock object
     * @throws PersistencyException
     */
    private ArrayList<TransactionEntry> getLockingTransaction(ItemPath itemPath, Object locker) throws PersistencyException {
        ArrayList<TransactionEntry> lockerTransaction;
        synchronized(locks) {
            // look to see if this object is already locked
            if (locks.containsKey(itemPath)) {
                // if it's this locker, get the transaction list
                Object thisLocker = locks.get(itemPath);
                
                if (thisLocker.equals(locker)) // retrieve the transaction list
                    lockerTransaction = pendingTransactions.get(locker);
                else // locked by someone else
                    throw new PersistencyException("Access denied: '"+itemPath+"' has been locked for writing by "+thisLocker);
            }
            else { // no locks for this item
                if (locker == null) { // lock the item until the non-transactional put/remove is complete :/
                    locks.put(itemPath, new Object());
                    lockerTransaction = null;
                }
                else { // initialise the transaction
                    locks.put(itemPath, locker);
                    lockerTransaction = new ArrayList<TransactionEntry>();
                    pendingTransactions.put(locker, lockerTransaction);
                }
            }
        }
        return lockerTransaction;
    }

    /**
     * Create the new entry in the transaction table.
     * equals() in TransactionEntry only compares sysKey and path, so we can use contains()
     * in ArrayList to look for existing entries for this cluster and overwrite them.
     * 
     * @param itemPath
     * @param obj
     * @param lockerTransaction
     * @throws PersistencyException 
     */
    private void createTransactionEntry(ItemPath itemPath, C2KLocalObject obj, String  path, ArrayList<TransactionEntry> lockerTransaction) throws PersistencyException {
        TransactionEntry newEntry;

        if (obj != null)      newEntry = new TransactionEntry(itemPath, obj);
        else if(path != null) newEntry = new TransactionEntry(itemPath, path);
        else                  throw new PersistencyException("");

        if (lockerTransaction.contains(newEntry)) lockerTransaction.remove(newEntry);

        lockerTransaction.add(newEntry);
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
        
        for (String element : children)
            removeCluster(itemPath, path+(path.length()>0?"/":"")+element, locker);
        
        if (children.length==0 && path.indexOf("/") > -1)
            remove(itemPath, path, locker);
    }

    @Override
    public void begin(Object locker) throws PersistencyException {
        synchronized(locks) {
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
    }

    /**
     * Writes all pending changes to the backends.
     * 
     * @param locker transaction locker
     * @throws PersistencyException 
     */
    public void commit(Object locker) throws PersistencyException {
        synchronized(locks) {
            ArrayList<TransactionEntry> lockerTransactions = pendingTransactions.get(locker);
            // quit if no transactions are present;
            if (lockerTransactions == null)
                return;

            try {
                storage.begin(locker);

                for (TransactionEntry thisEntry : lockerTransactions) {
                    if (thisEntry.obj == null)
                        storage.remove(thisEntry.itemPath, thisEntry.path, locker);
                    else
                        storage.put(thisEntry.itemPath, thisEntry.obj, locker);
                }

                storage.commit(locker);

                for (TransactionEntry thisEntry : lockerTransactions) {
                    locks.remove(thisEntry.itemPath);
                }

                pendingTransactions.remove(locker);
            }
            catch (Exception e) {
                storage.abort(locker);
                log.error("commit() - Problems during transaction commit of locker "+locker.toString()+". Database may be in an inconsistent state.");
                dumpPendingTransactions(0);
                AbstractMain.shutdown(1);
            }
        }
    }

    /**
     * Rolls back all changes sent in the name of 'locker' and unlocks the sysKeys
     * 
     * @param locker transaction locker
     */
    public void abort(Object locker) {
        synchronized(locks) {
            if (locks.containsValue(locker)) {
                for (ItemPath thisPath : locks.keySet()) {
                    if (locks.get(thisPath).equals(locker))
                        locks.remove(thisPath);
                }
            }
            pendingTransactions.remove(locker);
        }
    }

    public void clearCache(ItemPath itemPath, String path) {
        if (itemPath == null)  storage.clearCache();
        else if (path == null) storage.clearCache(itemPath);
        else                   storage.clearCache(itemPath, path);
    }

    public void dumpPendingTransactions(int logLevel) {
        log.error("Transaction dump. Locked Items:");

        if (locks.size() == 0) {
            log.error("  None");
        }
        else {
            for (ItemPath thisPath : locks.keySet()) {
                Object locker = locks.get(thisPath);
                log.error("  "+thisPath+" locked by "+locker);
            }
        }

        log.error("Open transactions:");
        if (pendingTransactions.size() == 0) {
            log.error("  None");
        }
        else {
            for (Object thisLocker : pendingTransactions.keySet()) {
                log.error("  Transaction owner:"+thisLocker);

                ArrayList<TransactionEntry> entries = pendingTransactions.get(thisLocker);
                for (TransactionEntry thisEntry : entries) {
                    log.error("    "+thisEntry.toString());
                }
            }
        }
    }

    /**
     * Used in the transaction table to store details of a put until commit
     */
    static class TransactionEntry {
        public ItemPath itemPath;
        public String path;
        public C2KLocalObject obj;
        public TransactionEntry(ItemPath itemPath, C2KLocalObject obj) {
            this.itemPath = itemPath;
            this.path = ClusterStorage.getPath(obj);
            this.obj = obj;
        }

        public TransactionEntry(ItemPath itemPath, String path) {
            this.itemPath = itemPath;
            this.path = path;
            this.obj = null;
        }

        @Override
        public String toString() {
            StringBuffer report = new StringBuffer();
         
            if (obj == null) report.append("Delete");
            else             report.append("Put "+obj.getClass().getName());

            report.append(" at ").append(path).append(" in ").append(itemPath);
            return report.toString();

        }

        @Override
        public int hashCode() {
            return itemPath.hashCode()*path.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof TransactionEntry)
                return hashCode() == ((TransactionEntry)other).hashCode();
            return false;
        }
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