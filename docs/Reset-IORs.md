It is possible to start a StandardServer with options to reset [CORBA IORs](https://en.wikipedia.org/wiki/Interoperable_Object_Reference). There are many situation when you need to reset the IORs.

* The host was was renamed
* Migrating or recovering the server to a new host
* Recreate test or development server from backup

Use the following command (check [Command line Arguments](../Command-line-Arguments) for further details):

`java -classpath <...> org.cristalise.kernel.process.StandardServer -logLevel 8 -config src/main/bin/server.conf -connect src/main/bin/integTest.clc -resetIOR`

This shall launch the Server process with fully initialised ORB and POAs, recreate the IORs for all Items in the system, and update it using the Lookup interface. When it is finished the process should exit gracefully. You must stop the existing StandardServer process before using the reset ior feature. 
