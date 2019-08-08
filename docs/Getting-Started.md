Dependencies
------------

This section describes the basic software needed to run the CRISTAL-ISE server. This guide focuses on using a directory server which can be any LDAP implementation. For this guide [OpenLDAP](http://www.openldap.org/) is used. It also uses an XML Database which in this case is [eXist](http://exist-db.org/exist/apps/homepage/index.html). However, other options are available but the current version of CRISTAL-ISE has been heavily tested with these components therefore they are recommended for use.

The kernel library itself provides only the CRISTAL-ISE runtime with a very basic set of descriptions, and to obtain a usable server and client additional components will be needed. The project _cristalise-dev_ has been created to provide a CRISTAL-ISE application that can introduce users to the creation and management of descriptions. This is based on an OpenLDAP directory (provided by cristalise-ldap.jar) and the eXist Native XML Database (using cristalise-xmldb.jar).

The current _cristalise-dev_ package can be downloaded from: http://dev.cccs.uwe.ac.uk/cristalise-dev-3.1-beta.zip
or they can be pulled directly from github. The server can work both on Linux and Windows.

### Java 
**CRISTAL-iSE requires Java 1.8 or higher**. It works with both the standard Oracle JDK and OpenJDK. You can either use the standard Java JRE, or get a server version. 
  * Set JAVA_HOME, add java/bin dir to PATH, associate .jar with bin/java.exe -jar
  * Latest OpenJDK on Linux is available from your package repository.

### eXist

* Download from: http://exist-db.org
* Install it as a service/daemon
* Default username and password is admin/admin.
    * Change this if you need it to be secure, but make a note of your settings, as it is needed to configure CRISTAL-iSE
* In linux it is advisable to see if you can find it in your package management system. Or you can download it and install it manually into /opt or your home directory.
* You can test the eXist installation by using this url: [http://localhost:8080/](http://localhost:8080/)

### OpenLDAP

_There are 2 ways to configure OpenLDAP, the static configuration based on editing slapd.conf file, and the runtime configuration (RTC) system using LDAP client tools. We use the static configuration on windows, on ubuntu we recommend to use the RTC method._

**On Windows - Static slapd.conf**

1. Install [Cygwin](http://cygwin.com)
   * Download and run [setup-x86.exe](https://cygwin.com/setup-x86.exe)
   * Select **openldap-server** and **cygrunsrv** components - consult [this tutorial](http://gagsap37.blogspot.co.uk/2014/04/setting-up-openldap-on-windows-using.html) for further details
1. Copy [cristal.schema](https://github.com/cristal-ise/lookup-ldap/blob/master/openldap/cristal.schema) into `/etc/openldap/schema/`
    * On Windows this directory can be found in `C:\cygwin\etc\openldap` provided you used the default cygwin installation directory
1. Edit `/etc/openldap/slapd.conf`
    * Add following line:`include /etc/openldap/schema/cristal.schema`
    * Set suffix, rootdn, rootpw (see example bellow)
        * Note of your settings, as it is needed to configure cristal
1. Install as Windows service by executing the **cygrunsrv** command provided in [this tutorial](http://www.rigsb.net/2009/04/16/run-openldap-as-a-windows-service-via-cygwin):
    * `cygrunsrv --install OpenLDAP --path /usr/sbin/slapd --args "-d 0 -h ldap://localhost:389 -f /etc/openldap/slapd.conf" --desc OpenLDAP/Cygwin`

The final version of your `slapd.conf` should look something like this: 

    include        /etc/openldap/schema/core.schema
    include        /etc/openldap/schema/cristal.schema

    modulepath     /lib/ldap/
    moduleload     back_bdb.so	

    database       bdb
    suffix         "o=cern,c=ch"
    rootdn         "cn=Manager,o=cern,c=ch"
    rootpw         slap
    index          objectClass eq
    index          cn eq,pres,sub

This is the LDAP section in the cristal clc file used to run integration test on localhost (Windows).

    // LDAP Lookup config
    LDAP.GlobalPath=o=cern,c=ch
    LDAP.RootPath=cn=cristal
    LDAP.LocalPath=cn=integTest
    LDAP.port=389
    LDAP.host=localhost
    LDAP.user=cn=Manager,o=cern,c=ch
    LDAP.password=slap

**On Ubuntu - Runtime Configuration**<br>
Read this documentation it is very informative if you are not familiar with LDAP - https://help.ubuntu.com/lts/serverguide/openldap-server.html. The steps bellow are extracted from this guide.

1. Check the /etc/hosts file. If there is no domain specified for `127.0.1.1 ` use `dc=nodomain` instead of `dc=example,dc=com`
1. Install and configure ldap packages:
    * `sudo apt install slapd ldap-utils`
    * `sudo dpkg-reconfigure slapd`
        * This is not required in the server guide, but the values you provide during the process are essential to configure CRISTAL-iSE (see clc file fragment bellow)
1. Check if install is correct:
    * `ldapsearch -x -LLL -H ldap:/// -b dc=nodomain dn`
    * `sudo ldapsearch -Q -LLL -Y EXTERNAL -H ldapi:/// -b cn=config dn`
1. Upload cristal schema. The converted schema in ldif format is available here: [cristal.ldif](https://github.com/cristal-ise/lookup-ldap/blob/master/openldap/cristal.ldif)
    * `sudo ldapadd -Q -Y EXTERNAL -H ldapi:/// -f cristal.ldif`
1. Check if cristal schema was added correctly:
    * `sudo ldapsearch -Q -LLL -Y EXTERNAL -H ldapi:/// -b cn=config dn`
```
    dn: cn=config
    dn: cn=module{0},cn=config
    dn: cn=schema,cn=config
    dn: cn={0}core,cn=schema,cn=config
    dn: cn={1}cosine,cn=schema,cn=config
    dn: cn={2}nis,cn=schema,cn=config
    dn: cn={3}inetorgperson,cn=schema,cn=config
    dn: cn={4}cristal,cn=schema,cn=config      <- this is a new line
    dn: olcBackend={0}mdb,cn=config
    dn: olcDatabase={-1}frontend,cn=config
    dn: olcDatabase={0}config,cn=config
    dn: olcDatabase={1}mdb,cn=config
```
This is the LDAP section in the _cristal clc_ file used to run integration test on localhost (Linux/Ubuntu). Most of these config values are specified while you run `sudo dpkg-reconfigure slapd`:

    // LDAP Lookup config
    LDAP.GlobalPath=dc=nodomain
    LDAP.RootPath=cn=cristal
    LDAP.LocalPath=cn=integTest
    LDAP.port=389
    LDAP.host=localhost
    LDAP.user=cn=admin,dc=nodomain
    LDAP.password=cristal


Running the CRISTAL-ISE server and Swing UI
---------------------------------------

If you haven't already, download the Cristalise-dev demo package from http://dev.cccs.uwe.ac.uk/cristalise-dev-3.1-beta.zip and unzip it. Or alternatively perform a pull from GitHub.

In the /conf directory, create a 'local.clc' file from the example provided. Edit this with a text editor, and replace the HOSTNAME with your computer name or 'localhost', insert your LDAP parameters into LDAPROOTPATH, LDAPHOSTNAME, LDAPROOTUSER and LDAPPASSWORD, and replace EXISTHOSTNAME, EXISTROOTUSER and EXISTPASSWORD with your eXist-db connection parameters. 

You can now start your server in a local console using the 'cristalise-server' bash script or the 'cristalise-server.bat' Windows batch file in the /bin directory. As the server starts, it will import its own bootstrap descriptions, followed by the cristalise-dev descriptions that you will use to create your own.

Launch the Swing UI using the 'cristalise-gui' bash script or the 'cristalise-gui.bat' Windows batch file. Log in with the demo cristalise-dev agent, username: dev, password: test. 

Now you can explore the bundled descriptions in the domain tree on the left. In /desc/dev you will find the bundled factories that you can use to create workflows, schemas, scripts and Item descriptions.