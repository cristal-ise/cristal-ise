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

