## Background

Traditionally, applications and systems are developed by experts by writing _code_. This code is then _compiled_ and then _deployed_ onto a production system where it can be used by _users_ to perform the tasks for which it was designed. If its functionality needs to be changed, then the developers would analyse the required changes, implement them in the code, the compile and deploy a new version, which replaces the old one. More recent methodologies shorten this lifecycle, deploying less complete versions for early user feedback, but still separate the development and production environments completely, and only generally have one version in production at a time. This latter point is dependent on the former, as with only one version possible in production, upgrading is a violent change: whether good or bad, the old version does not remain, and any data or configuration in the system that is no longer compatible with the newer version must be migrated during the upgrade.

## Configuration

Some software makes use of data to allow the administrator to choose between different behaviours it can exhibit at runtime. This configuration data is often stored as text files, which are free-form and depend on the administrator to fill in properly. More recently, more complex configuration storage forms such as GConf and the Windows Registry have become popular that attempt to constrain the possible values that can be supplied, making configuration easier and reducing the risk of the system becoming non-functional due to misconfiguration, and ease migration between versions that may demand different configuration structures.

Two spectrums of configuration dependency exists in the world of software, relating to configuration of data and processes. 

1. Process configuration describes the behaviour and logic of the system spanning from barely configurable programs that do the same thing every time they are run, to generic systems that merely supply libraries of functionality and rely on complex configuration to piece them together at runtime. Interpreted programming languages arguably lie at this extreme. Recent Workflow Management Systems are also along these lines, though they often provide only process orchestration (deciding what is to be done when) and depend on compiled code to perform the actual work. 
1. Data configuration is a more recent and more advanced innovation, and uses data description languages to define the structure of the data than an applications will use. Simple systems have their data structures hard-coded into their source code, and changing them requires re-compilation, while more dynamic data systems use DDLs to constrain and discover their data structures. Relational-Database Management Systems such as Oracle or MySQL lie at this extreme, though the applications that use them tend to be hard-coded to a particular data schema at compile time, even though the database itself may be more specific.

Some systems designed for data storage, such as the OpenLDAP directory server, can store their configuration in their own data store, and allow it to be changed through its existing data manipulation interfaces. This could be classed as a description-driven system.

## Descriptions

Descriptions, in the context of description-driven systems, are data which define the **processes** and **data structures** of an application, which are **stored**, **modified** and **managed** from within that application. As a result, entire domain applications may be stored as data, blurring the line between model and implementation.

In CRISTAL, descriptions are stored as XML in Documents called [Outcome](../Outcome)s. This XML may be serialized Java objects, but examples also include [XMLSchemas](../XMLSchemas) and Scripts.