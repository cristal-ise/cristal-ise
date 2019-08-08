Boot Sequence of Server
==========================
This is an account of the sequence of events involved in starting a CRISTAL server process. Starting from StandardServer.standardInitialization(args), which is the common entry point for all server, it brings up a CRISTAL kernel server.

1. `AbstractMain.readC2KArgs(args)` - parses [Command-line Arguments](../Command-line-Arguments). 
    * Converts all **command-line args** into name/value pairs in a Properties object
    * Opens default **log stream**. Default is System.out at log level 0, overridden by -logLevel and -logFile args. Command-line argument -noNewLogStream skips this.
    * **Config file** given by -config loaded into Properties object. BadArgumentsException thrown if arg not given.
    * **Connect file** given by -connect merged over properties. BadArgumentsException thrown if arg not given.
    * **'LocalCentre'** property set to clc file name if not already in properties.
    * **Argument Properties merged** over properties.

1. `Gateway.init(Properties, ResourceLoader)`
    * **[CRISTAL Properties](../CRISTAL-Properties)** created, supplied properties added
    * If custom **[ResourceLoader](../ResourceLoader)** not supplied, default one is created. In the default ResourceLoader, the kernel resource location is set to `org/cristalise/utils/resources/`
    * **Kernel [CastorXMLUtility](../CastorXMLUtility)** is created, and parses all mapfiles listed in kernel resource `mapFiles/index`. This creates a Castor XMLContext of validated map files for marshalling and unmarshalling of kernel objects.
    * **[ModuleManager](../ModuleManager)** is created, with all module definition file URLs given by the ResourceLoader (in the default ResourceLoader, this is found by `ClassLoader.getSystemResources("META-INF/cristal/module.xml")`)
    * A special [XMLSchema](../XMLSchema) object is created directly from the kernel Module schema located in `boot/OD/Module.xsd`. This will be imported later as an Item.
    * Each **Module Definition File is loaded** using the ResourceLoader, and validated against the schema. The module dependencies are verified, and the modules are ordered so they each follow their dependencies. Any properties defined by the `<Config>` element are collected, filtered according to whether the current process is a client or a server.
    * The **module properties** are merged into the Kernel Properties, by order of dependency.
    * The **Kernel Properties** are again merged with the properties supplied in the method **arguments**, so they take precedence over any module properties.
    * Any client [startup scripts](../Script) are run here, if this is a client process.

1. `Gateway.connect()` - makes a root connection to the **[Lookup](../Lookup)** directory
    * An [Authenticator](../Authenticator) is obtained. The implementation of this is taken from the **'Authenticator'** kernel property, which may be a class name or a pre-instantiated object. In the case of the default LDAP lookup module, this is provided in the module properties.
    * An authentication request is made using a 'System' context. For the LDAP Lookup:
        * **LDAP properties** are loaded from the Kernel Properties. Host, port, user and password must be set.
    * The Lookup implementation is obtained from the kernel property **'Lookup'**, which again may be a class name or an instance, and is specified in the [LDAPLookup](../LDAPLookup) module properties.
    * Lookup.open(Authenticator) is called. For LDAP:
        * ItemPath, DomainPath and RolePath **root contexts** are derived from the global, root and local paths defined in the kernel properties.
        * The **[LDAPPropertyManager](../LDAPPropertyManager)** [ClusterStorage](../ClusterStorage) object is initialized
    * `TransactionManager()` -  creates the **[TransactionManager](../TransactionManager)**, which manages locks, and fairly atomic transactions on top of the storages
        * `ClusterStorageManager()` - creates the **[ClusterStorageManager](../ClusterStorageManager)**, which loads and manages the individual Cluster Storage implementations
    * `ProxyManager()` - initialises the **[ProxyManager](../proxy-client)**, though it has nothing to connect to yet

1. `Logger.initConsole("ItemServer")` - creates the **server console** listener for remote administration. See [Logger](../Logger)

1. `Gateway.startServer` - starts the **server components** of the process
    * If the Lookup is also an instance of the [LookupManager](../LookupManager) interface, `LookupManager.initializeDirectory` is called. For LDAP:
        * The LDAP directory is initialized with the global, root and local contexts if they don't already exist.
        * The ItemPath and DomainPath roots are created.
        * The basic empty Role is created. All roles inherit from this, and all users hold it.
        * If a 3.0 Role structure inside the domain tree is detected, it is migrated to the new root. (kernel 3.1+)
    * The [ProxyServer](../ProxyServer) is created, which establishes the **server proxy listener** for clients to receive data change notification, on the port given by the property `ItemServer.Proxy.port`, and on the interface specified by `ItemServer.name`
    * The **ORB** is initialized.
    * The **[CorbaServer](../CorbaServer)** is initialized, which sets up and activates the **POA** 1 `Bootstrap.run()` - kernel description bootstrapping
    * The **core administrative agents** are checked and created if missing
    * The server Item is created, and the proxy client is reinitialized to connect to it.
    * The [Bootstrap](../Bootstrap) thread is started

At this point all server components are up, and it is considered functional. The Bootstrap process will proceed, verifying all boot and module descriptions against the versions bundled in the kernel and module jars, and creating Module Items for each installed module.


# Reset IORs
It is possible to start a StandardServer with options to reset [CORBA IORs](https://en.wikipedia.org/wiki/Interoperable_Object_Reference). There are many situation when you need to reset the IORs.

* The host was was renamed
* Migrating or recovering the server to a new host
* Recreate test or development server from backup

Use the following command (check [Command line Arguments](../Command-line-Arguments) for further details):

`java -classpath <...> org.cristalise.kernel.process.StandardServer -logLevel 8 -config src/main/bin/server.conf -connect src/main/bin/integTest.clc -resetIOR`

This shall launch the Server process with fully initialised ORB and POAs, recreate the IORs for all Items in the system, and update it using the Lookup interface. When it is finished the process should exit gracefully. You must stop the existing StandardServer process before using the reset ior feature. 


# Command line Arguments

These are the parameters supported by the [`AbstractMain.readC2KArgs()`](../blob/master/src/main/java/org/cristalise/kernel/process/AbstractMain.java#L65):

* *-logLevel [0-9]*: Sets the log level of the system console
* *-logFile <path>*: Redirects the system console to a file
* *-config <file>*: File to read process-specific [CRISTAL-iSE Properties](../CRISTAL-System-Properties) from.
* *-connect <file>*: File to read centre-specific [CRISTAL-iSE Properties](../CRISTAL-System-Properties) from that will override the -config properties.
* *-resetIOR*: Launch the server in [Reset IORs](../Reset-IORs) mode
* *-skipBootstrap*: **ONLY FOR DEVELOPMENT!** Launch the server without reading bootstrap files (module.xml)

As of 3.0, all command-line parameters are added to the [CRISTAL-iSE Properties](../CRISTAL-System-Properties), so may be used to override config file settings.

The command to launch an application looks like this:

`java -classpath <...> org.cristalise.kernel.process.StandardServer -logLevel 8 -config src/main/bin/server.conf -connect src/main/bin/integTest.clc`

# Activity Execution

## JooqClusterStorage
[JooqClusterStorage: 111](https://github.com/cristal-ise/jooqdb/blob/1cb92d738c6b711250302ea8ecab9e38e6d2f14c/src/main/java/org/cristalise/storage/jooqdb/JooqClusterStorage.java#L111)
- `createTables` currently disabled, has to be run separately

## Activity Execution
- For each item an `ItemImplementation` instance is created in the memory.
- When a CORBA call is received, Cristal finds the `ItemImplementation` object for the item and calls `delegatedAction` on it

***
[`ItemImplementation.delegatedAction()`](https://github.com/cristal-ise/kernel/blob/196706c46e7b76c1cb24d4f87b523760c071c042/src/main/java/org/cristalise/kernel/entity/ItemImplementation.java#L226) is invoked for all actions and predefined steps <br/>
The original idea behind `delegated` was that if an authorized agent goes on vacation, he delegates his duties to someone else.

   1. get the Workflow of the item
   1. [`Workflow.requestAction()`](https://github.com/cristal-ise/kernel/blob/618fe12abc80e1db5482843d78c590966608665c/src/main/java/org/cristalise/kernel/lifecycle/instance/Workflow.java#L128)
      i find the Activity (vertex)
      1. call `Activity.request()` using itself as a **`locker`** object
      1. [`Activity.request()`](https://github.com/cristal-ise/kernel/blob/d3cc8fd92607e097dd934e4ed9b674cc36cecdd2/src/main/java/org/cristalise/kernel/lifecycle/instance/Activity.java#L172)
         1. find requested Transition
         1. check authorization
         1. check that outcome was given if needed
         1. get new state
         1. [`Activity.runActivityLogic`](https://github.com/cristal-ise/kernel/blob/d3cc8fd92607e097dd934e4ed9b674cc36cecdd2/src/main/java/org/cristalise/kernel/lifecycle/instance/Activity.java#L334)
            * run extra logic in predefined steps (overridden method in predefined steps)
         1. set new state and reservation
         1. unmarshal Outcome
         1. `History.addEvent()` -> [`RemoteMap.put()`](https://github.com/cristal-ise/kernel/blob/develop/src/main/java/org/cristalise/kernel/persistency/RemoteMap.java#L285) called for the `event`
            * [`TransactionManager.put()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L175)
               * caches the `put()` call (as a "pending transaction"), but only in an internal cache, does not invoke the `ClusterStorage`
               * calls are grouped by the **`locker`** object (basically used as a transaction ID)
         1. `Gateway.getStorage().put()` called for the `outcome`
            * [`TransactionManager.put()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L175)
         1. `Gateway.getStorage().put()` called for the `attachment`
         1. `Gateway.getStorage().put()` called for the `viewpoint`
         1. `Gateway.getStorage().put()` called for the `last` `viewpoint`
         1. `Activity.updateItemProperties()`
         1. `runNext`
         1. `pushJobsToAgents`
   1. store the new workflow if state changed
   1. handle `Erase`
   1. [`TransactionManager.commit()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L282) - called for the Workflow **`locker`** object: commits all the previous `put()` changes
      1. for each "pending transaction"
      1. [`ClusterStorageManager.put()`](https://github.com/cristal-ise/kernel/blob/c49dd8aa8b7b278798a1f7e80c580f6b739ed7f8/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#L328)
         1. call `put()` on all ClusterStorages
         1. [`JooqClusterStorage.put()`](https://github.com/cristal-ise/jooqdb/blob/1cb92d738c6b711250302ea8ecab9e38e6d2f14c/src/main/java/org/cristalise/storage/jooqdb/JooqClusterStorage.java#L268)
            * calls `JooqHandler.put()` on the `JooqHandler` corresponding to the `ClusterType`
            * calls `DomainHandler.put()` on all registered domain handlers to update domain specific tables - this sees ALL the changes done with the same **`locker`** object (i.e. in the same transaction) because of [the get() implementation](#the-get-implementation)

## The get() implementation
For example, if a `DomainHandler` executes a script (for example aggregate script) which reads a viewpoint (details schema):
[`Script.evaluate()`](https://github.com/cristal-ise/kernel/blob/56e221a176dd9c9330bb286b41aba10494353662/src/main/java/org/cristalise/kernel/scripting/Script.java#L510)

   * get the ItemProxy
   * set the **`locker`** as a **`transactionKey`** on the `ItemProxy`
   * `execute()`
   * then in the script, for example: <br/>
[`ItemProxy.getViewpoint()`](https://github.com/cristal-ise/kernel/blob/7845981def21deef7a2a0a0180d13b7bbffb91fe/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L572)
      * `locker == null ?` **`transactionKey`** `: locker`
      * [`ItemProxy.getObject()`](https://github.com/cristal-ise/kernel/blob/7845981def21deef7a2a0a0180d13b7bbffb91fe/src/main/java/org/cristalise/kernel/entity/proxy/ItemProxy.java#L1062)
         * `Gateway.getStorage().get()` = <br/>
[`TransactionManager.get()`](https://github.com/cristal-ise/kernel/blob/d82053de2237a9bef35267034dc17ad0fa8737ae/src/main/java/org/cristalise/kernel/persistency/TransactionManager.java#L144)
            * HISTORY and JOB `ClusterType`s are handled in a special way
            * _if_ this **`locker`** has been modifying this `itemPath`, **read the object from the cache**
            * _else_ read the object from the `ClusterStorage` using [`ClusterStorageManager.get()`](https://github.com/cristal-ise/kernel/blob/c49dd8aa8b7b278798a1f7e80c580f6b739ed7f8/src/main/java/org/cristalise/kernel/persistency/ClusterStorageManager.java#L258)