An Event is a record of a [change of state](../StateMachine) of an [Activity](../Activity) in the [Workflow](../Workflow) of an [Item](../Item). Some Events may have [Outcome](../Outcome) data associated with them, usually associated with state transitions that complete the activity, but this is configurable.

Event data includes the following:

 * The activity name, type and path.
 * The activity transition performed, and the state machine used.
 * The date and time that the transition was performed.
 * The executing Agent name and the role they used to perform the transition.
 * An integer ID, in a sequence starting from zero.

In the case of events that have an associated Outcome, the following data is stored:

 * The Schema name and version.
 * The Viewpoint name, if specified by the Activity (_'last'_ is not recorded in the Event unless it is specifically named in the Activity properties).
