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

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.querying.Query;

import lombok.extern.slf4j.Slf4j;


/**
 * <p>Interface for persistency managers of entities. It allows different kernel
 * objects to be stored in different db backend. For instance, Properties may be
 * stored in LDAP, while Events, Outcomes and Viewpoints could be stored in a
 * relational database. The kernel does and needs no analytical querying of the 
 * ClusterStorages, only simple gets and puts. This may be implemented on top
 * of the storage implementation separately.
 * 
 * <p>Each item is indexed by its {@link ItemPath}, which is may be constructed from its
 * UUID, equivalent {@link SystemKey} object, or 
 * 
 * <p>Each first-level path under the Item is defined as a Cluster. Different
 * Clusters may be stored in different places. Each ClusterStorage must support
 * {@link #get(ItemPath, String)} and
 * {@link #getClusterContents(ItemPath, String)} for clusters they return
 * {@link #READ} and {@link #READWRITE} from queryClusterSupport and
 * {@link #put(ItemPath, C2KLocalObject)} and {@link #delete(ItemPath, String)}
 * for clusters they return {@link #WRITE} and {@link #READWRITE} from
 * {@link #getClusterContents(ItemPath, String)}. Operations that have not been
 * declared as not supported should throw a PersistencyException. If a
 * cluster does not exist, get should return null, and delete should return with
 * no action.
 */
@Slf4j
public abstract class ClusterStorage {
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for Cluster
     * types this storage does not support.
     */
    public static final short NONE = 0;
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for Cluster
     * types this storage can read from a database but not write. An example
     * would be pre-existing data in a database that is mapped to Items in some
     * way.
     */
    public static final short READ = 1;
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for Cluster
     * types this storage can write to a database but not read. An example would
     * be a realtime database export of data, which is transformed in an
     * unrecoverable way for use in other systems.
     */
    public static final short WRITE = 2;
    /**
     * Constant to return from {@link #queryClusterSupport(String)} for data
     * stores that CRISTAL may use for both reading and writing for the given
     * Cluster type.
     */
    public static final short READWRITE = 3;

    /**
     * Connects to the storage. It must be possible to retrieve CRISTAL local
     * objects after this method returns.
     * 
     * @param auth
     *            The Authenticator instance that the user or server logged in
     *            with.
     * @throws PersistencyException
     *             If storage initialization failed
     */
    public abstract void open() throws PersistencyException;

    /**
     * Shuts down the storage. Data must be completely written to disk before
     * this method returns, so the process can exit. No further gets or puts
     * should follow.
     * 
     * @throws PersistencyException If closing failed
     */
    public abstract void close() throws PersistencyException;

    /**
     * Informs the ClusterSorage that the Boostrap process has finished. It enables the implementation
     * to perform domain specific tasks
     * 
     * @throws PersistencyException Database error
     */
    public abstract void postBoostrap() throws PersistencyException;

    /**
     * Informs the ClusterSorage that the start server process has finished. It enables the implementation
     * to perform domain specific tasks
     * 
     * @throws PersistencyException Database error
     */
    public abstract void postStartServer() throws PersistencyException;

    /**
     * Informs the ClusterSorage that connect was done. It enables the implementation
     * to perform domain specific tasks
     * 
     * @throws PersistencyException Database error
     */
    public abstract void postConnect() throws PersistencyException;

    /**
     * Declares whether or not this ClusterStorage can read or write a
     * particular CRISTAL local object type.
     * 
     * @param clusterType The Cluster type requested
     * @return A ClusterStorage constant: NONE, READ, WRITE, or READWRITE
     */
    public abstract short queryClusterSupport(ClusterType clusterType);

    /**
     * Checks whether the storage support the given type of query or not
     * 
     * @param language type of the query (e.g. SQL/XQuery/XPath/....)
     * @return whether the Storage supports the type of the query or not
     */
    public abstract boolean checkQuerySupport(String language);

    /**
     * @return A full name of this storage for logging
     */
    public abstract String getName();

    /**
     * @return A short code for this storage for reference
     */
    public abstract String getId();

    /**
     * Utility method to find the cluster for a particular Local Object (the first part of its path)
     * 
     * @param path object path
     * @return The cluster to which it belongs
     */
    protected static ClusterType getClusterType(String path) {
        try {
            if (path == null || path.length() == 0) return ClusterType.ROOT;

            int start = path.charAt(0) == '/' ? 1 : 0;
            int end = path.indexOf('/', start + 1);

            if (end == -1) end = path.length();

            return ClusterType.getValue(path.substring(start, end));
        }
        catch (Exception ex) {
            log.error("", ex);
            return ClusterType.ROOT;
        }
    }

    /**
     * Gives the path for a local object. Varies by Cluster.
     * 
     * @param obj C2KLocalObject
     * @return Its path
     */
    public static String getPath(C2KLocalObject obj) {
        if (obj.getClusterType() == null) return null; // no storage allowed

        return obj.getClusterPath();
    }

    /**
     * Executes an SQL/OQL/XQuery/XPath/etc query in the target database. 
     * 
     * @param query the query to be executed
     * @param transactionKey the key of the transaction, can be null
     * @return the xml result of the query
     */
    public abstract String executeQuery(Query query, TransactionKey transactionKey) throws PersistencyException;

    /**
     * History and JobList based on a integer id that is incremented each tome a new Event or Job is stored
     * 
     * @param itemPath The ItemPath (UUID) of the containing Item
     * @param path the cluster patch, either equals to 'AuditTrail' or 'Job'
     * @param transactionKey the key of the transaction, can be null
     * @return returns the last found integer id (zero based), or -1 if not found
     * @throws PersistencyException When storage fails
     */
    public abstract int getLastIntegerId(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Fetches a CRISTAL local object from storage by path
     * 
     * @param itemPath The ItemPath of the containing Item
     * @param path The path of the local object
     * @param transactionKey the key of the transaction, can be null
     * @return The C2KLocalObject, or null if the object was not found
     * @throws PersistencyException when retrieval failed
     */
    public abstract C2KLocalObject get(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Stores a CRISTAL local object. The path is automatically generated.
     * 
     * @param itemPath The Item that the object will be stored under
     * @param obj The C2KLocalObject to store
     * @param transactionKey the key of the transaction, cannot be null
     * @throws PersistencyException When storage fails
     */
    public abstract void put(ItemPath itemPath, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Remove all CRISTAL local object of the given ClusterType from storage. This should be used sparingly
     * and responsibly, as it violated traceability. Objects removed in this way cannot be recovered.
     * 
     * @param itemPath The containing Item
     * @param cluster The type of the object to be removed
     * @param transactionKey the key of the transaction, cannot be null
     * @throws PersistencyException When deletion fails or is not allowed
     */
    public abstract void delete(ItemPath itemPath, ClusterType cluster, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Remove a CRISTAL local object from storage. This should be used sparingly and responsibly, 
     * as it violated traceability. Objects removed in this way cannot be recovered.
     * 
     * @param itemPath The containing Item
     * @param path The path of the object to be removed
     * @param transactionKey the key of the transaction, cannot be null
     * @throws PersistencyException When deletion fails or is not allowed
     */
    public abstract void delete(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Removes all data of an Item. This should be used sparingly
     * and responsibly, as it violated traceability. Objects removed in this way
     * are not expected to be recoverable.
     * 
     * @param itemPath The containing Item
     * @param transactionKey the key of the transaction, cannot be null
     * @throws PersistencyException When deletion fails or is not allowed
     */
    public abstract void delete(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Queries the local path below of the item and returns the possible next elements.
     * 
     * @param itemPath The Item to query
     * @param path The path within that Item to query. May be ClusterStorage.ROOT (empty String)
     * @param transactionKey the key of the transaction, can be null
     * @return A String array of the possible next path elements
     * @throws PersistencyException When an error occurred during the query
     */
    public abstract String[] getClusterContents(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException;

    /**
     * Queries the local path below the given type and returns the possible next elements.
     * 
     * @param itemPath
     * @param type
     * @param transactionKey the key of the transaction, can be null
     * @return
     * @throws PersistencyException
     */
    public String[] getClusterContents(ItemPath itemPath, ClusterType type, TransactionKey transactionKey) throws PersistencyException {
        return getClusterContents(itemPath, type.getName(), transactionKey);
    }

    /**
     * Queries the Item for the Clusters (root path elements) that are available.
     * 
     * @param itemPath the Item to query
     * @param transactionKey the key of the transaction, can be null
     * @return A ClusterType array of the possible next path elements
     * @throws PersistencyException When an error occurred during the query
     */
    public ClusterType[] getClusters(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException {
        String[] contents = getClusterContents(itemPath, "", transactionKey);
        ArrayList<ClusterType> types = new ArrayList<ClusterType>();

        for (String content : contents) {
            ClusterType type = ClusterType.getValue(content);

            if (type != null) 
                types.add(type);
            else 
                log.warn("Cannot convert content '{}' to ClusterType", content);
                //throw new PersistencyException("Cannot convert content '"+content+"' to ClusterType");
        }

        return types.toArray(new ClusterType[0]);
    }

    public abstract void begin(TransactionKey transactionKey) throws PersistencyException;

    public abstract void commit(TransactionKey transactionKey) throws PersistencyException;
    
    public abstract void abort(TransactionKey transactionKey) throws PersistencyException;


    @Override
    public String toString() {
        return getName();
    }
}
