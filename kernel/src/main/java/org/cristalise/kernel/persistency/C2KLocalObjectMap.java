package org.cristalise.kernel.persistency;

import static org.cristalise.kernel.persistency.ClusterType.ATTACHMENT;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A simple wrapper for ClusterStorage API to provide Map like access to cluster data. 
 * It based on the cache of the ClusterStorage, i.e. it does no store any data.
 *
 * @param <V>
 */
@Slf4j @Getter
public class C2KLocalObjectMap<V extends C2KLocalObject> implements Map<String, V> {

    protected ItemPath itemPath;
    protected ClusterType clusterType;
    private ClusterStorageManager storage;

    /**
     * Use it participate in a transaction. Can be null
     */
    TransactionKey transactionKey;

    public C2KLocalObjectMap(ItemPath item, ClusterType cluster) {
        this(item, cluster, null);
    }

    public C2KLocalObjectMap(ItemPath item, ClusterType cluster, TransactionKey transKey) {
        itemPath = item;
        transactionKey = transKey;
        clusterType = cluster;

        storage = Gateway.getStorage();
    }

    public int getLastId() {
        try {
            return storage.getLastIntegerId(itemPath, clusterType.getName(), transactionKey);
        }
        catch (PersistencyException e) {
            log.error("getLastId() - {}/{}", itemPath, clusterType, e);
        }
        return -1;
    }

    private String getFullKey(Object key) {
        String path = (String)key;
        return clusterType + (path.length() > 0 ? "/" : "") + path;
    }

    protected Set<String> loadKeys(String path) throws PersistencyException {
        Set<String> keys = new HashSet<>();
        String fullKey = getFullKey(path);
        int count = StringUtils.countMatches(fullKey, '/');

        String[] children = storage.getClusterContents(itemPath, fullKey, transactionKey);

        if (((clusterType == PROPERTY  || clusterType == LIFECYCLE || clusterType == JOB || clusterType == HISTORY) && count == 0) ||
            ((clusterType == VIEWPOINT || clusterType == COLLECTION) && count == 1) ||
            ((clusterType == OUTCOME   || clusterType == ATTACHMENT) && count == 2))
        {
            for (String subKey : children) {
                fullKey = path + (path.length() > 0 ? "/" : "") + subKey;
                keys.add(fullKey);
            }
        }
        else {
            for (String element : children) {
                fullKey = path + (path.length() > 0 ? "/" : "") + element;
                keys.addAll(loadKeys(fullKey));
            }
        }
        return keys;
    }

    @Override
    public boolean containsKey(Object key) {
        return keySet().contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return containsKey(((C2KLocalObject)value).getClusterPath());
    }

    @Override
    public synchronized Set<String> keySet() {
        try {
            Set<String> keys = loadKeys("");
            log.debug("keySet() - Returning #{} keys of cluster:{}/{}", keys.size(), itemPath, clusterType);
            return keys;
        }
        catch (PersistencyException e) {
            log.error("keySet() - {}/{}", itemPath, clusterType, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        String fullKey = getFullKey(key);
        try {
            return (V) storage.get(itemPath, fullKey, transactionKey);
        }
        catch (PersistencyException e) {
            log.error("get() - {}/{}", itemPath, fullKey, e);
            throw new IllegalArgumentException(e);
        }
        catch (ObjectNotFoundException e) {
            log.trace("get() - could not get cluster:{}/{}", itemPath, fullKey, e);
        }
        return null;
    }

    @Override
    public V put(String key, V value) {
        try {
            storage.put(itemPath, value, transactionKey);
        }
        catch (PersistencyException e) {
            log.error("put() - {}/{}", itemPath, value.getClusterPath(), e);
            throw new IllegalArgumentException(e);
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        String fullKey = getFullKey(key);
        try {
            storage.remove(itemPath, fullKey, transactionKey);
        }
        catch (PersistencyException e) {
            log.error("remove() - {}/{}", itemPath, fullKey, e);
            throw new IllegalArgumentException(e);
        }
        return null;
    }

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        for (Entry<? extends String, ? extends V> entry: m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot delete the full content of cluster:"+itemPath+"/"+clusterType);
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        Set<Entry<String, V>> entries = new TreeSet<>();
        for (String key: keySet()) {
            entries.add(new AbstractMap.SimpleImmutableEntry<String, V>(key, get(key)));
        }
        return entries;
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (String key: keySet()) values.add(get(key));
        return values;
    }
}
