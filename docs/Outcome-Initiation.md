The default behaviour for [[Job execution|JobExecution]] is for a new [[Outcome]] XML string to be created by the Agent from scratch and set in the [[Job]] object before execution is requested, but it is possible for the Job Outcome to have some initial state when the Agent receives it. The Agent is ultimately responsible for the preparation of the Outcome, so any initial state is not final and may be edited or even discarded by the Agent, as long as an Outcome is set on execution that validates against the defined XML Schema. 

If the 'Viewpoint' activity property is  set, then the activity is considered as an editor of that viewpoint, so new Jobs will return the previous Outcome defined if present for that Viewpoint when getOutcome is called. Otherwise the outcome will be initially empty, unless an OutcomeInitiator is defined for that Activity.

OutcomeInitators can be defined to construct an initial state for the Outcome, which in the case of simple automatic Agents may also be the final state. They are implementations of the Java interface org.cristalise.kernel.persistency.outcome.OutcomeInitiator, which defines two methods:

* String initOutcome(Job job) - return an initial state of the Outcome XML string for the given Job
* Outcome initOutcomeInstance(Job job) - return an initial state of the Outcome for the given Job

This implementation is given in the [[CRISTA-iSEL Properties|CRISTALProperties]], mapping a keyword onto a Java class which is the implementation of the OutcomeInitiator interface. The property key should be 'OutcomeInit.*keyword*', and the value is either the Java classname or a singleton instance of the initiator. That initiator will then be invoked for every activity that contains the keyword as the value of its 'OutcomeInit' property.

Kernel provides one implementation [QueryOutcomeInitiator](../blob/master/src/main/java/org/cristalise/kernel/persistency/outcome/QueryOutcomeInitiator.java). It executes the [[Query]] associated with the Activity when the Job contains `OutcomeInit = Query` Activity property. Also such Query will not be executed during Job execution (i.e. `AgentProxy.execute(job)` ) to avoid potential override of user provided values. In order to use itt add the following System property settings to your module.xml or clc file:

`OutcomeInit.Query = org.cristalise.kernel.persistency.outcome.QueryOutcomeInitiator` 
