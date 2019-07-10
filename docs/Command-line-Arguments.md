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
