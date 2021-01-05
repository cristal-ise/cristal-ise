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
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.agent.JobList;
import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.SoftCache;
import org.cristalise.kernel.utils.WeakCache;

import lombok.extern.slf4j.Slf4j;


/**
 * Instantiates ClusterStorages listed in properties file. All read/write requests to storage pass through this object,
 * which can query the capabilities of each declared storage, and channel requests accordingly. Transaction based.
 * It also has a memoryCache to increase performance, use 'Storage.disableCache=true' to disable it.
 */
@Slf4j
public class ClusterStorageManager {
    HashMap<String, ClusterStorage>                 allStores           = new HashMap<String, ClusterStorage>();
    String[]                                        clusterPriority     = new String[0];
    HashMap<ClusterType, ArrayList<ClusterStorage>> clusterWriters      = new HashMap<ClusterType, ArrayList<ClusterStorage>>();
    HashMap<ClusterType, ArrayList<ClusterStorage>> clusterReaders      = new HashMap<ClusterType, ArrayList<ClusterStorage>>();

    /**
     * No need for soft cache for the top level cache - the proxies and entities clear that when reaped
     */
    HashMap<ItemPath, Map<String, C2KLocalObject>> memoryCache = new HashMap<ItemPath, Map<String, C2KLocalObject>>();
    /**
     * For each transactionKey stores proxy messages to be sent during commit
     */
    Map<Object, Set<ProxyMessage>> proxyMessagesMap = new ConcurrentHashMap<Object, Set<ProxyMessage>>();
    /**
     * Stores the transactionKey for each Item updated during the transaction. It prevents concurrent writing to the same Item.
     */
    private Map<ItemPath, Object> itemLocks = new ConcurrentHashMap<ItemPath, Object>();
    /**
     * Catalog of the locked Items. It is required during commit/abor. Key is the transactionKey
     */
    private Map<Object, Set<ItemPath>> lockCatalog = new ConcurrentHashMap<Object, Set<ItemPath>>();

    /**
     * Initializes all ClusterStorage handlers listed by class name in the property "ClusterStorages"
     * This property is usually process specific, and so should be in the server/client.conf and not the connect file.
     *
     * @param auth the Authenticator to be used to initialise all the handlers
     */
    public ClusterStorageManager(Authenticator auth) throws PersistencyException {
        Object clusterStorageProp = Gateway.getProperties().getObject("ClusterStorage");

        if (clusterStorageProp == null || "".equals(clusterStorageProp)) {
            throw new PersistencyException("init() - no ClusterStorages defined. No persistency!");
        }

        ArrayList<ClusterStorage> rootStores;

        if (clusterStorageProp instanceof String) {
            rootStores = instantiateStores((String)clusterStorageProp);
        }
        else if (clusterStorageProp instanceof ArrayList<?>) {
            ArrayList<?> propStores = (ArrayList<?>)clusterStorageProp;
            rootStores = new ArrayList<ClusterStorage>();
            clusterPriority = new String[propStores.size()];
            
            for (Object thisStore : propStores) {
                if (thisStore instanceof ClusterStorage) {
                    rootStores.add((ClusterStorage)thisStore);
                }
                else {
                    throw new PersistencyException("Supplied ClusterStorage "+thisStore+" was not an instance of ClusterStorage");
                }
            }
        }
        else {
            throw new PersistencyException("Unknown class of ClusterStorage property: "+clusterStorageProp.getClass().getName());
        }

        int clusterNo = 0;
        for (ClusterStorage newStorage : rootStores) {
            newStorage.open(auth);

            log.debug("init() - Cluster storage " + newStorage.getClass().getName() + " initialised successfully.");
            allStores.put(newStorage.getId(), newStorage);
            clusterPriority[clusterNo++] = newStorage.getId();
        }

        clusterReaders.put(ClusterType.ROOT, rootStores); // all storages are queried for clusters at the root level
    }

    /**
     * 
     * @param allClusters
     * @return
     * @throws PersistencyException
     */
    public ArrayList<ClusterStorage> instantiateStores(String allClusters) throws PersistencyException {
        ArrayList<ClusterStorage> rootStores = new ArrayList<ClusterStorage>();
        StringTokenizer tok = new StringTokenizer(allClusters, ",");
        clusterPriority = new String[tok.countTokens()];

        while (tok.hasMoreTokens()) {
            ClusterStorage newStorage = null;
            String newStorageClass = tok.nextToken();
            try {
                if (!newStorageClass.contains(".")) newStorageClass = "org.cristalise.storage."+newStorageClass;
                newStorage = (ClusterStorage)(Class.forName(newStorageClass).newInstance());
            }
            catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                throw new PersistencyException("init() - The cluster storage handler class "+newStorageClass+" could not be found.");
            }
            rootStores.add(newStorage);
        }
        return rootStores;
    }

    /**
     * 
     */
    public void close() {
        for (ClusterStorage thisStorage : allStores.values()) {
            try {
                thisStorage.close();
            }
            catch (PersistencyException ex) {
                log.error("Error closing storage " + thisStorage.getName(), ex);
            }
        }
    }

    /**
     * Check which storage can execute the given query
     *
     * @param language the language of the query
     * @return the found store or null
     */
    private ClusterStorage findStorageForQuery(String language) {
        for (String element : clusterPriority) {
            ClusterStorage store = allStores.get(element);
            if (store.checkQuerySupport(language) ) return store;
        }
        return null;
    }

    /**
     * Returns the loaded storage that declare that they can handle writing or reading the specified cluster name (e.g.
     * Collection, Property) Must specify if the request is a read or a write.
     *
     * @param clusterType
     * @param forWrite whether the request is for write or read
     * @return the list of usable storages
     */
    private ArrayList<ClusterStorage> findStorages(ClusterType clusterType, boolean forWrite) {
        // choose the right cache for readers or writers
        HashMap<ClusterType, ArrayList<ClusterStorage>> cache;

        if (forWrite) cache = clusterWriters;
        else          cache = clusterReaders;

        // check to see if we've been asked to do this before
        if (cache.containsKey(clusterType)) return cache.get(clusterType);

        // not done yet, we'll have to query them all
        log.debug("findStorages() - finding storage for "+clusterType+" forWrite:"+forWrite);

        ArrayList<ClusterStorage> useableStorages = new ArrayList<ClusterStorage>();

        for (String element : clusterPriority) {
            ClusterStorage thisStorage = allStores.get(element);
            short requiredSupport = forWrite ? ClusterStorage.WRITE : ClusterStorage.READ;

            if ((thisStorage.queryClusterSupport(clusterType) & requiredSupport) == requiredSupport) {
                log.debug( "findStorages() - Got "+thisStorage.getName());
                useableStorages.add(thisStorage);
            }
        }
        cache.put(clusterType, useableStorages);
        return useableStorages;
    }

    /**
     * 
     * @param query
     * @return
     * @throws PersistencyException
     */
    public String executeQuery(Query query) throws PersistencyException {
        return executeQuery(query, null);
    }

    /**
     * Executes the Query
     *
     * @param query the Query to be executed
     * 
     * @param query
     * @param transactionKey
     * @return the xml result of the query
     * 
     * @throws PersistencyException
     */
    public String executeQuery(Query query, TransactionKey transactionKey) throws PersistencyException {
        ClusterStorage reader = findStorageForQuery(query.getLanguage());

        if (reader != null) return reader.executeQuery(query, transactionKey);
        else                throw new PersistencyException("No storage was found supporting language:"+query.getLanguage()+" query:"+query.getName());
    }

    public String[] getClusterContents(ItemPath itemPath, ClusterType type) throws PersistencyException {
        return getClusterContents(itemPath, type, null);
    }

    public String[] getClusterContents(ItemPath itemPath, ClusterType type, TransactionKey transactionKey) throws PersistencyException {
        return getClusterContents(itemPath, type.getName(), transactionKey);
    }

    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        return getClusterContents(itemPath, path, null);
    }

    /**
     * Retrieves the ids of the next level of a cluster.
     *
     * @param itemPath the current Item
     * @param path the cluster path. The leading slash is removed if exists
     * @return list of keys found in the cluster
     */
    public String[] getClusterContents(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        ArrayList<String> contents = new ArrayList<String>();
        // get all readers
        log.trace( "getClusterContents() - path:"+path);
        ArrayList<ClusterStorage> readers = findStorages(ClusterStorage.getClusterType(path), false);
        // try each in turn until we get a result
        for (ClusterStorage thisReader : readers) {
            try {
                String[] thisArr = thisReader.getClusterContents(itemPath, path, transactionKey);
                if (thisArr != null) {
                    for (int j = 0; j < thisArr.length; j++) {
                        if (!contents.contains(thisArr[j])) {
                            log.trace("getClusterContents() - "+thisReader.getName()+" reports "+thisArr[j]);
                            contents.add(thisArr[j]);
                        }
                    }
                }
            }
            catch (PersistencyException e) {
                log.debug( "getClusterContents() - reader " + thisReader.getName() +
                        " could not retrieve contents of " + itemPath + "/" + path + ": " + e.getMessage());
            }
        }

        log.trace( "getClusterContents() - Returning "+contents.size()+" elements of path:"+path);

        String[] retArr = new String[0];
        retArr = contents.toArray(retArr);
        return retArr;
    }

    /**
     * 
     * @param itemPath
     * @param path
     * @return
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException, ObjectNotFoundException {
        return get(itemPath, path, null);
    }

    /**
     * Retrieves clusters from ClusterStorages & maintains the memory cache.
     * <br>
     * There is a special case for Viewpoint. When path ends with /data it returns referenced Outcome instead of Viewpoint.
     *
     * @param itemPath current Item
     * @param path the cluster path. The leading slash is removed if exists
     * @return the C2KObject located by path
     */
    public C2KLocalObject get(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException, ObjectNotFoundException {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        // check cache first
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(itemPath);

        if (sysKeyMemCache != null) {
            synchronized(sysKeyMemCache) {
                C2KLocalObject obj = sysKeyMemCache.get(path);
                if (obj != null) {
                    log.debug( "get() - found "+itemPath+"/"+path+" in memcache");
                    return obj;
                }
            }
        }

        // special case for Viewpoint- When path ends with /data it returns referenced Outcome instead of Viewpoint
        if (path.startsWith(VIEWPOINT.getName()) && path.endsWith("/data")) {
            StringTokenizer tok = new StringTokenizer(path,"/");
            if (tok.countTokens() == 4) { // to not catch viewpoints called 'data'
                Viewpoint view = (Viewpoint)get(itemPath, path.substring(0, path.lastIndexOf("/")), transactionKey);

                if (view != null) return view.getOutcome();
                else              return null;
            }
        }

        C2KLocalObject result = null;

        // deal out top level remote maps
        if (path.indexOf('/') == -1) {
            if (path.equals(HISTORY.getName())) {
                result = new History(itemPath, transactionKey);
            }
            else if (path.equals(JOB.getName())) {
                if (itemPath instanceof AgentPath) result = new JobList((AgentPath)itemPath, transactionKey);
                else                               throw new ObjectNotFoundException("Items do not have job lists");
            }
        }

        if (result == null) {
            // else try each reader in turn until we find it
            ArrayList<ClusterStorage> readers = findStorages(ClusterStorage.getClusterType(path), false);
            for (ClusterStorage thisReader : readers) {
                try {
                    result = thisReader.get(itemPath, path, transactionKey);
                    log.debug( "get() - reading "+path+" from "+thisReader.getName() + " for item " + itemPath);
                    if (result != null) break; // got it!
                }
                catch (PersistencyException e) {
                    log.debug( "get() - reader "+thisReader.getName()+" could not retrieve "+itemPath+"/"+ path+": "+e.getMessage());
                }
            }
        }

        //No result was found after reading the list of ClusterStorages
        if (result == null) {
            throw new ObjectNotFoundException("get() - Path "+path+" not found in "+itemPath);
        }

        putInMemoryCache(itemPath, path, result);

        return result;
    }

    /**
     * Retrieves the last id of the History
     * @param itemPath current Item
     * @param path the cluster path. The leading slash is removed if exists
     * @param transactionKey
     * @return
     * @throws PersistencyException
     */
    public int getLastIntegerId(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        if (path.startsWith("/") && path.length() > 1) path = path.substring(1);

        ArrayList<ClusterStorage> readers = findStorages(HISTORY, false);
        for(ClusterStorage storage: readers) {
            return storage.getLastIntegerId(itemPath, path, transactionKey);
        }
        return -1;
    }

    /**
     * 
     * @param itemPath
     * @param obj
     * @throws PersistencyException
     */
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        put(itemPath, obj, null);
    }

    /**
     * Creates or overwrites a cluster in all writers. Used when committing transactions.
     */
    public void put(ItemPath itemPath, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        lockItem(itemPath, transactionKey);

        String path = ClusterStorage.getPath(obj);
        ArrayList<ClusterStorage> writers = findStorages(ClusterStorage.getClusterType(path), true);
        for (ClusterStorage thisWriter : writers) {
            try {
                log.debug( "put() - writing "+path+" to "+thisWriter.getName());
                thisWriter.put(itemPath, obj, transactionKey);
            }
            catch (PersistencyException e) {
                log.error("put() - writer " + thisWriter.getName() + " could not store " + itemPath + "/" + path + ": " + e.getMessage());
                throw e;
            }
        }

        putInMemoryCache(itemPath, path, obj);

        ProxyMessage message = new ProxyMessage(itemPath, path, ProxyMessage.ADDED);

        if (transactionKey != null) keepMessageForLater(message, transactionKey);
        else                        sendProxyEvent(message);
    }

    /**
     * Put the given C2KLocalObject of the Item in the memory cache. Use 'Storage.disableCache=true' to disable caching.
     *
     * @param itemPath the Item which data is going to be cached
     * @param path the cluster patch of the object
     * @param obj the C2KLocalObject to be cached
     */
    private void putInMemoryCache(ItemPath itemPath, String path, C2KLocalObject obj) {
        if (Gateway.getProperties().getBoolean("Storage.disableCache", false)) {
            log.trace("putInMemoryCache() - Cache is DISABLED");
            return;
        }

        Map<String, C2KLocalObject> sysKeyMemCache;

        if (memoryCache.containsKey(itemPath)) {
            sysKeyMemCache = memoryCache.get(itemPath);
        }
        else {
            boolean useWeak = Gateway.getProperties().getBoolean("Storage.useWeakCache", false);

            log.debug("putInMemoryCache() - Creating "+(useWeak ? "Weak" : "Strong")+" cache for item "+itemPath);

            sysKeyMemCache = useWeak ? new WeakCache<String, C2KLocalObject>() : new SoftCache<String, C2KLocalObject>(0);

            synchronized (memoryCache) {
                memoryCache.put(itemPath, sysKeyMemCache);
            }
        }

        synchronized(sysKeyMemCache) {
            sysKeyMemCache.put(path, obj);
        }

        if (log.isTraceEnabled()) dumpCacheContents();
    }

    /**
     * Deletes a cluster from all writers
     * 
     * @param itemPath - Item to delete from
     * @param path - root path to delete
     * 
     * @throws PersistencyException - when deleting fails
     */
    public void remove(ItemPath itemPath, String path) throws PersistencyException {
        remove(itemPath, path, null);
    }

    /**
     * Deletes a cluster from all writers
     * 
     * @param itemPath - Item to delete from
     * @param path - root path to delete
     * @param transactionKey - locking object
     *
     * @throws PersistencyException - when deleting fails
     */
    public void remove(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        lockItem(itemPath, transactionKey);

        ArrayList<ClusterStorage> writers = findStorages(ClusterStorage.getClusterType(path), true);
        for (ClusterStorage thisWriter : writers) {
            try {
                log.debug( "delete() - removing "+path+" from "+thisWriter.getName());
                thisWriter.delete(itemPath, path, transactionKey);
            }
            catch (PersistencyException e) {
                log.error("delete() - writer " + thisWriter.getName() + " could not delete " + itemPath + "/" + path + ": " + e.getMessage());
                throw e;
            }
        }

        if (memoryCache.containsKey(itemPath)) {
            Map<String, C2KLocalObject> itemMemCache = memoryCache.get(itemPath);
            synchronized (itemMemCache) {
                itemMemCache.remove(path);
            }
        }

        ProxyMessage message = new ProxyMessage(itemPath, path, ProxyMessage.DELETED);

        if (transactionKey != null) keepMessageForLater(message, transactionKey);
        else                        sendProxyEvent(message);
    }

    /**
     * Removes all child objects from the given path for all writers
     *
     * @param itemPath - Item to delete from
     * @param path - root path to delete
     * @param transactionKey - locking object
     *
     * @throws PersistencyException - when deleting fails
     */
    public void removeCluster(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        String[] children = getClusterContents(itemPath, path);

        for (String element : children) {
            removeCluster(itemPath, path+(path.length()>0?"/":"")+element, transactionKey);
        }

        if (children.length==0 && path.indexOf("/") > -1) {
            remove(itemPath, path, transactionKey);
        }
    }

    /**
     * Clear the cache of the given cluster content of the given Item. If itemPath is null clear the entire cache.
     * If path is empty clear the cache of the given Item.
     * 
     * @param itemPath the item for which the cache should be cleared. Can be null.
     * @param path the identifier of the cluster content to be cleared. Can be null.
     */
    public void clearCache(ItemPath itemPath, String path) {
        if (itemPath == null) {
            clearCache();
        }
        else if (path == null) {
            clearCache(itemPath);
        }
        else {
            log.debug( "clearCache() - removing "+itemPath+"/"+path);
    
            if (memoryCache.containsKey(itemPath)) {
                Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(itemPath);
                synchronized(sysKeyMemCache) {
                    for (Iterator<String> iter = sysKeyMemCache.keySet().iterator(); iter.hasNext();) {
                        String thisPath = iter.next();
                        if (thisPath.startsWith(path)) {
                            log.trace( "clearCache() - removing "+itemPath+"/"+thisPath);
                            iter.remove();
                        }
                    }
                }
            }
        }
    }

    /**
     * Clear the cache of the given Item. If itemPath is null clear the entire cache.
     * 
     * @param itemPath the Item for which the cache should be cleared. Can be null.
     */
    public void clearCache(ItemPath itemPath) {
        if (itemPath == null) {
            clearCache();
        }
        else {
            log.debug( "clearCache() - removing complete item:"+itemPath);

            if (memoryCache.containsKey(itemPath)) {
                synchronized (memoryCache) {
                    log.trace( "clearCache() - {} objects to remove for {}", memoryCache.get(itemPath).size(), itemPath);
                    memoryCache.remove(itemPath);
                }
            }
            else {
                log.debug("No objects cached for {}", itemPath);
            }
        }
    }

    /**
     * Clear entire cache
     */
    public void clearCache() {
        log.debug( "clearCache() - clearing entire cache, "+memoryCache.size()+" entities.");
        synchronized (memoryCache) {
            memoryCache.clear();
        }
    }

    /**
     * Print the content of the cache to the log
     */
    public void dumpCacheContents() {
        synchronized(memoryCache) {
            for (ItemPath itemPath : memoryCache.keySet()) {
                log.info("Cached Objects of {}", itemPath);
                Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(itemPath);
                
                try {
                    synchronized (sysKeyMemCache) {
                        for (Object name : sysKeyMemCache.keySet()) {
                            String path = (String) name;
                            try {
                                log.info("    Path {}: {}", path, sysKeyMemCache.get(path).getClass().getName());
                            }
                            catch (NullPointerException e) {
                                log.info("    Path {}: reaped", path);
                            }
                        }
                    }
                }
                catch (ConcurrentModificationException ex) {
                    log.info("Cache modified - aborting");
                }
            }
            log.info("Total number of cached entities: "+memoryCache.size());
        }
    }

    /**
     * 
     * @param transactionKey
     * @throws PersistencyException
     */
    public void begin(TransactionKey transactionKey)  throws PersistencyException {
        if (transactionKey != null) {
            if (lockCatalog.containsKey(transactionKey)) {
                throw new PersistencyException("TransactionKey '"+transactionKey+"' is already in use");
            }
            else {
                lockCatalog.put(transactionKey, new LinkedHashSet<ItemPath>());
            }
        }

        for (ClusterStorage thisStore: allStores.values()) {
            thisStore.begin(transactionKey);
        }
    }

    /**
     * 
     * @param transactionKey
     * @throws PersistencyException
     */
    public void commit(TransactionKey transactionKey) throws PersistencyException {
        for (ClusterStorage thisStore : allStores.values()) {
            thisStore.commit(transactionKey);
        }

        if (transactionKey != null) {
            if (lockCatalog.containsKey(transactionKey)) {
                for(ItemPath ip: lockCatalog.get(transactionKey)) {
                    itemLocks.remove(ip);
                }
                lockCatalog.remove(transactionKey);
            }
            else {
                throw new PersistencyException("TransactionKey '"+transactionKey+"' is unknown");
            }

            sendProxyMessages(proxyMessagesMap.remove(transactionKey));
        }
    }

    /**
     * 
     * @param transactionKey
     * @throws PersistencyException
     */
    public void abort(TransactionKey transactionKey) throws PersistencyException {
        for (ClusterStorage thisStore : allStores.values()) {
            thisStore.abort(transactionKey);
        }

        if (transactionKey != null) {
            if (lockCatalog.containsKey(transactionKey)) {
                for(ItemPath ip: lockCatalog.get(transactionKey)) {
                    itemLocks.remove(ip);
                }
                lockCatalog.remove(transactionKey);
            }
            else {
                throw new PersistencyException("TransactionKey '"+transactionKey+"' is unknown");
            }

            proxyMessagesMap.remove(transactionKey);
        }
    }

    /**
     * 
     * @throws PersistencyException
     */
    public void postConnect() throws PersistencyException {
        for (ClusterStorage thisStore : allStores.values()) {
            thisStore.postConnect();
        }
    }

    /**
     * 
     * @throws PersistencyException
     */
    public void postBoostrap() throws PersistencyException {
        for (ClusterStorage thisStore : allStores.values()) {
            thisStore.postBoostrap();
        }
    }

    /**
     * 
     * @throws PersistencyException
     */
    public void postStartServer() throws PersistencyException{
        for (ClusterStorage thisStore : allStores.values()) {
            thisStore.postStartServer();
        }
    }

    /**
     * 
     * @param message
     * @param transactionKey
     */
    private void keepMessageForLater(ProxyMessage message, TransactionKey transactionKey){
        Set<ProxyMessage> set = proxyMessagesMap.get(transactionKey);
        if (set == null){
            set = new HashSet<ProxyMessage>();
            proxyMessagesMap.put(transactionKey, set);
        }
        set.add(message);
    }

    /**
     * 
     * @param messageSet
     */
    private void sendProxyMessages(Set<ProxyMessage> messageSet){
        if (messageSet != null) {
            for (ProxyMessage message: messageSet) sendProxyEvent(message);
        }
    }

    /**
     * 
     * @param message
     */
    private void sendProxyEvent(ProxyMessage message){
        if(Gateway.getProxyServer() != null) {
            Gateway.getProxyServer().sendProxyEvent(message);
        }
        else {
            log.warn("sendProxyEvent: ProxyServer is null - Proxies are not notified of this event");
        }
    }

    private void lockItem(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException {
        synchronized(itemLocks) {
            // look to see if this object is already locked
            if (itemLocks.containsKey(itemPath)) {
                Object thisLocker = itemLocks.get(itemPath);
                
                if (transactionKey != null && thisLocker.equals(transactionKey)) {
                    // nothing to do
                    lockCatalog.get(transactionKey).add(itemPath);
                }
                else {
                    // the item is already locked by someone else
                    // at this point a semaphore could be used to block the thread, but this could create deadlocks
                    throw new PersistencyException("Access denied for '"+transactionKey+"': '"+itemPath+
                                                   "' has been locked for writing by '"+thisLocker+"'");
                }
            }
            else { // no locks for this item
                if (transactionKey == null) {
                    // nothing to do, all the writer storages must be in autocommit mode
                }
                else { // add the lock
                    Set<ItemPath> lockEntry = lockCatalog.get(transactionKey);
                    if (lockEntry == null) {
                        throw new PersistencyException("'"+itemPath+"' - No lockentry was found for transactionKey:"+transactionKey);
                    }

                    lockEntry.add(itemPath);
                    itemLocks.put(itemPath, transactionKey);
                }
            }
        }
    }
}
