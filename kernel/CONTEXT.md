# CRISTAL-iSE

The CRISTAL-iSE implements a Description-Driven Framework where application logic and business objects are configured entirely through metadata rather than hard-coded logic.

## Language

### Core Entities

**Item**:
The fundamental business object in CRISTAL-iSE, analogous to an Entity or Resource in REST applications. Every Item can instantiate other Items through kernel PredefinedSteps.
_Avoid_: Entity, Resource, Object

**Agent**:
An Item with Roles that is entitled to execute Activities. Agents must be authenticated and authorized in the system.
_Avoid_: User, Actor

**Role**:
A named collection of Permissions that can be assigned to Agents.
_Avoid_: Group, UserGroup

**Permission**:
A Shiro WildcardPermission string following the format `domain:action:target` that controls access to Activities on Items.
_Avoid_: AccessRight, Privilege

### Metadata & Configuration

**Description**:
An Item containing all the logic (Lifecycle) to maintain the data describing other Items. Descriptions have Activities for both creating Items and editing the data/structure used to create Items. See [Description types](#Description Types) for more details.
_Avoid_: Template, Blueprint

**Factory**:
A simplified form of Description that only contains Activities for creating Items, without the editing capabilities.
_Avoid_: Builder, Creator

**Property**:
A key-value pair used as metadata by the kernel for identification and typing of Items.
_Avoid_: Attribute, Field, Metadata

### Description Types

**ImportItem**: A Description Item that defines the complete structure, properties, collections, workflow, and lifecycle for creating new Items. _Avoid_: Template, Blueprint

**ImportAgent**: A Description Item that defines an Agent, including its Roles and permissions. _Avoid_: User

**ImportRole**: A Description Item that defines a Role, including its permissions.

**Module**: A Description Item that defines a collection of Items that implement a set of functionalities, with its own namespace and version. _Avoid_: Package, Component, Library

**PropertyDescriptionList**: A Description Item that defines the metadata properties (Property/ItemProperty) used for identification and typing of Items, including their names, default values, and mutability. _Avoid_: Attribute, Field, Metadata

**Schema**: A Description Item that defines the structure of data that is stored or computed in the system, specifying the fields, types, and constraints for Outcomes. _Avoid_: DataModel, DataStructure

**Script**: A Description Item that encapsulates executable code (using JSR-223 API) that can be associated with Activities or execute as GET endpoints. _Avoid_: Code, Function, Procedure

**Query**: A Description Item that encapsulates data selection logic that can execute as GET endpoints or be associated with Activities. _Avoid_: SQLQuery, DataQuery

**StateMachine**: A Description Item that defines the possible transitions during the execution of an Activity, managing internal state within that Activity. _Avoid_: StateDiagram, TransitionModel

**ActivityDef**: A Description Item that defines a Service/Task/Endpoint within an Item's Lifecycle, representing a PUT/POST operation with write transaction. Base class for ElementaryActivityDef and CompositeActivityDef. _Avoid_: Operation, Action, Method

**ElementaryActivityDef**: A Description Item that defines a single, atomic Activity within an Item's Lifecycle. _Avoid_: Operation, Action, Method

**CompositeActivityDef**: A Description Item that defines a composite Activity that coordinates multiple sub-Activities within an Item's Lifecycle. _Avoid_: Operation, Action, Method

**DomainContext**: A Description Item that defines the domain namespace and context for Items. _Avoid_: Path, Domain, URI

### Relationships

**Collection**:
An abstract relationship container that references other Items. Collections can restrict membership by type and may have fixed or flexible slots.
_Avoid_: Relationship, Association

**Dependency**:
A concrete Collection implementation containing a variable number of members (like an array or list). Dependencies never contain empty slots or duplicated members.
_Avoid_: List, Array

**Aggregation**:
A concrete Collection implementation with a graph layout for modeling real-world compositions on a two-dimensional canvas.
_Avoid_: Composition, Structure

### Lifecycle Management

**Activity**:
A Service/Task/Endpoint within an Item's Lifecycle that represents a PUT/POST operation with write transaction. All state changes of an Item are the direct consequence of executing Activities.
_Avoid_: Operation, Action, Method

**Workflow**:
The dependency graph of Activities that represents the complete Lifecycle of an Item. Workflows calculate the availability of Activities.
_Avoid_: Process, Flow, BusinessProcess

**StateMachine**:
A Description defining the possible transitions during the execution of an Activity. Each Activity has its own StateMachine for internal state management.
_Avoid_: StateDiagram, TransitionModel

**PredefinedStep**:
A special Activity implemented within the kernel that executes core functionality during Activity transaction processing.
_Avoid_: BuiltInActivity, KernelActivity

### Data & Execution

**Outcome**:
Data obtained during the execution of an Activity. Outcomes must conform to a Schema and can be submitted by users or generated by Scripts.
_Avoid_: Result, Output, Response

**Schema**:
A Description storing the definitions of data that is either stored or computed in the system. Schemas define the structure that Outcomes must conform to.
_Avoid_: DataModel, DataStructure

**Viewpoint**:
A named version of an Outcome that serves as a shortcut to retrieve specific versions. The default Viewpoint 'last' always points to the most recent Outcome.
_Avoid_: VersionPointer, Alias

**Script**:
A Description encapsulating a piece of code that can be executed. Scripts use JSR-223 API (Groovy, JavaScript) and can execute as GET endpoints or be associated with Activities.
_Avoid_: Code, Function, Procedure

**Query**:
A Description encapsulating data selection logic (currently only SQL). Queries can execute as GET endpoints or be associated with Activities.
_Avoid_: SQLQuery, DataQuery

### Navigation & Storage

**DomainPath**:
The domain name of an Item, following the pattern `/Module/Type/Name`. An Item can have multiple DomainPaths with arbitrary nesting levels.
_Avoid_: Path, Domain, URI

**ClusterType**:
The storage category for different object types in the kernel's persistency system.
_Avoid_: StorageType, Category

**Module**:
A collection of Items that were created to implement a set of functionalities. Modules have their own namespace (DomainPath segment) and version, and are independent deployment units.
_Avoid_: Package, Component, Library

### Security

**SecurityManager**:
The kernel wrapper around Apache Shiro API that handles Agent authentication and permission checking for Activity execution.
_Avoid_: AuthManager, PermissionManager
