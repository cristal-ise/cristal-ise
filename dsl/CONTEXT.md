# Domain-Specific Language

Groovy DSL wrapper for CRISTAL-iSE kernel concepts, providing user-friendly textual definitions for Items, Schemas, Workflows, Scripts, Queries, and their components.

## Language

### Schema DSL

**Schema**:
A DSL construct that creates a kernel Schema Description, defining the structure for Outcomes and data storage.
_Avoid_: DataModel, DataStructure

**Struct**:
A named container within a Schema that groups related Fields and Attributes.
_Avoid_: Group, Section

**Field**:
A typed data element within a Struct, with optional constraints, validation, and UI configuration.
_Avoid_: Column, DataField

**Attribute**:
A metadata element attached to a Struct or Field, providing additional descriptive information.
_Avoid_: MetaField, Annotation

**AnyField**:
A flexible Field type that can accept any data structure.
_Avoid_: GenericField, DynamicField

**Expression**:
Computed field logic that derives values from other Fields using Groovy expressions.
_Avoid_: ComputedField, Formula

**DynamicForm**:
UI configuration for Fields, including labels, containers, masks, and visibility rules.
_Avoid_: UIHint, FormConfig

**Warning**:
Validation rule that produces a message when a Field's value fails a specified condition.
_Avoid_: ValidationError, Alert

**ListOfValues**:
Predefined set of values for a Field, optionally populated from a Query reference.
_Avoid_: Dropdown, Enum, Picklist

**Reference**:
A Field constraint that restricts values to Items of a specified type.
_Avoid_: ForeignKey, ItemRef

**Unit**:
Unit of measurement configuration for numeric Fields, including valid values.
_Avoid_: Measurement, UOM

**TabularSchema**:
A Schema defined via CSV or Excel tabular data, using special column headers to declare Structs, Fields, and their properties.
_Avoid_: SpreadsheetSchema, CSVSchema

**SchemaGenerator**:
An abstraction that transforms the parsed Schema Struct representation into a specific target format.
_Avoid_: SchemaExporter, SchemaSerializer, OutputFormatter

**JsonSchema**:
A JSON Schema representation generated from a Schema definition, defining structural and validation constraints for JSON data payloads.
_Avoid_: JsonDataModel, JsonSpec

**NgForgeConfig**:
A dynamic form configuration generated from a Schema definition for rendering Angular forms with @ng-forge/dynamic-forms.
_Avoid_: AngularFormConfig, ForgeFormDef

### Script DSL

**Script**:
A DSL construct that creates a kernel Script Description, encapsulating executable code (Groovy, JavaScript, etc.).
_Avoid_: Code, Function, Procedure

**param**:
A parameter definition for a Script, specifying name and type.
_Avoid_: parameter, argument

**input**:
An input binding for a Script, specifying the name and expected type of data passed into the script.
_Avoid_: in, inputParam

**output**:
An output binding for a Script, specifying the name and type of data returned by the script.
_Avoid_: out, outputParam

### Workflow DSL

**Workflow**:
A DSL construct that creates a kernel Workflow, defining the dependency graph of Activities for an Item's Lifecycle.
_Avoid_: Process, Flow, BusinessProcess

**Layout**:
Container for workflow pattern primitives that defines the possible sequences of Activity execution.
_Avoid_: Diagram, Structure

**AndSplit**:
A workflow pattern that enables parallel execution of multiple branches.
_Avoid_: ParallelSplit, Fork

**OrSplit**:
A workflow pattern that enables conditional execution based on data evaluation.
_Avoid_: ConditionalSplit, Choice

**XOrSplit**:
A workflow pattern that enables exclusive conditional execution where only one branch is taken.
_Avoid_: ExclusiveSplit, Switch

**Loop**:
A workflow pattern that enables repetitive execution of Activities.
_Avoid_: Repeat, Iterate

**LoopInfinitive**:
A workflow pattern that enables continuous repetitive execution without a predefined end condition.
_Avoid_: InfiniteLoop, WhileTrue

**Act**:
A reference to an ActivityDef within a Workflow Layout definition.
_Avoid_: ActivityRef, Call

**Block**:
A grouping container for workflow elements that executes its contents sequentially.
_Avoid_: Sequence, Group

**CompActDef**:
A DSL construct for defining a Composite ActivityDef Description within a Workflow.
_Avoid_: CompositeActivity, ContainerActivity

**ElemActDef**:
A DSL construct for defining an Elementary ActivityDef Description within a Workflow.
_Avoid_: ElementaryActivity, SimpleActivity

**Split**:
A workflow primitive for branching execution paths.
_Avoid_: Branch, Divide

**Transition**:
A state change definition within a StateMachine.
_Avoid_: StateChange, Move

**StateMachine**:
A DSL construct that creates a kernel StateMachine Description, defining the possible transitions during Activity execution.
_Avoid_: StateDiagram, TransitionModel

### Query DSL

**Query**:
A DSL construct that creates a kernel Query Description, encapsulating data selection logic (typically SQL).
_Avoid_: SQLQuery, DataQuery

**parameter**:
A parameter definition for a Query, specifying name and type for runtime substitution.
_Avoid_: param, queryParam

**rootElement**:
The name of the root XML element in Query results.
_Avoid_: root, resultRoot

**recordElement**:
The name of the repeating record element in Query results.
_Avoid_: record, rowElement

### Property DSL

**Property**:
A DSL construct for defining Item metadata as key-value pairs, used for identification and typing.
_Avoid_: Attribute, Metadata, Tag

### Collection DSL

**Dependency**:
A DSL construct for defining a variable-member Collection that references other Items.
_Avoid_: List, Array, Relationship

**Aggregation**:
NOT Implemented in DSL yet
_Avoid_: List, Array, Relationship
