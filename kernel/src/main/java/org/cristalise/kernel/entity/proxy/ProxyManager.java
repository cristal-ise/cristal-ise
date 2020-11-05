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
package org.cristalise.kernel.entity.proxy;

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;
import static org.cristalise.kernel.property.BuiltInItemProperties.TYPE;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.SoftCache;

import lombok.extern.slf4j.Slf4j;

/**
 * Manager of pool of Proxies and their subscribers
 */
@Slf4j
public class ProxyManager {
    
    public static final String CONFIG_STRICT_POLICY = "ProxyManager.strictPolicy";

    SoftCache<ItemPath, ItemProxy>             proxyPool       = new SoftCache<ItemPath, ItemProxy>(50);
    HashMap<DomainPathSubscriber, DomainPath>  treeSubscribers = new HashMap<DomainPathSubscriber, DomainPath>();
    HashMap<String, ProxyServerConnection>     connections     = new HashMap<String, ProxyServerConnection>();

    ProxyMessageListener messageListener = null;

    /**
     * Create a proxy manager to listen for proxy events and reap unused proxies
     */
    public ProxyManager() throws InvalidDataException {
        Iterator<Path> servers = Gateway.getLookup().search(new DomainPath("/servers"), new Property(TYPE, "Server", false));

        while(servers.hasNext()) {
            Path thisServerResult = servers.next();
            try {
                ItemPath thisServerPath = thisServerResult.getItemPath();

                String remoteServer = ((Property)Gateway.getStorage().get(thisServerPath, ClusterType.PROPERTY+"/"+NAME, null)).getValue();
                String portStr      = ((Property)Gateway.getStorage().get(thisServerPath, ClusterType.PROPERTY+"/ProxyPort", null)).getValue();

                connectToProxyServer(remoteServer, Integer.parseInt(portStr));
            }
            catch (Exception ex) {
                log.error("Exception retrieving proxy server connection data for "+thisServerResult, ex);
            }
        }

        try {
            if (Gateway.getProperties().containsKey("ProxyMessageListener")) {
                messageListener = (ProxyMessageListener) Gateway.getProperties().getInstance("ProxyMessageListener");
            }
        }
        catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error("", e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    public void connectToProxyServer(String name, int port) {
        ProxyServerConnection oldConn = connections.get(name);

        if (oldConn != null) oldConn.shutdown();

        connections.put(name, new ProxyServerConnection(name, port, this));
    }

    protected void resubscribe(ProxyServerConnection conn) {
        synchronized (proxyPool) {
            for (ItemPath key : proxyPool.keySet()) {
                ProxyMessage sub = new ProxyMessage(key, ProxyMessage.ADDPATH, false);
                log.debug("resubscribe() - item:{}", key);
                conn.sendMessage(sub);
            }
        }
    }

    /**
     * @param sub
     */
    private void sendMessage(ProxyMessage sub) {
        for (ProxyServerConnection element : connections.values()) {
            element.sendMessage(sub);
        }
    }

    public void shutdown() {
        log.info("shutdown() - flagging shutdown of server connections");
        for (ProxyServerConnection element : connections.values()) {
            element.shutdown();
        }
    }

    protected void processMessage(ProxyMessage thisMessage) throws InvalidDataException {
        log.trace("processMessage() - Received proxy message:{}", thisMessage);

        if (thisMessage.getPath().equals(ProxyMessage.PINGPATH)) // ping response
            return;

        if (thisMessage.getItemPath() == null) {
            // must be domain path info
            informTreeSubscribers(thisMessage.isState(), thisMessage.getPath());
        }
        else {
            // proper proxy message
            ItemProxy relevant = proxyPool.get(thisMessage.getItemPath());
            if (relevant == null) {
                log.warn("processMessage() - Received message for which there is no proxy - message:{}", thisMessage);
            }
            else {
                try {
                    log.trace("processMessage() - notify relevant '{}' of proxy message:{}", relevant, thisMessage);
                    relevant.notify(thisMessage);
                }
                catch (Throwable ex) {
                    log.error("Error caught notifying relevant '{}' of proxy message:{}", relevant, thisMessage, ex);
                }
            }
        }

        if (messageListener != null) messageListener.notifyMessage(thisMessage);
    }

    private void informTreeSubscribers(boolean state, String path) {
        DomainPath last = new DomainPath(path);
        DomainPath parent;
        boolean first = true;

        synchronized (treeSubscribers) {
            while ((parent = last.getParent()) != null) {
                ArrayList<DomainPathSubscriber> currentKeys = new ArrayList<DomainPathSubscriber>();
                currentKeys.addAll(treeSubscribers.keySet());

                for (DomainPathSubscriber sub : currentKeys) {
                    DomainPath interest = treeSubscribers.get(sub);

                    if (interest != null && interest.equals(parent)) {
                        if (state == ProxyMessage.ADDED) sub.pathAdded(last);
                        else if (first)                  sub.pathRemoved(last);
                    }
                }
                last = parent;
                first = false;
            }
        }
    }

    public void subscribeTree(DomainPathSubscriber sub, DomainPath interest) {
        synchronized(treeSubscribers) {
            treeSubscribers.put(sub, interest);
        }
    }

    public void unsubscribeTree(DomainPathSubscriber sub) {
        synchronized(treeSubscribers) {
            treeSubscribers.remove(sub);
        }
    }

    private ItemProxy createProxy( org.omg.CORBA.Object ior, ItemPath itemPath) throws ObjectNotFoundException {
        ItemProxy newProxy = null;

       log.debug("createProxy() - Item:{}", itemPath);

        if( itemPath instanceof AgentPath ) {
            newProxy = new AgentProxy(ior, (AgentPath)itemPath);
        }
        else {
            newProxy = new ItemProxy(ior, itemPath);
        }

        // subscribe to changes from server
        ProxyMessage sub = new ProxyMessage(itemPath, ProxyMessage.ADDPATH, false);
        sendMessage(sub);
        reportCurrentProxies(9);
        return ( newProxy );
    }

    protected void removeProxy( ItemPath itemPath ) {
        ProxyMessage sub = new ProxyMessage(itemPath, ProxyMessage.DELPATH, true);
        log.debug("removeProxy() - Unsubscribing to proxy informer for {}", itemPath);
        sendMessage(sub);
    }


    /**
     * Called by the other GetProxy methods to either load the find the proxy in the cache
     * or create it from the ItemPath.
     * 
     * @param ior
     * @param itemPath
     * @return the ItemProx
     * @throws ObjectNotFoundException
     */
    private ItemProxy getProxy( org.omg.CORBA.Object ior, ItemPath itemPath) throws ObjectNotFoundException {
        synchronized(proxyPool) {
            ItemProxy newProxy;
            // return it if it exists
            newProxy = proxyPool.get(itemPath);

            // create a new one
            if (newProxy == null) {
                newProxy = createProxy(ior, itemPath);
                proxyPool.put(itemPath, newProxy);
            }
            else {
                // Avoid sharing false transactionKey between calls to the same item (i.e. side scripting requires transactionKey)
                // FIXME for server side use of proxies disable caching and event notifications
                newProxy.setTransactionKey(null);
            }

            return newProxy;
        }
    }

    public ItemProxy getProxy( Path path ) throws ObjectNotFoundException {
        ItemPath itemPath = null;

        log.trace("getProxy(" + path.toString() + ")");

        if (path instanceof ItemPath) {
            try {
                //issue #165: get ItemPath from Lookup to ensure it is a correct class
                itemPath = Gateway.getLookup().getItemPath(((ItemPath)path).getUUID().toString());
            }
            catch (InvalidItemPathException e) {
                throw new ObjectNotFoundException(e.getMessage());
            }
        }
        else if (path instanceof DomainPath) {
            //issue #165: reset target to enforce to read target from Lookup
            ((DomainPath) path).setTargetUUID(null);
            itemPath = path.getItemPath();
        }

        if (itemPath == null) throw new ObjectNotFoundException("Cannot use RolePath");

        return getProxy( itemPath.getIOR(), itemPath );
    }

    public AgentProxy getAgentProxy( String agentName ) throws ObjectNotFoundException {
        AgentPath path = Gateway.getLookup().getAgentPath(agentName);
        return (AgentProxy) getProxy(path);
    }

    public AgentProxy getAgentProxy( AgentPath path ) throws ObjectNotFoundException {
        return (AgentProxy) getProxy(path);
    }

    /**
     * A utility to Dump the current proxies loaded
     * 
     * @param logLevel the selectd log level
     */
    public void reportCurrentProxies(int logLevel) {
        log.trace("Current proxies: ");
        try {
            synchronized(proxyPool) {
                Iterator<ItemPath> i = proxyPool.keySet().iterator();

                for( int count=0; i.hasNext(); count++ ) {
                    ItemPath nextProxy = i.next();
                    ItemProxy thisProxy = proxyPool.get(nextProxy);
                    if (thisProxy != null) {
                        log.trace(""+count + ": "+proxyPool.get(nextProxy).getClass().getName()+": "+nextProxy);
                    }
                }
            }
        }
        catch (ConcurrentModificationException ex) {
            log.trace("Proxy cache modified. Aborting.");
        }
    }

    /**
     * Clears all entries from the cache
     */
    public void clearCache() {
        synchronized(proxyPool) {
            proxyPool.clear();
        }
        log.debug("clearCache() - DONE");
    }

    /**
     * Clears the given Item from the cache
     * @param item the UUID
     */
    public void clearCache(ItemPath item) {
        synchronized(proxyPool) {
            if (proxyPool.remove(item) != null) {
                log.debug("clearCache({}) - Item was removed from cache", item);
            }
            else {
                log.trace("clearCache({}) - Item was NOT in cache", item);
            }
        }
    }
}
