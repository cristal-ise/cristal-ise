**This page needs to be rewritten becuase many of the new Propoerties are not documented here. Until this is fixed please use the JavaDoc of the BuiltInVertexProperties class instead.**

All [[Activities|Activity]] contain name/value properties that configure them. Certain properties are used by the kernel, but additional properties may be added by the domain to be used by [[Script]]s and Agent implementations. [[Job]]s contain a copy of all activity properties.

At the description level, both Activity descriptions and their referencing Activity slots in Composite Activity descriptions may define properties. When the Activity is instantiated, the two sets are merged, with the slot properties overwriting any Activity properties of the same name. Generic Activities may be parametrized in this way, and Activity properties may be defined as abstract to indicate that they must be overridden in slots, and instantiation will fail if they are not.

Activity property values may be String, Integer, Boolean or Float objects, though this cannot yet be constrained at the description level. They are stored using the `org.cristalise.kernel.utils.KeyValuePair` class.

Properties used by the kernel are initialized with a new Activity or Activity description:

| Name | Type | Default Value | Purpose |
|------|------|---------------|---------|
| Description | String | *none* | A human-readable prose description of the purpose of the Activity |
| Agent Role | String | *none* | The role which this Activity is restricted to, if any. Agents not holding this role will not receive Jobs from this Activity, nor be able to perform transitions on it |
| Agent Name | String | *none* | The Agent for whom this activity is reserved, if any |
| Viewpoint | String | *none* | Provides the name or value of the [[Viewpoint]] to be updated |
| Property.${name} | String | *none* | Provides the value to update the named Item [[Property]]  |
| StateMachineName | String | Default, CompositeActivity or PredefinedStep, depending on Activity subclass | The [[StateMachine]] that this Activity should use |
| StateMachineVersion | Integer | 0 | The state machine version to use. |
| Breakpoint | Boolean | false | If set, the workflow will not proceed to the next Activity after this one completes, to help with debugging |
| OutcomeInit | String | *none* | The name of the [[Outcome-Initiation]], defined by a module, that should be used to initialize a new Outcome for Agents to use as a template |

The following properties are used by the default state machines of the kernel, but not in the code directly.

| Name | Type | Default Value | Purpose |
|------|------|---------------|---------|
| SchemaType | String | *none* | Specifies that this Activity requires an Outcome, and gives the name of the [[XMLSchema]] item against which to validate that Outcome. |
| SchemaVersion | String or Integer | *none* | The version of the XML Schema to use. |
| ScriptName | String | *none* | Specifies that the named Script should be run on completion of this Activity |
| ScriptVersion | String or Integer | *none* | The version of the Script to run |
| QueryName | String | *none* | Specifies that the named Query should be run on completion of this Activity |
| QueryVersion | String or Integer | *none* | The version of the Query to run |
 