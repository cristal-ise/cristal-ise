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
    * The Lookup implementation is obtained from the kernel property **'Lookup'**, which again may be a class name or an instance, and is specified in the [DAPLookup](../DAPLookup) module properties.
    * Lookup.open(Authenticator) is called. For LDAP:
        * ItemPath, DomainPath and RolePath **root contexts** are derived from the global, root and local paths defined in the kernel properties.
        * The **[LDAPPropertyManager](../LDAPPropertyManager)** [ClusterStorage](../ClusterStorage) object is initialized
    * `TransactionManager()` -  creates the **[TransactionManager](../TransactionManager)**, which manages locks, and fairly atomic transactions on top of the storages
        * `ClusterStorageManager()` - creates the **[ClusterStorageManager](../ClusterStorageManager)**, which loads and manages the individual Cluster Storage implementations
    * `ProxyManager()` - initialises the **[ProxyManager](../proxy client)**, though it has nothing to connect to yet
1. `Logger.initConsole("ItemServer")` - creates the **server console** listener for remote administration. See [Logger](../Logger)
1. `Gateway.startServer` - starts the **server components** of the process
    * If the Lookup is also an instance of the [LookupManager](../LookupManager) interface, `LookupManager.initializeDirectory` is called. For LDAP:
        * The LDAP directory is initialized with the global, root and local contexts if they don't already exist.
        * The ItemPath and DomainPath roots are created.
        * The basic empty Role is created. All roles inherit from this, and all users hold it.
        * If a 3.0 Role structure inside the domain tree is detected, it is migrated to the new root. (kernel 3.1+)
    * The [ProxyServer](../ProxyServer) is created, which establishes the **server proxy listener** for clients to receive data change notification, on the port given by the property `ItemServer.Proxy.port`, and on the interface specified by `ItemServer.name`
    * The **ORB** is initialized.
    * The **[CorbaServer](../CorbaServer)** is initialized, which sets up and activates the **POA**
1 `Bootstrap.run()` - kernel description bootstrapping
    * The **core administrative agents** are checked and created if missing
    * The server Item is created, and the proxy client is reinitialized to connect to it.
    * The [Bootstrap](../Bootstrap) thread is started

At this point all server components are up, and it is considered functional. The Bootstrap process will proceed, verifying all boot and module descriptions against the versions bundled in the kernel and module jars, and creating Module Items for each installed module.