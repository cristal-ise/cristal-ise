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

### Server/client-specific

These properties configure server or client processes. Some are included in module definitions.

| Property | Process type | Description | Example value |
|----------|--------------|-------------|---------------|
| JOOQ.domainHandlers | Server | Comma separated list of fully qualified class names implementing the `JooqDomainHandler` interface | String |
| JOOQ.disableDomainCreateTables | Server | Disables the invocation of `createTables(DSLContext)` on `JooqDomainHandler` instances from `JooqClusterStorage.initialiseHandlers()` (default: false) | Boolean |
| JOOQ.Event.enableHasAttachment | Server | Enables the proper handling of attachement of Event table. Using this feature coukd require database migration (default: true) | Boolean |
| JOOQ.OutcomeAttachment.enableFileName | Server | Enables the proper handling of attachement of Outome table. Using this feature coukd require database migration (default: true) | Boolean |
| JOOQ.TemporaryPwdFieldImplemented | Server | Enables the proper handling of temproary password in Item table. Using this feature coukd require database migration (default: true) | Boolean |
|JooqLookupManager.getChildrenPattern.specialCharsToReplace | Server | Regular expression to escape special charaters in Item names during Lookup.getChildren() (default: [^a-zA-Z0-9]) | String |
| JOOQ.NameType.length | Server | Configure the length of the varchar column for Names (default: 64) | Integer |
| JOOQ.PasswordType.length | Server | Configure the length of the varchar column for Passwords (default: 800) | Integer |
| JOOQ.StringType.length | Server | Configure the length of the varchar column for Strings (default: 800) | Integer |
| JOOQ.TextType.length | Server | Configure the length of the varchar column for clobs (default: 800) | Integer |
| JOOQ.readOnlyDataSource | Client | Setup the client connection to use readonly connections (default: false) | boolean |
| Webui.autoComplete.default | Server | Value of default autoComplete flag used during DynamicForms generation. Use values 'on' or 'off' (default=off)| String |

## Module-specific

These properties enable integration of module extensions into the kernel.

| Property | Process type | Description | Example value |
|----------|--------------|-------------|---------------|


