# CRISTAL Module Maven Archetype

This is the source of the template module project to be used to create a new module using Maven.

Currently, this is built and deployed to the CCCS Nexus Repository, so to use it you must use the following archetype catalog URL:
http://dev.cccs.uwe.ac.uk:8081/nexus/service/local/repositories/releases/content/archetype-catalog.xml

When creating the module, the following parameters are required:

  * Group Id, Artifact Id - Maven project details
  * Package - the location where the module resources will be stored. This should be unique to avoid clashes with other modules on the classpath
  * namespace - This is a short name for the module, which will be used as a sub-context in the domain tree for descriptions
