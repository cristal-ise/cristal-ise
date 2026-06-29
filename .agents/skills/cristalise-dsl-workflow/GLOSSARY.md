# Glossary

This glossary defines the key terms used in CRISTAL-iSE Workflow DSL.

## A

### Activity
A **unit of work** in a workflow. Activities can be Elementary (atomic) or Composite (container for other activities). See: [Activity](../references/activity.md)

### ActivityDef
The **Description** of an Activity, including its properties, schema, scripts, and other metadata.

### AndJoin
The **join vertex** for an AndSplit, which waits for all parallel branches to complete before continuing.

### AndSplit
A **workflow pattern** that enables parallel execution of multiple branches. All branches execute concurrently. See: [Split Patterns](../references/splits.md#andsplit)

### Alias
A **name** for a Block within a Split, used for edge identification and routing. **Required** for Blocks in OrSplit and XOrSplit.

## B

### Block
A **grouping container** for workflow elements that executes its contents sequentially. Blocks are the primary container within Split patterns. See: [Block](../references/block.md)

## C

### CA
Short alias for **CompAct**, a Composite Activity reference.

### CompAct
Alias for **Composite Activity** reference. See: [Activity](../references/activity.md)

### CompActDef
A DSL construct for defining a **Composite ActivityDef** Description within a Workflow. See: [Activity](../references/activity.md)

## D

### Dependency
A reference to another Item or resource that an Item depends on.

## E

### EA
Short alias for **ElemAct**, an Elementary Activity reference.

### ElemAct
Alias for **Elementary Activity** reference. See: [Activity](../references/activity.md)

### ElemActDef
A DSL construct for defining an **Elementary ActivityDef** Description within a Workflow. See: [Activity](../references/activity.md)

### Edge
A **connection** between Vertices in the workflow graph, representing the flow of execution.

## F

### Fan-Out/Fan-In
A **parallel processing pattern** where execution fans out to multiple branches (AndSplit) and then fans back in at a join point (AndJoin).

## I

### Item
The **primary entity** in CRISTAL-iSE that has a lifecycle defined by a Workflow. Each Item has exactly one Workflow.

## J

### Join
The **synchronization point** for Split branches. Joins wait for incoming branches to complete before allowing execution to continue. Types: AndJoin, Join (for OrSplit/XOrSplit), LoopJoin.

## L

### Layout
The **container** for workflow pattern primitives that defines the possible sequences of Activity execution. See: [Layout](../references/layout.md)

### Loop
A **workflow pattern** that enables repetitive execution of Activities with a predefined end condition. See: [Split Patterns](../references/splits.md#loop)

### LoopInfinitive
A **workflow pattern** that enables continuous repetitive execution without a predefined end condition. See: [Split Patterns](../references/splits.md#loopinfinitive)

### LoopJoin
The **join vertex** for a Loop, consisting of LoopJoin_first and LoopJoin_last.

### LoopSplit
The **split vertex** for a Loop.

## O

### OrSplit
A **workflow pattern** that enables conditional execution based on data evaluation. Can execute zero, one, or multiple branches based on routing conditions. See: [Split Patterns](../references/splits.md#orsplit)

### Outcome
The **data structure** produced by an Activity, defined by a Schema.

## P

### Parallel Execution
Execution mode where multiple branches run **concurrently**, enabled by AndSplit.

### Property
A **key-value pair** that configures and customizes workflow elements (Activities, Blocks, Splits, etc.). See: [Properties](../references/properties.md)

## R

### Routing Script
A **script** (Groovy or JavaScript) that determines which branches of a Split should execute. Essential for conditional splits (OrSplit, XOrSplit). See: [Routing Scripts](../references/routing.md)

## S

### Schema
The **structure definition** for Outcomes and data storage, defining Fields, Attributes, and their constraints.

### Script
An **executable code** element (Groovy, JavaScript, etc.) that can be attached to Activities or used for routing.

### Sequential Execution
The **default execution mode** where elements execute one after another in order.

### Split
A **workflow primitive** for branching execution paths. Types: AndSplit, OrSplit, XOrSplit, Loop. See: [Split Patterns](../references/splits.md)

### State Machine
A **model** defining the possible transitions during Activity execution.

## T

### Transition
A **state change definition** within a StateMachine.

## V

### Vertex
A **node** in the workflow graph. Types: Activity (EA/CA), Split, Join.

## W

### Workflow
A **DSL construct** that creates a kernel Workflow, defining the dependency graph of Activities for an Item's Lifecycle. Each Item has exactly one Workflow. See: [Workflow](../references/workflow.md)

## X

### XOrSplit
A **workflow pattern** that enables exclusive conditional execution where only ONE branch is taken based on routing evaluation. See: [Split Patterns](../references/splits.md#xorsplit)

---

## See Also
n- [Binding](../references/binding.md) — Variable binding convention

- [Workflow](../references/workflow.md)
- [Layout](../references/layout.md)
- [Block](../references/block.md)
- [Activity](../references/activity.md)
- [Split Patterns](../references/splits.md)
- [Routing Scripts](../references/routing.md)
- [Properties](../references/properties.md)
- [Examples](EXAMPLES.md)
