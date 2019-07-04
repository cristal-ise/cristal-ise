CRISTAL [[Agent]]s need to authenticate with CRISTAL to receive jobs from the system and execute them. An Agent may be a human user interacting with a graphical interface, an instrument submitting measurements over a network protocol, or a software agent generating analyses of existing data. Authentication is performed in the CRISTAL client API using a connect() method of the Gateway. For client processes, a username and password is passed to this method. CRISTAL stores Agent entities in the LDAP directory as LDAP users, so the Gateway attempts to connect to the LDAP server as the given user. If successful, the method returns an [[AgentProxy]] object for that user.

For server processes, the connect() method that takes no arguments which attempts to authenticate against the directory using superuser credentials already supplied in the CRISTAL properties. This gives the server process write access to the directory, and the ability to write data for any user. Client processes should never normally have direct write access to any of the data stores of CRISTAL, especially not when a server process is running.

## Connecting to CRISTAL from a Java

To use CRISTAL from a Java program, the [[Gateway]] class must be initialized with a set of [[properties|CRISTAL_Properties]], then passed credentials to authenticate.

```java
Properties props = new Properties(propsFile);
Gateway.init(props);
AgentProxy agent = Gateway.connect("user", "hunter2");
```

At this point, all of the CRISTAL singleton manager objects will be properly initialized, and can be accessed in the Gateway.

In a multi-user environment such as a web server, the connect method can be called without credentials, to default to whichever generic credentials exist in the properties set. Then the login() method is used to authenticate and obtain an AgentProxy for each user without affecting the Gateway singletons. This AgentProxy may be stored with its user's context in your application (e.g. the session).


```java
Properties props = new Properties(propsFile);
Gateway.init(props);
Gateway.connect("user", "hunter2");

...

AgentProxy agent = Gateway.login("user", "hunter2");
```

The Gateway should be closed prior to the process exiting. This will disconnect from all databases, and stop any CRISTAL threads.

```java	
Gateway.close();
```
## Initializing a CRISTAL process using AbstractMain

source:/src/main/java/com/c2kernel/process/AbstractMain.java

A [[command-line|Command-line_Arguments]] parser exists in CRISTAL to initialize the CRISTAL Properties object by merging a connection-specific config file (called a 'CRISTAL Local Centre' file, or CLC) with client/server specific config. It also provides logging options. It can be used by either extending the StandardClient class, or by accessing AbstractMain.readC2KArgs directly.

In your main():

```java
	Gateway.init(readC2KArgs(args));
```

## Creating a UserCode process (Job consumer)

source:/src/main/java/com/c2kernel/process/UserCodeProcess.java

A UserCode process is a worker process. If an Agent Role is configured to push active Jobs, Agents holding that Role will have Job objects pushed to them by Activities as they change state. UserCode processes subscribe to changes in their Agent joblist, and so can execute those Jobs as soon as they become available, in order to perform calculations or make other non-interactive changes to Items.

By default, a UserCodeProcess executes every START and COMPLETE job it receives, relying on Scripts defined in the Activities to generate Outcomes. Developers may alter this behaviour by overriding the following methods:

* `public boolean assessStartConditions(Job job)` - returns true if this process should execute the START job. Note that if 'false' is returned, this process will not get another chance to execute that job until it is restarted or the Job is reissued by a change in Workflow state.
* `public void runUCLogic(Job job)` - perform the actions required by the Activity, assign the resulting Outcome to the given Job, and then execute it using the 'agent' member.

