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
package org.cristalise.kernel.process;

import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.ItemVerticle;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ProxyManager;
import org.cristalise.kernel.entity.proxy.ProxyServer;
import org.cristalise.kernel.lookup.Lookup;
import org.cristalise.kernel.lookup.LookupManager;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.process.module.ModuleManager;
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.process.resource.DefaultResourceImportHandler;
import org.cristalise.kernel.process.resource.Resource;
import org.cristalise.kernel.process.resource.ResourceImportHandler;
import org.cristalise.kernel.process.resource.ResourceLoader;
import org.cristalise.kernel.scripting.ScriptConsole;
import org.cristalise.kernel.security.SecurityManager;
import org.cristalise.kernel.utils.CastorXMLUtility;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.kernel.utils.ObjectProperties;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * The Gateway is the central object of a CRISTAL process. It initializes,
 * maintains and shuts down every other subsystem in both the client and the
 * server.
 *
 * Child objects:
 * <ul>
 * <li>Lookup - Provides access to the CRISTAL directory. Find or
 * search for Items or Agents.
 * <li>ProxyManager - Gives a local proxy object for Entities found
 * in the directory. Execute activities in Items, query or subscribe to Entity data.
 * </ul>
 */
@Slf4j
public class Gateway
{
    static private ObjectProperties     mC2KProps = new ObjectProperties();
    static private ModuleManager        mModules;
    static private Vertx                mVertx;
    static private Lookup               mLookup;
    static private LookupManager        mLookupManager = null;
    static private ClusterStorageManager   mStorage;
    static private ProxyManager         mProxyManager;
    static private ProxyServer          mProxyServer;
    static private CastorXMLUtility     mMarshaller;
    static private ResourceLoader       mResource;
    static private SecurityManager      mSecurityManager = null;

    //FIXME: Move this cache to Resource class - requires to extend ResourceLoader with getResourceImportHandler()
    static private HashMap<BuiltInResources, ResourceImportHandler> resourceImportHandlerCache = new HashMap<BuiltInResources, ResourceImportHandler>();

    private Gateway() { }

    /**
     * Initialises the Gateway and all of the client objects it holds, with
     * the exception of the Lookup, which is initialised during connect()
     *
     * @param props - java.util.Properties containing all application properties.
     * If null, the java system properties are used
     * @throws InvalidDataException - invalid properties caused a failure in initialisation
     */
    static public void init(Properties props) throws InvalidDataException {
        init(props, null);
    }

    /**
     * Initialises the Gateway and all of the client objects it holds, with
     * the exception of the Lookup, which is initialised during connect()
     *
     * @param props - java.util.Properties containing all application properties.
     * If null, the java system properties are used
     * @param res - ResourceLoader for the kernel to use to resolve all class resource requests
     * such as for bootstrap descriptions and version information
     * @throws InvalidDataException - invalid properties caused a failure in initialisation
     */
    static public void init(Properties props, ResourceLoader res) throws InvalidDataException {

        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
        System.setProperty("hazelcast.logging.type", "slf4j");

        // Init properties & resources
        mC2KProps.clear();

        mResource = res;
        if (mResource == null) mResource = new Resource();

        // report version info
        log.info("Kernel version: "+getKernelVersion());

        // load kernel mapfiles giving the resourse loader and the properties of
        // the application to be able to configure castor
        try {
            mMarshaller = new CastorXMLUtility(mResource, props, mResource.getKernelResourceURL("mapFiles/"));
        }
        catch (MalformedURLException e1) {
            throw new InvalidDataException("Invalid Resource Location");
        }

        Properties allModuleProperties;

        // init module manager
        try {
            mModules = new ModuleManager(AbstractMain.isServer);
            allModuleProperties = mModules.loadModules(mResource.getModuleDefURLs());
        }
        catch (Exception e) {
            log.error("", e);
            throw new InvalidDataException("Could not load module definitions.");
        }

        // merge in module props
        for (Enumeration<?> e = allModuleProperties.propertyNames(); e.hasMoreElements();) {
            String propName = (String)e.nextElement();
            mC2KProps.put(propName, allModuleProperties.get(propName));
        }

        // Overwrite with argument props
        if (props != null) mC2KProps.putAll(props);

        // dump properties
        log.info("Gateway.init() - DONE");
        dumpC2KProps(7);
    }

    /**
     * 
     */
    static private void createVerticles() {
        DeploymentOptions options = new DeploymentOptions().setWorker(true);
        mVertx.deployVerticle(ItemVerticle.class, options);
    }

    /**
     * Makes this process capable of creating and managing server entities. Runs the
     * Creates the LookupManager, ProxyServer, initialises the vertx services
     */
    static public void startServer() throws InvalidDataException, CannotManageException {
        try {
            // check top level directory contexts
            if (mLookup instanceof LookupManager) {
                mLookupManager = (LookupManager) mLookup;
                mLookupManager.initializeDirectory(null);
            }
            else {
                throw new CannotManageException("Lookup implementation is not a LookupManager. Cannot write to directory");
            }

            createVerticles();

            // start entity proxy server
            String serverName = mC2KProps.getProperty("ItemServer.name");
            if (serverName != null) mProxyServer = new ProxyServer(serverName);

            log.info("Server '"+serverName+"' STARTED.");

            if (mLookupManager != null) mLookupManager.postStartServer();
            mStorage.postStartServer();
        }
        catch (Exception ex) {
            log.error("Exception starting server components. Shutting down.", ex);
            AbstractMain.shutdown(1);
        }
    }

    /**
     * Static getter for ModuleManager
     * 
     * @return ModuleManager
     */
    public static ModuleManager getModuleManager() {
        return mModules;
    }
    

    /**
     * 
     * @param options
     * @param clustered
     * @throws CriseVertxException 
     */
    private static void createVertx(VertxOptions options, boolean clustered) throws CriseVertxException {
        if (mVertx == null) {
            if (clustered) {
                CompletableFuture<Void> future = new CompletableFuture<Void>();

                Vertx.clusteredVertx(options, (result) -> {
                    if (result.succeeded()) {
                        mVertx = result.result();
                        log.info("createVertx(clustered) -  Done:{}", mVertx);
                        future.complete(null);
                    }
                    else {
                        log.error("createVertx(clustered)", result.cause());
                        future.completeExceptionally(result.cause());
                  }
                });

                try {
                    future.get(120, TimeUnit.SECONDS);
                }
                catch (ExecutionException e) {
                    throw CriseVertxException.convert(e);
                }
                catch (InterruptedException | TimeoutException e) {
                    log.error("requestAction()", e);
                    throw new CannotManageException(e);
                }
            }
            else {
                mVertx = Vertx.vertx(options);
                log.info("createVertx() -  Done:{}", mVertx);
            }
        }
    }

    /**
     * Initialises the {@link Lookup} and {@link ProxyManager}
     *
     * @param auth the Authenticator instance
     * @throws CriseVertxException 
     */
    private static void setup(Authenticator auth) throws CriseVertxException {
        if (mLookup != null) mLookup.close();

        createVertx(new VertxOptions(), mC2KProps.getBoolean("Gateway.clusteredVertx", true));

        try {
            mLookup = (Lookup)mC2KProps.getInstance("Lookup");
            mLookup.open(auth);
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            log.error("", ex);
            throw new InvalidDataException("Cannot connect server process. Please check config.");
        }

        mStorage = new ClusterStorageManager(auth);
        mProxyManager = new ProxyManager();
    }

    /**
     * Connects to the Lookup server in an administrative context - using the admin username and
     * password available in the implementation of the Authenticator. It shall be
     * used in server processes only.
     *
     * @throws InvalidDataException - bad params
     * @throws PersistencyException - error starting storages
     * @throws ObjectNotFoundException - object not found
     */
    static public Authenticator connect() throws CriseVertxException {
        mSecurityManager = new SecurityManager();
        mSecurityManager.authenticate();

        setup(mSecurityManager.getAuth());

        log.info("connect(system) DONE.");

        mStorage.postConnect();

        return mSecurityManager.getAuth();
    }

    /**
     * Log in with the given username and password, and initialises the {@link Lookup} and 
     * {@link ProxyManager}. It shall be used in client processes only.
     * 
     * @param agentName - username
     * @param agentPassword - password
     * @return an AgentProxy on the requested user
     * 
     * @throws InvalidDataException - bad params
     * @throws PersistencyException - error starting storages
     * @throws ObjectNotFoundException - object not found
     */
    static public AgentProxy connect(String agentName, String agentPassword)throws CriseVertxException {
        return connect(agentName, agentPassword, null);
    }

    /**
     * Log in with the given username and password, and initialises the {@link Lookup} 
     * and {@link ProxyManager}. It shall be uses in client processes only.
     * 
     * @param agentName - username
     * @param agentPassword - password
     * @param resource - resource
     * @return an AgentProxy on the requested user
     * 
     * @throws InvalidDataException - bad params
     * @throws PersistencyException - error starting storages
     * @throws ObjectNotFoundException - object not found
     */
    static public AgentProxy connect(String agentName, String agentPassword, String resource)
            throws CriseVertxException
    {
        mSecurityManager = new SecurityManager();
        mSecurityManager.authenticate(agentName, agentPassword, resource, true, null);

        setup(mSecurityManager.getAuth());

        AgentProxy agent = Gateway.getProxyManager().getAgentProxy(agentName);

        //TODO: swingui specific initialization
        ScriptConsole.setUser(agent);

        // Run module startup scripts. Server does this during bootstrap
        mModules.setUser(agent);
        mModules.runScripts("startup");

        log.info("connect(agent) DONE.");

        mStorage.postConnect();

        return agent;
    }

    /**
     * Get the Authenticator instance
     * 
     * @return the Authenticator
     * @throws InvalidDataException in case of ClassNotFoundException or InstantiationException or IllegalAccessException
     * @deprecated use {{@link #getSecurityManager()}} instead if you need to authenticate
     */
    @Deprecated
    static public Authenticator getAuthenticator() throws InvalidDataException {
        try {
            return (Authenticator)mC2KProps.getInstance("Authenticator");
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            log.error("Authenticator "+mC2KProps.getString("Authenticator")+" could not be instantiated", ex);
            throw new InvalidDataException("Authenticator "+mC2KProps.getString("Authenticator")+" could not be instantiated");
        } 
    }

    /**
     * Shuts down all kernel API objects
     */
    public static void close() {
        // run shutdown module scripts
        if (mModules != null) mModules.runScripts("shutdown");

        // shut down servers if running
        if (mVertx != null) mVertx.close();

        // disconnect from storages
        if (mStorage != null) mStorage.close();
        mStorage = null;

        // disconnect from lookup
        if (mLookup != null) mLookup.close();
        mLookup = null;
        mLookupManager = null;

        // shut down proxy manager & server
        if (mProxyManager != null) mProxyManager.shutdown();
        if (mProxyServer != null)  mProxyServer.shutdownServer();
        mProxyManager = null;
        mProxyServer = null;

        // close log consoles
        Logger.closeConsole();

        // clean up remaining objects
        mModules = null;
        mResource = null;
        mMarshaller = null;
        mC2KProps.clear();

        // abandon any log streams
        Logger.removeAll();
    }

    static public SecurityManager getSecurityManager() {
        return mSecurityManager;
    }

    static public Lookup getLookup() {
        return mLookup;
    }

    static public LookupManager getLookupManager() throws CannotManageException {
        if (mLookupManager == null)
            throw new CannotManageException("No Lookup Manager created. Not a server process.");
        else
            return mLookupManager;
    }

    static public Vertx getVertx() {
        return mVertx;
    }

    static public ClusterStorageManager getStorage() {
        return mStorage;
    }

    static public CastorXMLUtility getMarshaller() {
        return mMarshaller;
    }

    static public ResourceLoader getResource() {
        return mResource;
    }

    static public ProxyManager getProxyManager() {
        return mProxyManager;
    }


    public static ProxyServer getProxyServer() {
        return mProxyServer;
    }

    static public String getCentreId() {
        return getProperties().getString("LocalCentre");
    }

    static public Enumeration<?> propertyNames() {
        return mC2KProps.propertyNames();
    }

    static public void dumpC2KProps(int logLevel) {
        mC2KProps.dumpProps(logLevel);
    }

    static public ObjectProperties getProperties() {
        return mC2KProps;
    }

    static public String getKernelVersion() {
        try {
            return mResource.getTextResource(null, "textFiles/version.txt");
        }
        catch (Exception ex) {
            return "No version info found";
        }
    }

    /**
     * Retrieves the ResourceImportHandler available for the resource type. It creates a new if configured 
     * or falls back to the default one provided in the kernel
     * 
     * @param resType the type o the Resource. ie. one of these values: OD/SC/SM/EA/CA/QL
     * @return the initialised ResourceImportHandler
     */
    @Deprecated
    public static ResourceImportHandler getResourceImportHandler(String resType) throws Exception {
        return getResourceImportHandler(BuiltInResources.getValue(resType));
    }

    /**
     * Retrieves the ResourceImportHandler available for the resource type. It creates a new if configured 
     * or falls back to the default one provided in the kernel
     * 
     * @param resType the type o the Resource
     * @return the initialised ResourceImportHandler
     */
    public static ResourceImportHandler getResourceImportHandler(BuiltInResources resType) throws Exception {
        if (resourceImportHandlerCache.containsKey(resType)) return resourceImportHandlerCache.get(resType);

        ResourceImportHandler handler = null;

        if (Gateway.getProperties().containsKey("ResourceImportHandler."+resType)) {
            try {
                handler = (ResourceImportHandler) Gateway.getProperties().getInstance("ResourceImportHandler."+resType);
            }
            catch (Exception ex) {
                log.error("Exception loading ResourceHandler for "+resType+". Using default.", ex);
            }
        }

        if (handler == null) handler = new DefaultResourceImportHandler(resType);

        resourceImportHandlerCache.put(resType, handler);

        return handler;
    }

    /**
     * Run the different kind of Boostrap processes
     * 
     * @throws Exception anything could happen
     */
    public static void runBoostrap() throws Exception {
        if (Gateway.getProperties().containsKey(AbstractMain.MAIN_ARG_SKIPBOOTSTRAP)) {
            //minimum initialisation only
            Bootstrap.init();

            if (mLookupManager != null) mLookupManager.postBoostrap();
            mStorage.postBoostrap();
        }
        else {
            //creates a new thread to run initialisation and complete checking bootstrap & module items
            Bootstrap.run();
        }
    }
}
