Standard Job execution serves to collect Events and Outcomes as the Item's lifecycle progresses, but do not alter any other Item data such as Properties or Collections. Client processes may not alter this data directly, as they have no write access to the kernel objects, so a special mechanism is required to allow client processes to perform changes to Item structures in a traceable way. This mechanism is provided through special Activities called [PredefinedStep](../PredefinedStep)s.

Predefined Steps are held in a parallel container to the Item's domain lifecycle in the Workflow object. They are not persistent - they are all permanently available to Agents with permission to call them. They are not visible or schedulable in the ordinary domain workflow, and they are never returned in job queries. They are intended to be called from Scripts, run during the execution of domain Activities, which take an Outcome from an Agent, validate and process it, then call Predefined Steps with the processed data which will perform the required changes to the Item.

Most Predefined Steps take a standard outcome: PredefinedStepOutcome, which defines a list of Param elements which each contain a String. The number and contents of parameters varies according to the step. The full list of steps and their parameters can be found on the [PredefinedStep](../PredefinedStep) page. 

## Calling Predefined Steps

From a Script, two AgentProxy methods exist to simplify the calling of Predefined Steps.

* `execute(ItemProxy item, String stepName, String... params)` - call the Predefined Step referenced by 'stepName' with the parameters given in a String array. The PredefinedStepOutcome will be assembled automatically.

e.g. adding an Item to the collection of another

```java
	agent.execute(item, "AddMemberToCollection", "members", otherItemProxy.getPath().getUUID());
```
* `execute(ItemProxy item, String stepName, C2KLocalObject obj)` - call the Predefined Step referenced by 'stepName' with one parameter containing the XML marshalled form of the object given in 'obj'. This is an administrative method used to directly replace Item objects through the Steps 'ReplaceDomainWorkflow' and 'AddC2KObject'. Most scripts should use the first method, which better protects the Items from invalid data, and provides better traceability.

e.g. creating a new Dependency collection in an Item

```java
	agent.execute(item, "AddC2KObject", new Dependency("members"));
```