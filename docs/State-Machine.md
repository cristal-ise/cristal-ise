In CRISTAL 3.x, the hard-coded State Machine of CRISTAL 2.x was replaced with a described one, defined in StateMachine Items. 

- Each State and Transition are identified by integer ID, and have non-unique names. 
- Each State indicates whether it is a terminal state that triggers the Workflow to proceed.
- Each Transition can:
  - Name a boolean Activity Property that can enable or disable it.
  - Name a Role that may override the Activity assigned Role for that Transition
  - Indicate how it may change the Agent reservation: Set, clear or preserve
  - Specify a Script that must be run during the Transition.
  - Specify a Query that must be executed during the Transition.
  - Specify that an Outcome may be supplied, referencing its [XMLSchema](../XMLSchema).
  - Indicate that the Outcome is required.

Values for these properties can be explicit, or reference Activity properties like this: `${SchemaType}`. The referenced properties may also invoke [data helpers](../DataHelper) to reference other Item data.


### State Machine Definitions provided by CRISTAL-iSE Kernel

Three state machines are bundled with the kernel:

- **Default** - a simplified version of the CRISTAL 2.x state machine, including only the Waiting, Started, Suspended and Finished states, and the Start, Done, Complete, Suspend and Resume transitions - check the diagram bellow for more details. The diagram can be changed by editing this gist: [CRISTAL-iSEDefaultSM.puml](https://gist.github.com/kovax/f61ec3c44656fee0fa904fe402becec2)
![DefaultStateMachine](https://www.plantuml.com/plantuml/img/NP51ImCn48NlyolUMLZ1Qm_IedWIArRm81wokzCsi9aioQIm_zvPirRRNF9ctkJD1rbdTKbB6EZpvWlDFJwSLoy7McuqrjlDCzolghYmdhpuVEGUApn6OLf6hxs76CQ1zTBVbJooz3y3YwQ_th_5vhznBe7f77cL_vfYnzmJILI6otYv8-3PPqNSupqvoSOS1PqJ7Ds9iItQHKk4dQQoLtb6hUKlc9ML0S5DQ1aZftrCmTQt4vnCsFJ12r8SvgTjKNZ5c3XpEd7As1nO5BLsCyijMIvcO5gRiZ_u1W00)


- **PredefinedStep** - a dummy state machine for [PredefinedStep](../PredefinedStep)s. Features just one state, Available, and one transition, Done, which loops on it, allowing an Outcome of PredefinedStepOutcome v0.

- **CompositeActivity** - a state machine for [CompositeActivities](../CompositeActivity). Can be Waiting, Started or Finished. Transitions are Start, Complete or Loop (which reinitialized the activity, and run with the 'RepeatWhen' activity property). No outcome. 

Each Activity sub-class specifies which state machine it uses by default with the getDefaultSMName method. They may all be overridden with the StateMachineName and StateMachineVersion Activity properties.

### Custom State Machines

Activities can have their own state machines defined. Each state machine contains state and transition definitions, containing the following data in its XML, as defined by the StateMachine kernel schema:

#### State
 
* _int_ **id** - State id. This is stored in each activity instance that uses this state machine. Usually numbered in sequence starting from 0. It is important to note that state ids are only unique within the current state machine, and will reference different states in different machines.
* _String_ **name** - State name. Human readable.
* _boolean_ **proceeds** - The state is a terminal state, and the activity should advance the workflow with a call to runNext if it is reached. 

#### Transition

* _int_ **id** - Transition id. Each job refers to a specific transition by this id. Not stored in the activity.
* _String_ **Name** - Transition name. Human readable.
* _int_ **origin** - The state id where this transition is available.
* _int_ **target** - The state in which activities will end up when executing this transition.
* _String_ **enablingProperty** - the activity property that enables this state change. Should either contain a boolean value, or reference a [DataHelper](../DataHelper) that will supply one. The Transition is disabled if this property is undefined.
* _boolean_ **reinitializes** - indicates whether this transition reinitializes the state of the activity. This applies to composite activities, which will reset their child workflow on execution. Used for looping.
* _String_ **roleOverride** - overrides the activity defined agent role for this transition. The value may explicitly name a role, or invoke a DataHelper to give one.
* _String_ **reservation** - defines the change to the activity's ownership caused by this transition. Can be:
  * set - the activity becomes reserved by the executing agent
  * preserve - the activity owner does not change
  * clear - the activity becomes ownerless
* **Outcome**
  * _String_ **name** - The name of the outcome schema. Can be a DomainPath, a UUID, or reference an activity property using ${this} syntax. The activity property may invoke a DataHelper to supply the value.
  * _String_ **version** - Either an integer schema version number, or an activity property reference as above
  * _boolean_ **required** - Whether or not the job must have a valid outcome set in order to successfully execute.
* **Script**
  * _String_ **name** - The name of the script. Can be a DomainPath, a UUID, or reference an activity property using ${this} syntax. The activity property may invoke a DataHelper to supply the value.
  * _String_ **version** - Either an integer script version number, or an activity property reference as above.
* **Query**
  * _String_ **name** - The name of the query. Can be a DomainPath, a UUID, or reference an activity property using ${this} syntax. The activity property may invoke a DataHelper to supply the value.
  * _String_ **version** - Either an integer script version number, or an activity property reference as above.

The State Machine itself must define an 'initialState', giving an integer state id.
