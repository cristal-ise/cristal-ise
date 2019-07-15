Listed below are the currently supported CRISTAL system properties to configure the different client or server processes. 

If AbstractMain.readC2KArgs() is used, for example when running a process that uses either org.cristalise.kernel.process.StandardServer, Properties are collected from various places in a specific order:

1. Module &lt;Config&gt; elements
1. Server/client specific config file given with the -config parameter
1. Centre-specific config file (.clc) given by the -connect  parameter
1. Command-line parameters

Later properties overwrite ones defined earlier, which is why the command-line parameters appear twice.

### Centre-specific

These properties configure a server or client with particular parameters for a centre e.g. connection settings. They are usually collected in a 'clc' file which defined the centre.


| Property | Description | Example value |
|----------|-------------|---------------|
| Authenticator | Object or classname of the authentication manager that will be used by Gateway.connect to authenticate the user and pass their credentials to other Gateway components. Use `Shiro` to enable Apache Shiro integration | LDAPAuthManager or instance |
| Lookup | Object or classname of the naming and directory manager to use | LDAPLookup or instance |
| ItemServer.name | Hostname of machine on which the Cristal Java VM runs | cristal2.cern.ch |
| ItemServer.Proxy.port | The port on which the change notification server is listening | 1553 |
| ItemServer.Console.port | The port on which the debugging server is listening. | 88 |
| ItemServer.HTTP.port | The port of the experimental HTTP item server | 8000 | 
| ItemServer.iiop | Port for the CORBA ORB | 1500 | 

### Server/client-specific

These properties configure server or client processes. Some are included in module definitions.

| Property | Server / Process type | Description | Example value |
|----------|-----------------------|-------------|---------------|
| ClusterStorage | All | Ordered list of implementation of the ClusterStorage interface that Cristal will use for storage of item local objects. Precedence is left to right. If package name is not supplied, org.cristalise.storage is implied | LDAPClusterStorage, XMLClusterStorage |
| LocalCentre (not used in 3.0) | All | The centre to connect to/start as. Avoids having to specify the clc file in the arguments. | MyCentre | 
| XMLStorage.root | Server | If using XMLClusterStorage, this defined the root directory of XML file storage | /var/lib/cristal/db | 
| AdminPassword | Server | Overrides the default password for the built-in ‘admin’ and ‘system’ users | admin12345 | 
| ItemServer.Console.allow<br>ItemServer.Console.deny | Server | Host allowed to connect to the debug console, or those not. Only one value should be present. Allow overrides deny. | localhost,cristal-dev |
| Storage.useWeakCache | All | Whether to use Java WeakReferences for the Item object cache instead of the default SoftReferences. The weak cache is useful in servers which access many different Items, such as in large data imports, as it allows the cache to be emptied by the garbage collector regularly, before a low memory situation occurs. | Boolean |
| Storage.disableCache| All | Disable caching (default: false) | Boolean |
| Export.replaceActivitySlotDefUUIDWithName | Client | Replace UUID with ActivityDefinition names when exporting CompositeActivityDef (default: false) | Boolean |
| OutcomeInit.jobUseViewpoint| Client | Job.getOutcome() will use last viewpoint if exist instead OutcomeInitiator (default: false) | Boolean |
| ProxyMessageListener | Client | Name of the class that implements the ProxyMessageListener interface, which will be triggered by ProxyManager.processMessage() for ALL items (since 3.5) | String |
| DSL.GenerateModuleXml| DSL (Client) | Disables module.xml generation (default: true) | Boolean |
| JOOQ.domainHandlers | ? | Comma separated list of fully qualified class names implementing the `JooqDomainHandler` interface | String |
| JOOQ.disableDomainCreateTables | ? | Disables the invocation of `createTables(DSLContext)` on `JooqDomainHandler` instances from `JooqClusterStorage.initialiseHandlers()` (default: false) | Boolean | 
| PredefinedStep.AgentRole.enableAdmin | Server | Set it true to switch on original Admin role settings in certain predefined steps (default: false) | Boolean | 
| PredefinedStep.Erase.roles | Server | Comma seperated list of Roles that are also enabled for the Erase predefined step. It is only needed when `PredefinedStep.AgentRole.enableAdmin` is set to true | String |
| Shiro.iniFile | Server | The location of the shiro ini file (default: `classpath:shiro.ini`) | String |
| Webui.autoComplete.default | Server | Value of default autoComplete flag used during DynamicForms generation. Use values 'on' or 'off' (default=off)| String |

## Module-specific

These properties enable integration of module extensions into the kernel.

| Property | Server / Process type | Description | Example value |
|----------|-----------------------|-------------|---------------|
| OutcomeInit.&lt;name&gt; | Agent | Specifies an OutcomeInitiator implementation to use to create new empty outcomes. Will be invoked from Job.getOutcome() for Activities with an 'OutcomeInit' property set to the given name | Class name |
| Module.debug | Server | Attempts to assign cristal-dev workflows to module resources as they are imported, so they can be edited. | Boolean |
| Module.reset<br>Module.&lt;namespace&gt;.reset | Server | Instructs the module manager not to preserve any modified module resources for either this module or all modules. If false, then resources will not be updated if the current version was modified by someone other than the bootstrapper | Boolean |
| ResourceImportHandler.&lt;type code&gt; | Server | Specifies a custom ResourceImportHandler implementation, allowing modules to define their own resource types, or override the import of the core ones. The type code can be any string, but by convention a short upper-case string is used. The core types are EA (Elementary Activity), CA (Composite Activity), OD (Outcome Description - Schema), SC (Script) and SM (State Machine)| Class name |
| cli.auth | Shell | ProxyLogin implementation to handle console user login | Class name |
| OverrideScriptLang.&lt;lang&gt; | Agent | Override the javax.script engine for the given scripting language. Used to override Javascript in Java8 with Rhino | Alternative language name |


