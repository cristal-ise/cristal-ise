**WARNING: This page has been copied raw from the CRISTAL 2.x wiki for the purposes of preservation, so may be out-of-date, links may be broken, and the formatting is probably rubbish.**

An Activity represents a task that needs to be done by an [[Agent]] as part of the lifecycle of an Item. An Activity may traverse several states during execution, as dictated by its [StateMachine](State-Machine).

Activities have:

 * A *name*
 * A *path* from the root of the [[Workflow]]. May have several components if the workflow contains [[CompositeActivities]].
 * A *type*, which is the name of the [[ElementaryActivityDescription]] it was instantiated from.
 * A boolean indicating whether the Activity is *active* or not. It may only be executed if active.
 * A *[[State-Machine]]* to track its execution, and its current state.
 * *[[ActivityProperties]]*. A set of name/value pairs that define the behaviour and requirements of the Activity. The name is always a String, but the value may be a String, a Boolean or an Integer. At the time of writing, the following properties are used by the kernel during execution, though it is planned to migrate some of the description references to collections to improve semantic possibilities. Already in the 3.0 kernel, the Schema and Script property names are defined in the default State Machine, and so may be overriden already.
  * *SchemaType* and *SchemaVersion*: Indicate the [[XMLSchema]] which defines the [[Outcome]] of this Activity. The submitted Outcome must be valid according to the named version of the Schema.
  * *ScriptName* and *ScriptVersion*: Gives the [[Script]] that must be run by the AgentProxy of the client process during completion of the Activity.
  * *Viewpoint*: provides the name of the [[Viewpoint]] that should be created or modified to point to the Activity's Outcome. In the CRISTAL GUI, this property will cause that Viewpoint to be loaded into the form during execution, so the previous Outcome can be edited.
  * *StateMachineName* and *StateMachineVersion*: Overrides the [[StateMachine]] to be used for this Item.
  * *Description* - A natural language description of the Activity's purpose.
  * *Breakpoint* - If set to true, the Activity will not proceed as it should do when complete, rather the workflow will pause until manually reactivated.
  * *Agent Role* - Restricts the execution of the Activity to a particular Role. May be overriden by an administrator, or if 'RoleOverride' is used in the State Machine definition. 
  * *Agent Name* - Restricts the execution of the Activity to a particular Agent. The State Machine may set this during execution to lock the Activity, if necessary.
