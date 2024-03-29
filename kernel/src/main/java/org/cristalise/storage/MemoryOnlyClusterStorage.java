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
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.querying.Query;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MemoryOnlyClusterStorage extends ClusterStorage {

    Map<ItemPath, Map<String, C2KLocalObject>> memoryCache = new ConcurrentHashMap<ItemPath, Map<String, C2KLocalObject>>();

    public void clear() {
        memoryCache.clear();
    }
    /**
     * 
     */
    public MemoryOnlyClusterStorage() {
        memoryCache = new HashMap<ItemPath, Map<String,C2KLocalObject>>();
    }

    @Override
    public void open() throws PersistencyException {

    }

    @Override
    public void close() throws PersistencyException {
    }

    @Override
    public boolean checkQuerySupport(String language) {
        log.warn("MemoryOnlyClusterStorage DOES NOT Support any query");
        return false;
    }

    @Override
    public short queryClusterSupport(ClusterType clusterType) {
        return ClusterStorage.READWRITE;
    }

    @Override
    public String getName() {
        return "Memory Cache";
    }

    @Override
    public String getId() {
        return "Memory Cache";
    }

    @Override
    public String executeQuery(Query query, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnction");
    }

    @Override
    public C2KLocalObject get(ItemPath thisItem, String path, TransactionKey transactionKey)
            throws PersistencyException
    {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
    
        if (sysKeyMemCache != null) return sysKeyMemCache.get(path);

        return null;
    }

    @Override
    public void put(ItemPath thisItem, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        // create item cache if not present
        Map<String, C2KLocalObject> sysKeyMemCache;
        synchronized (memoryCache) {
            if (memoryCache.containsKey(thisItem))
                sysKeyMemCache = memoryCache.get(thisItem);
            else {
                sysKeyMemCache = new HashMap<String, C2KLocalObject>();
                memoryCache.put(thisItem, sysKeyMemCache);
            }
        }

        // store object in the cache
        String path = ClusterStorage.getPath(obj);
        synchronized(sysKeyMemCache) {
            sysKeyMemCache.put(path, obj);
        }
    }

    private void removeCluster(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        String[] children = getClusterContents(itemPath, path, transactionKey);

        for (String element : children) {
            removeCluster(itemPath, path+(path.length()>0?"/":"")+element, transactionKey);
        }

        if (children.length == 0 && path.indexOf("/") > -1) {
            delete(itemPath, path, transactionKey);
        }
    }

    @Override
    public void delete(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException {
        removeCluster(itemPath, "", transactionKey);
    }

    @Override
    public void delete(ItemPath thisItem, ClusterType cluster, TransactionKey transactionKey) throws PersistencyException {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
        if (sysKeyMemCache != null) {
            synchronized (sysKeyMemCache) {
                sysKeyMemCache.keySet().removeIf(key -> key.startsWith(cluster.getName()));
                if (sysKeyMemCache.isEmpty()) {
                    memoryCache.remove(thisItem);
                }
            }
        }
    }

    @Override
    public void delete(ItemPath thisItem, String path, TransactionKey transactionKey) throws PersistencyException {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
        if (sysKeyMemCache != null) {
            synchronized (sysKeyMemCache) {
                if (sysKeyMemCache.containsKey(path)) {
                    sysKeyMemCache.remove(path);
                    if (sysKeyMemCache.isEmpty()) {
                        memoryCache.remove(thisItem);
                    }
                }
            }
        }
    }

    @Override
    public String[] getClusterContents(ItemPath thisItem, String path, TransactionKey transactionKey) throws PersistencyException {
        Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
        ArrayList<String> result = new ArrayList<String>();
        if (sysKeyMemCache != null) {
            while (path.endsWith("/")) path = path.substring(0,path.length()-1);
            path = path+"/";
            for (String thisPath : sysKeyMemCache.keySet()) {
                if (thisPath.startsWith(path)) {
                    String end = thisPath.substring(path.length());
                    int slash = end.indexOf('/');
                    String suffix = slash>-1?end.substring(0, slash):end;
                    if (!result.contains(suffix)) result.add(suffix);
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, ClusterType type, TransactionKey transactionKey) throws PersistencyException {
        return getClusterContents(itemPath, type.getName(), transactionKey);
    }

    public void dumpContents(ItemPath thisItem) {
        synchronized(memoryCache) {
            log.info("Cached Objects of Entity "+thisItem);
            Map<String, C2KLocalObject> sysKeyMemCache = memoryCache.get(thisItem);
            if (sysKeyMemCache == null) {
                log.info("No cache found");
                return;
            }
            try {
                synchronized(sysKeyMemCache) {
                    for (Object name : sysKeyMemCache.keySet()) {
                        String path = (String) name;
                        try {
                            log.info("    Path "+path+": "+sysKeyMemCache.get(path).getClass().getName());
                        } catch (NullPointerException e) {
                            log.info("    Path "+path+": reaped");
                        }
                    }
                }
            } catch (ConcurrentModificationException ex) {
                log.info("Cache modified - aborting");
            }
        }
        log.info("Total number of cached entities: "+memoryCache.size());
    }

    @Override
    public void postBoostrap() {
        //nothing to be done
    }

    @Override
    public void postStartServer() {
        //nothing to be done
    }

    @Override
    public void postConnect() {
        //nothing to be done
    }

    @Override
    public int getLastIntegerId(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        int lastId = -1;
        try {
            String[] keys = getClusterContents(itemPath, path, transactionKey);
            for (String key : keys) {
                int newId = Integer.parseInt(key);
                lastId = newId > lastId ? newId : lastId;
            }
        }
        catch (NumberFormatException e) {
           log.error("Error parsing keys", e);
           throw new PersistencyException(e.getMessage());
        }

        return lastId;
    }

    @Override
    public void begin(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void commit(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void abort(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }
}
