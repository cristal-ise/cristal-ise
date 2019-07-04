Client processes have only read-only access to the persistency and directory layers, and may not write to Item data directly. All changes to Items must be performed through the execution of [[Job]]s, which represents a possible state transition of an Activity of an Item's lifecycle. Each transition can allow or require some data to be sent and recorded along with the transition, and this data is known as the Outcome of the Activity. Usually, transitions that result in the completion of the Activity and cause the workflow to proceed to the next step are the transitions that can take Outcomes, but this depends on the state machine definition of the Activity. Outcomes are namespace-less XML fragments that must be valid to the schema specified by the Job, which was traditionally taken from the SchemaType and SchemaVersion Activity properties, but now can vary according to the state machine.

In the Default [[State-Machine]], Outcomes are required on the DONE and COMPLETE transitions if the SchemaType and SchemaVersion properties are defined, or optionally on the SUSPEND transition, which requires a marshalled ErrorInfo object, described by the 'Errors' schema. This is provided to allow UserCode processes to use the SUSPENDED state as an Error state, and report their execution errors to the system.

A client process that has authenticated as an Agent may obtain jobs either passively or actively:

* Passive - the Agent holds a Role that has job pushing enabled. When Activities that hold that role change state, jobs are automatically added to each Agent's Joblist. The Joblist is a [[ClusterStorage]] context, and a [[RemoteMap]] implementation like [[History]] and so can be subscribed to for push notification. 

* Active - the Agent may query any [[ItemProxy]] for jobs using the *getJobList* method, or query for Jobs of a particular activity using *getJobByName*.

Each Job contains information about the Activity that generated it, including a copy of its property table, and can be queried for information about the requested transition and the Outcome data required by the Job. Client processes submit their completed Outcome to the Job, and use the AgentProxy.execute(Job) method to request the transition in the Item's workflow. If successful, the Item's lifecycle will change state, and a new Event will be stored at the end of the History. If an Outcome accompanies the Event, that will be stored in /Outcome/SchemaName/SchemaVersion/EventId. A Viewpoint will be created or updated in /ViewPoint/SchemaName/last, and if a specific Viewpoint name was given in the Activity properties, that will be updated too.

## Job methods

Client processes can interact with Jobs to discover and parametrize the work it must do using the following methods:

### Activity properties

* `HashMap<String, Object> getActProps()` - gets the HashMap of all properties
* `Object getActProp(String name)` - gets the named property as an Object
* `String getActPropString(String name)` - gets the named property as a String (null guarded)

### Outcomes

* `requiresOutcome()` - returns true if the Job needs to have an Outcome set to successfully execute
* `isError()` - returns true for Jobs which can take an error message as their Outcome (SUSPEND)
* `hasOutcome()` - returns true if it is possible for the Job to have an Outcome set
* `getOutcome()` - gives an [[Outcome]] object if one has already been set
* `getOutcomeString()` - the raw XML outcome
* `setOutcome(String xml)` - sets the Outcome string of the Job. Note that this is a different parameter than the return type of getOutcome(), because the rest of the Outcome object is fixed by Job properties and thus built automatically around the XML.
* `getSchemaType()`, `getSchemaVersion()` - the name and version of the Schema that any Outcome is required to validate against. 
* `setError(ErrorInfo errors)` - sets the Outcome to the XML marshalled form of the given ErrorInfo object, which can hold multiple error lines and a flag indicating whether or not the error was fatal and so blocked execution.

### Other Job properties

These may be read and/or written using get/set methods in the Job:

* AgentId - the system key of the executing Agent
* AgentName - the name of the executing Agent. Stored in the activity property 'Agent Name'
* AgentRole - the role that the executing Agent is required to hold.
* CurrentState - the current Activity state id, see [[StateMachine]]
* TargetState - the Activity state id after a successful transition, see [[StateMachine]]
* Description - the Activity description (long text description of the work to be done)
* PossibleTransition - the Activity transition id that will be performed, see [[StateMachine]]
* StepName, StepType, StepPath - Activity identity information.

## A Job query and execution

This code sample creates a new Script Item using the cristal-dev ScriptFactory, by fetching a Job from its ItemProxy, setting the required Outcome, and executing the Job using an AgentProxy (obtained during [[Authentication|login]]):

```java
ItemProxy scriptFactory = (ItemProxy)Gateway.getProxyManager().getProxy(new DomainPath("/desc/dev/ScriptFactory");
Job createJob = scriptFactory.getJobByName("CreateNewScript");
createJob.setOutcome("<NewDevObjectDef><ObjectName>MyNewScript</ObjectName><SubFolder/></NewDevObjectDef>");
agent.execute(createJob);
```