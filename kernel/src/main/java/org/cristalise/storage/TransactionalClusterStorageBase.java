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
package org.cristalise.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.AbstractMain;
import lombok.extern.slf4j.Slf4j;

/**
 * Potentially the base class for ClusterStorage implementation without transactional support, i.e.
 * XMLClusterStorage and MemoryOnlyClusterStorage.
 * 
 * @implNote Class was created from TransactionalClusterStorage and it is very much incomplete
 */
@Slf4j
public abstract class TransactionalClusterStorageBase extends ClusterStorage{

    private HashMap<ItemPath, TransactionKey> locks;
    HashMap<TransactionKey, ArrayList<TransactionEntry>> pendingTransactions;

    // ClusterStorage storage;

    public TransactionalClusterStorageBase() throws PersistencyException {
        locks = new HashMap<ItemPath, TransactionKey>();
        pendingTransactions = new HashMap<TransactionKey, ArrayList<TransactionEntry>>();
    }

    /**
     * Retrieves the ids of the next level of a cluster
     * Checks the transaction table first to see if the caller has uncommitted changes
     * 
     * @param itemPath the item 
     * @param path the cluster path
     * @param transactionKey the transaction key
     * @return array of ids
     * @throws PersistencyException
     */
    public String[] getUncommittedClusterContents(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        List<String> uncomittedContents = new ArrayList<>();

        if (locks.containsKey(itemPath) && locks.get(itemPath).equals(transactionKey)) {
            for (TransactionEntry thisEntry : pendingTransactions.get(transactionKey)) {
                if (itemPath.equals(thisEntry.itemPath) && thisEntry.path.startsWith(path)) {
                    if (thisEntry.obj == null)
                        throw new PersistencyException("TransactionManager.get() - Cluster " + path + " has been deleted in " + itemPath +
                                " but not yet committed");
                    String content = StringUtils.substringAfterLast(thisEntry.path, "/");
                    uncomittedContents.add(content);
                }
            }
        }

        return uncomittedContents.toArray(new String[uncomittedContents.size()]);
    }

    /**
     * Public get method. Required a 'transactionKey' object for a transaction key.
     * Checks the transaction table first to see if the caller has uncommitted changes
     */
    public C2KLocalObject getUncommitted(ItemPath itemPath, String path, TransactionKey transactionKey)
            throws PersistencyException, ObjectNotFoundException
    {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        // deal out top level remote maps, if transactions aren't needed
        if (path.indexOf('/') == -1) {
            if (path.equals(ClusterType.HISTORY.getName()) && transactionKey != null) {
                return new History(itemPath, transactionKey);
            }
        }

        // check to see if the transactionKey has been modifying this cluster
        if (locks.containsKey(itemPath) && locks.get(itemPath).equals(transactionKey)) {
            ArrayList<TransactionEntry> lockerTransaction = pendingTransactions.get(transactionKey);
            for (TransactionEntry thisEntry : lockerTransaction) {
                if (itemPath.equals(thisEntry.itemPath) && path.equals(thisEntry.path)) {
                    if (thisEntry.obj == null)
                        throw new PersistencyException("TransactionManager.get() - Cluster " + path + " has been deleted in " + itemPath +
                                " but not yet committed");
                    return thisEntry.obj;
                }
            }
        }
        return null;
    }

    public void put(ItemPath itemPath, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        ArrayList<TransactionEntry> lockingTransaction = getLockingTransaction(itemPath, transactionKey);

        if (lockingTransaction == null) {
//            storage.put(itemPath, obj, transactionKey);
            locks.remove(itemPath);
        }
        else
            createTransactionEntry(itemPath, obj, null, lockingTransaction);
    }

    /**
     * Uses the put method, with null as the object value.
     */
    public void delete(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        ArrayList<TransactionEntry> lockingTransaction = getLockingTransaction(itemPath, transactionKey);

        if (lockingTransaction == null) {
//            storage.delete(itemPath, path, transactionKey);
            locks.remove(itemPath);
        }
        else
            createTransactionEntry(itemPath, null, path, lockingTransaction);
    }

    /**
     * Manages the transaction table keyed by the object 'transactionKey'.
     * If this object is null, transaction support is bypassed (so long as no lock exists on that object).
     * 
     * @param itemPath
     * @param transactionKey
     * @return the list of transaction corresponds to that lock object
     * @throws PersistencyException
     */
    private ArrayList<TransactionEntry> getLockingTransaction(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException {
        ArrayList<TransactionEntry> lockerTransaction;
        synchronized(locks) {
            // look to see if this object is already locked
            if (locks.containsKey(itemPath)) {
                // if it's this transactionKey, get the transaction list
                Object thisLocker = locks.get(itemPath);

                if (thisLocker.equals(transactionKey)) { // retrieve the transaction list
                    lockerTransaction = pendingTransactions.get(transactionKey);
                }
                else { // locked by someone else
                    throw new PersistencyException("Access denied: '"+itemPath+"' has been locked for writing by "+thisLocker);
                }
            }
            else { // no locks for this item
                if (transactionKey == null) { // lock the item until the non-transactional put/remove is complete :/
                    locks.put(itemPath, new TransactionKey(itemPath));
                    lockerTransaction = null;
                }
                else { // initialise the transaction
                    locks.put(itemPath, transactionKey);
                    lockerTransaction = new ArrayList<TransactionEntry>();
                    pendingTransactions.put(transactionKey, lockerTransaction);
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

    @Override
    public void begin(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
    }

    /**
     * Writes all pending changes to the backends.
     * 
     * @param transactionKey transaction transactionKey
     * @throws PersistencyException 
     */
    public void commit(TransactionKey transactionKey) throws PersistencyException {
        synchronized(locks) {
            ArrayList<TransactionEntry> lockerTransactions = pendingTransactions.get(transactionKey);
            // quit if no transactions are present;
            if (lockerTransactions == null) return;

            try {
                for (TransactionEntry thisEntry : lockerTransactions) {
//                    if (thisEntry.obj == null) storage.delete(thisEntry.itemPath, thisEntry.path, transactionKey);
//                    else                       storage.put(thisEntry.itemPath, thisEntry.obj, transactionKey);
                }

//                storage.commit(transactionKey);

                for (TransactionEntry thisEntry : lockerTransactions) {
                    locks.remove(thisEntry.itemPath);
                }

                pendingTransactions.remove(transactionKey);
            }
            catch (Exception e) {
//                storage.abort(transactionKey);
                log.error("commit() - Problems during transaction commit of transactionKey "+transactionKey.toString()+". Database may be in an inconsistent state.");
                dumpPendingTransactions(0);
                AbstractMain.shutdown(1);
            }
        }
    }

    /**
     * Rolls back all changes sent in the name of 'transactionKey' and unlocks the sysKeys
     * 
     * @param transactionKey transaction transactionKey
     */
    public void abort(TransactionKey transactionKey) {
        synchronized(locks) {
            if (locks.containsValue(transactionKey)) {
                for (ItemPath thisPath : locks.keySet()) {
                    if (locks.get(thisPath).equals(transactionKey))
                        locks.remove(thisPath);
                }
            }
            pendingTransactions.remove(transactionKey);
        }
    }

    public void dumpPendingTransactions(int logLevel) {
        log.error("Transaction dump. Locked Items:");

        if (locks.size() == 0) {
            log.error("  None");
        }
        else {
            for (ItemPath thisPath : locks.keySet()) {
                TransactionKey transactionKey = locks.get(thisPath);
                log.error("  "+thisPath+" locked by "+transactionKey);
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
            if (other instanceof TransactionEntry) {
                return hashCode() == ((TransactionEntry)other).hashCode();
            }
            return false;
        }
    }
}
