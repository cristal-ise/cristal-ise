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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;


/**
 * Maps a storage cluster onto a java.util.Map
 *
 * @param <V> the C2KLocalObject stored by this Map
 */
@Slf4j
public class RemoteMap<V extends C2KLocalObject> extends TreeMap<String, V> implements C2KLocalObject  {

    private static final long serialVersionUID = -2356840109407419763L;

    private int mID=-1;
    private String mName;
    protected ItemPath mItemPath;
    private String mPath = "";
    Object keyLock = null;
    ClusterStorageManager storage;
    Comparator<String> comp;

    /**
     * For remote client processes to receive updates, disables write operations (remove, put).
     * @check activate()
     */
    ItemProxy source;

    ProxyObserver<V> listener;

    /**
     * if this remote map will participate in a transaction
     */
    TransactionKey mTransactionKey;

    public RemoteMap(ItemPath itemPath, String path, TransactionKey transactionKey) {
        super(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Integer i1 = null, i2 = null;
                try {
                    i1 = Integer.valueOf(o1);
                    i2 = Integer.valueOf(o2);
                    return i1.compareTo(i2);
                }
                catch (NumberFormatException ex) { }

                return o1.compareTo(o2);
            }
        });

        mItemPath = itemPath;
        mTransactionKey = transactionKey;

        // split the path into path/name
        int lastSlash = path.lastIndexOf("/");
        mName = path.substring(lastSlash + 1);
        if (lastSlash > 0) mPath = path.substring(0, lastSlash);

        // see if the name is also a suitable id
        try {
            mID = Integer.parseInt(mName);
        }
        catch (NumberFormatException e) {}

        storage = Gateway.getStorage();
    }

    public void activate() {
        if (listener != null) {
            log.debug("activate() - ALREADY active name:{}", mItemPath);
            return;
        }

        listener = new ProxyObserver<V>() {
            @Override
            public void add(V obj) {
                synchronized (this) {
                    log.debug("ProxyObserver.add() - id:"+obj.getName());
                    putLocal(obj.getName(), obj);
                }
            }

            @Override
            public void remove(String id) {
                synchronized (this) {
                    log.debug("ProxyObserver.remove() - id:"+id);
                    removeLocal(id);
                }
            }

            @Override
            public void control(String control, String msg) { }
        };

        try {
            source = Gateway.getProxyManager().getProxy(mItemPath, mTransactionKey);
            source.subscribe(new MemberSubscription<V>(listener, mPath+mName, false));

            log.debug("activate() - name:"+mName+" "+mItemPath);
        }
        catch (Exception ex) {
            log.error("Error subscribing to remote map. Changes will NOT be received", ex);
        }
    }

    public void deactivate() {
        if (source != null) source.unsubscribe(listener);
    }

    @Override
    public void finalize() {
        deactivate();
        Gateway.getStorage().clearCache(mItemPath, mPath+mName);
    }

    protected void loadKeys() {
        if (keyLock != null) return;
        clear();
        keyLock = new Object();

        synchronized(this) {
            String[] keys;
            try {
                keys = storage.getClusterContents(mItemPath, mPath+mName);
                for (String key : keys) super.put(key, null);
            }
            catch (PersistencyException e) {
               log.error("Error loading keys", e);
            }
        }
    }

    public synchronized int getLastId() {
        try {
            return storage.getLastIntegerId(mItemPath, mPath+mName, mTransactionKey);
        }
        catch (PersistencyException ex) {
            log.error("Failed to get last integer id for path:{}", "/"+ mItemPath + "/"+ mPath + mName, ex);
        }
        return -1;
    }

    public void setID(int id) { mID = id; }

    public int getID() { return mID; }

    @Override
    public void setName(String name) { mName = name; }

    @Override
    public String getName() { return mName; }

    /**
     * Returns null so it cannot be stored
     */
    @Override
    public ClusterType getClusterType() {
        return null;
    }

    @Override
    public String getClusterPath() {
        return null;
    }

    @Override
    public synchronized void clear() {
        synchronized (this) {
            super.clear();
        }
        keyLock = null;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        try {
            int i = (int) key;
            return i >= 0 && i <= getLastId();
        }
        catch (ClassCastException | NullPointerException e) {
            return false;
        }
    }

    /**
     * This must retrieve all the values until a match is made.
     * Very expensive, but if you must, you must.
     * @see java.util.Map#containsValue(Object)
     */
    @Override
    public synchronized boolean containsValue(Object value) {
        loadKeys();
        synchronized(this) {
            for (String key: keySet()) {
                if (get(key).equals(value)) return true;
            }
        }
        return false;
    }


    @Override
    @SuppressWarnings("unchecked")
    public synchronized V get(Object objKey) {
        String key;

        if (objKey instanceof Integer)     key = ((Integer)objKey).toString();
        else if (objKey instanceof String) key = (String)objKey;
        else                               return null;

        synchronized(this) {
            try {
                V value = (V)storage.get(mItemPath, mPath+mName+"/"+key, mTransactionKey);
                super.put(key, value);
                return value;
            }
            catch (PersistencyException | ObjectNotFoundException e) {
                log.error("get()", e);
                throw new IllegalArgumentException(e);
            }
        }
    }

    @Override
    public synchronized boolean isEmpty() {
        loadKeys();
        return super.isEmpty();
    }

    /**
     * @see java.util.Map#keySet()
     */
    @Override
    public synchronized Set<String> keySet() {
        loadKeys();
        return super.keySet();
    }

    /**
     * Inserts the given object into the storage
     * the key is ignored - it can be fetched from the value.
     * @see java.util.Map#put(Object, Object)
     */
    @Override
    public synchronized V put(String key, V value) {
        if (source != null) throw new UnsupportedOperationException("Cannot use an activated RemoteMap to write to storage.");

        try {
            synchronized(this) {
                storage.put(mItemPath, value, mTransactionKey);
                return putLocal(key, value);
            }
        }
        catch (PersistencyException e) {
            log.error("put()",e);
            throw new IllegalArgumentException(e);
        }
    }

    protected synchronized V putLocal(String key, V value) {
        return super.put(key, value);
    }

    /**
     * @see java.util.Map#remove(Object)
     */
    @Override
    public synchronized V remove(Object key) {
        if (source != null) throw new UnsupportedOperationException("Cannot use an activated RemoteMap to write to storage.");

        try {
            synchronized(keyLock) {
                storage.remove(mItemPath, mPath+mName+"/"+key, mTransactionKey);
                return removeLocal(key);
            }
        }
        catch (PersistencyException e) {
            log.error("remove()", e);
            throw new IllegalArgumentException(e);
        }
    }

    protected synchronized V removeLocal(Object key) {
        return super.remove(key);
    }

    /**
     * @see java.util.Map#size()
     */
    @Override
    public synchronized int size() {
        loadKeys();
        return super.size();
    }

    /**
     * @see java.util.Map#values()
     */
    @Override
    public synchronized Collection<V> values() {
        return new RemoteSet<V>(this);
    }

    /**
     * Basic implementation of Set and Collection to bridge to the Iterator
     * Disallows all writes.
     */
    private class RemoteSet<E extends C2KLocalObject> extends AbstractSet<E> {
        RemoteMap<E> mParent;

        public RemoteSet(RemoteMap<E> parent) {
            mParent = parent;
        }

        // no modifications allowed
        @Override
        public boolean add(E o) {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }
        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<E> iterator() {
            return new RemoteIterator<E>(mParent);
        }

        @Override
        public int size() {
            return mParent.size();
        }
    }

    /**
     * Iterator view on RemoteMap data. Doesn't preload anything.
     * REVISIT: Will go strange if the RemoteMap is modified. Detect this and throw ConcurrentMod ex
     */
    private class RemoteIterator<C extends C2KLocalObject> implements Iterator<C> {
        RemoteMap<C> mParent;
        Iterator<String> iter;
        String currentKey;

        public RemoteIterator(RemoteMap<C> parent) {
            mParent = parent;
            iter = mParent.keySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public C next() {
            currentKey = iter.next();
            return mParent.get(currentKey);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
