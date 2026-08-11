# Activities

**Activities** are the units of work in a workflow. They can be Elementary (EA) or Composite (CA).

## Activity Types

| Type | Description | DSL Construct |
|------|-------------|---------------|
| Elementary Activity | Simple, atomic unit of work | ElemActDef, ElemAct, EA |
| Composite Activity | Container for other activities/workflows | CompActDef, CompAct, CA |

## Activity References

### Act

**Act** references an existing ActivityDef:

```groovy
// With explicit name for the workflow vertex
Act('WorkflowVertexName', $activityDef_Variable)

// Without explicit name - uses ActivityDef name
Act($update_ActivityDef)
```

### ElemAct

**ElemAct** is an alias for Act with Complexity = Elementary:

```groovy
ElemAct('Name', $elemAct_Def)
```

### EA

**EA** is a short alias for ElemAct:

```groovy
EA('Name', $elemAct_Def)
```

### CompAct

**CompAct** is an alias for Act with Complexity = Composite:

```groovy
CompAct('Name', $compAct_Def)
```

### CA

**CA** is a short alias for CompAct:

```groovy
CA('Name', $compAct_Def)
```

## Inline Activity Definitions
Creates the ActivityDef Item as well when used within the workflow Layout

### ElemActDef

Define an Elementary ActivityDef inline:

```groovy
ElemActDef(name: 'ActivityName', version: 0) {
    Property((OUTCOME_INIT): 'Empty')
    Schema($schema_Variable)
    Script($script_Variable)
}
```

#### Common Properties

- `OUTCOME_INIT` — Initial outcome state (e.g., 'Empty')
- `AGENT_ROLE` — Agent role for execution (e.g., 'UserCode')
- `AGENT_NAME` — Specific agent name

### CompActDef

Define a Composite ActivityDef inline with its own Layout:

```groovy
CompActDef(name: 'CompositeName', version: 0) {
    Property(key: 'value')
    Schema($schema_Variable)
    StateMachine($stateMachine_Variable)
    Layout {
        // Nested workflow patterns
        Act($some_Activity)
    }
}
```

## Best Practices

### Use Composite ActivityDef for Subflows

**Use CompActDef to group and name complex subflows** when your workflow contains logical sections that:
- **Reusable** across multiple workflows
- **Named** for clarity and maintainability
- **Encapsulated** to hide complexity

#### Example: Grouping Related Operations

```groovy
Workflow('OrderProcessing_Workflow', 0, generate: true) {
    Layout {
        // Main workflow flow
        Act('ReceiveOrder', $receiveOrder_ActivityDef)

        // Encapsulate payment processing as a named subflow
        CompActDef(name: 'ProcessPayment', version: 0) {
            Layout {
                Act('ValidatePayment', $validatePayment_ActivityDef)
                Act('ChargeCustomer', $chargeCustomer_ActivityDef)
                Act('RecordTransaction', $recordTransaction_ActivityDef)
            }
        }
        
        Act('ShipOrder', $shipOrder_ActivityDef)
    }
}
```

**Benefits:**
- The subflow appears as a single vertex named `ProcessPayment` in the main workflow
- Internal complexity is hidden from the main workflow view
- Can be reused in other workflows by reference
- Easier to maintain and understand

#### Example: Reusing a Composite Activity

```groovy
// Define once
CompActDef(name: 'StandardApproval', version: 0) {
    Layout {
        Act('Validate', $validate_ActivityDef)
        XOrSplit(javascript: 'item.properties.approved') {
            Block(Alias: 'approved') { Act('Approve', $approve_ActivityDef) }
            Block(Alias: 'rejected') { Act('Reject', $reject_ActivityDef) }
        }
        Act('Notify', $notify_ActivityDef)
    }
}

// Reuse in multiple workflows
Workflow('PurchaseOrder_Workflow', 0, generate: true) {
    Layout {
        Act('CreateOrder', $createOrder_ActivityDef)
        CompAct('Approval', $standardApproval_CompositeActivityDef)
        Act('FulfillOrder', $fulfillOrder_ActivityDef)
    }
}

Workflow('ExpenseReport_Workflow', 0, generate: true) {
    Layout {
        Act('SubmitReport', $submitReport_ActivityDef)
        CompAct('Approval', $standardApproval_CompositeActivityDef)
        Act('Reimburse', $reimburse_ActivityDef)
    }
}
```

### Reusing Existing ActivityDefs

When referencing existing ActivityDefs, you have two options for the first parameter to `Act()`:

#### Option 1: Explicit Vertex Name (Recommended)

```groovy
// Bind to the automatically created variable
Act('CreateItem', $createItem_ActivityDef)
```

**Benefits:**
- The vertex in the workflow graph will have the name `CreateItem`
- Clear separation between the ActivityDef name and its role in the workflow
- Same ActivityDef can be used multiple times with different vertex names

#### Option 2: Implicit Name (Uses ActivityDef name)

```groovy
// Uses the ActivityDef's name as the vertex name
Act($createItem_ActivityDef)
```

**When to use:**
- When the ActivityDef name perfectly describes its role in the workflow
- When the Activity is used only once in the workflow

#### Example: Reusing Same Activity Multiple Times

```groovy
Workflow('DataProcessing_Workflow', 0, generate: true) {
    Layout {
        // Same ActivityDef used twice with different vertex names
        Act('InitialValidation', $validate_ActivityDef)
        Act('FinalValidation', $validate_ActivityDef)
        
        // Different purposes, same underlying Activity
        // Creates two distinct vertices: InitialValidation and FinalValidation
    }
}
```

### Naming Conventions for Activity Vertices

| Scenario | Convention | Example |
|----------|------------|---------|
| Single use, matches ActivityDef | Use ActivityDef name | `Act($create_ActivityDef)` |
| Multiple uses | Explicit vertex names | `Act('FirstCreate', $create_ActivityDef)` |
| Subflow reference | Descriptive subflow name | `CompAct('PaymentProcessing', $payment_CompActDef)` |
| Inline CompActDef | Subflow purpose | `CompActDef(name: 'Validation', version: 0)` |

## Properties on Activities

Set properties when referencing Activities:

```groovy
Act('MyAct', $actDef) {
    Property(AGENT_ROLE: 'UserCode')
    Property((OUTCOME_INIT): 'Empty')
    Property(stringVal: 'custom')
    Property(intVal: 42, booleanVal: true)
}
```

## Common Built-in Property Keys

Check `org.cristalise.kernel.graph.model.BuiltInVertexProperties` enum for properties that have specific usage with the system

## See Also

- [Workflow](workflow.md) — Where activities are used
- [Layout](layout.md) — Container for activity references
- [Block](block.md) — Sequential activity grouping
- [../EXAMPLES.md](../EXAMPLES.md) — Activity usage examples
