# CRISTAL-iSE Workflow DSL Examples

This file contains complete, runnable examples for all workflow patterns in CRISTAL-iSE DSL.

## Table of Examples

1. [Simple Sequential Workflow](#1-simple-sequential-workflow)
2. [Parallel Processing Workflow](#2-parallel-processing-workflow)
3. [Conditional Branching Workflow](#3-conditional-branching-workflow)
4. [Exclusive Conditional Workflow](#4-exclusive-conditional-workflow)
5. [Nested Workflow](#5-nested-workflow)
6. [Complex Workflow with All Patterns](#6-complex-workflow-with-all-patterns)
7. [Workflow in Item Definition](#7-workflow-in-item-definition)
8. [Fan-Out/Fan-In Pattern](#8-fan-outfan-in-pattern)
9. [Optional Processing Pattern](#9-optional-processing-pattern)
10. [Retry Loop Pattern](#10-retry-loop-pattern)
11. [Sequential with Parallel Sections](#11-sequential-with-parallel-sections)
12. [Nested Splits](#12-nested-splits)
13. [Testing Workflows](#13-testing-workflows)
14. [Inline Activity Definitions](#14-inline-activity-definitions)
15. [Composite ActivityDef for Subflows](#15-composite-activitydef-for-subflows)
16. [Reusing ActivityDef with Explicit Vertex Names](#16-reusing-activitydef-with-explicit-vertex-names)
17. [Reusing Existing Composite ActivityDef](#17-reusing-existing-composite-activitydef)

---

## 1. Simple Sequential Workflow

Basic workflow with sequential Activity execution.

```groovy
Workflow('Sequential_Workflow', 0, generate: true) {
    Layout {
        Act('Create', $create_ActivityDef)
        Act('Validate', $validate_ActivityDef)
        Act('Publish', $publish_ActivityDef)
    }
}
```

**Execution flow**: Create → Validate → Publish

---

## 2. Parallel Processing Workflow

Workflow with parallel branches using AndSplit.

```groovy
Workflow('Parallel_Workflow', 0, generate: true) {
    Layout {
        AndSplit {
            Block { Act('ProcessData', $data_Activity) }
            Block { Act('NotifyUsers', $notify_Activity) }
            Block { Act('LogEvent', $log_Activity) }
        }
    }
}
```

**Execution flow**: All three Activities execute concurrently, workflow waits for all to complete.

---

## 3. Conditional Branching Workflow

Workflow with conditional branches using XOrSplit (exclusive).

```groovy
Workflow('Conditional_Workflow', 0, generate: true) {
    Layout {
        XOrSplit(javascript: 'item.properties.get("priority") == "high"') {
            Block(Alias: 'highPriority') {
                Act('Expedite', $expedite_Activity)
            }
            Block(Alias: 'normalPriority') {
                Act('Queue', $queue_Activity)
            }
        }
    }
}
```

**Execution flow**: Either Expedite OR Queue executes based on priority check, not both.

---

## 4. Exclusive Conditional Workflow

Workflow with XOrSplit based on approval status.

```groovy
Workflow('Approval_Workflow', 0, generate: true) {
    Layout {
        XOrSplit(javascript: 'item.properties.get("status") == "approved"') {
            Block(Alias: 'approved') { Act('ProcessApproval', $approveAct) }
            Block(Alias: 'rejected') { Act('ProcessRejection', $rejectAct) }
        }
    }
}
```

---

## 5. Nested Workflow

Workflow with nested split patterns.

```groovy
Workflow('Nested_Workflow', 0, generate: true) {
    Layout {
        Act('Init', $init_Activity)
        AndSplit {
            Block {
                Act('Branch1', $act1)
            }
            Block {
                OrSplit(groovy: 'condition') {
                    Block(Alias: 'yes') { Act('YesPath', $yesAct) }
                    Block(Alias: 'no')  { Act('NoPath', $noAct) }
                }
            }
        }
        Act('Finalize', $final_Activity)
    }
}
```

**Execution flow**: 
1. Init
2. Parallel: Branch1 AND (YesPath OR NoPath based on condition)
3. Finalize (waits for both parallel branches)

---

## 6. Complex Workflow with All Patterns

Comprehensive example using multiple pattern types.

```groovy
Workflow('Complex_Workflow', 0, generate: true) {
    Layout {
        // Initial sequence
        Act('Start', $start_Activity)
        
        // Parallel branches
        AndSplit {
            // First parallel branch
            Block {
                Act('Parallel1', $p1_Activity)
            }
            // Second parallel branch with conditional
            Block {
                OrSplit(javascript: 'checkCondition()') {
                    Block(Alias: 'true')  { Act('TrueAction', $trueAct) }
                    Block(Alias: 'false') { Act('FalseAction', $falseAct) }
                }
            }
        }
        
        // Conditional branching
        XOrSplit(groovy: 'item.properties.status == "ready"') {
            Block(Alias: 'ready')   { Act('Process', $process_Activity) }
            Block(Alias: 'waiting') { Act('Wait', $wait_Activity) }
        }
        
        // Loop
        Loop {
            Act('Iterate', $iterate_Activity)
        }
        
        // Final action
        Act('Complete', $complete_Activity)
    }
}
```

---

## 7. Workflow in Item Definition

Complete Item definition with workflow reference.

```groovy
Item(name: 'PatientFactory', version: 0, folder: '/integTest', 
      workflow: 'Patient_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/integTest/Patients')
    InmutableProperty('UpdateSchema': 'Patient_Details:0')

    Dependency('workflow') {
        Member(itemPath: '/desc/Workflow/integTest/Patient_Workflow') {
            Property('Version': 0)
        }
    }
}
```

---

## 8. Fan-Out/Fan-In Pattern

Parallel processing with automatic join.

```groovy
Workflow('FanOutFanIn_Workflow', 0, generate: true) {
    Layout {
        AndSplit {
            Block { Act('Task1', $task1) }
            Block { Act('Task2', $task2) }
            Block { Act('Task3', $task3) }
        }
        // Implicit AndJoin waits for all
        Act('AggregateResults', $aggregate_Activity)
    }
}
```

**Execution flow**: Task1, Task2, Task3 execute in parallel → AndJoin waits → AggregateResults

---

## 9. Optional Processing Pattern

Using OrSplit for optional branches.

```groovy
Workflow('Optional_Workflow', 0, generate: true) {
    Layout {
        OrSplit(groovy: 'needsSpecialProcessing') {
            Block(Alias: 'special') { Act('SpecialProcess', $specialAct) }
            Block(Alias: 'normal')  { Act('NormalProcess', $normalAct) }
        }
    }
}
```

**Execution flow**: 
- If `needsSpecialProcessing` is true: Both branches may execute (depends on script)
- If false: Only normal branch executes

---

## 10. Retry Loop Pattern

Loop with retry logic.

```groovy
Workflow('Retry_Workflow', 0, generate: true) {
    Layout {
        Loop {
            Act('Attempt', $attempt_Activity) {
                Property((RETRY_COUNT): 3)
            }
        }
    }
}
```

---

## 11. Sequential with Parallel Sections

Mixed sequential and parallel execution.

```groovy
Workflow('Mixed_Workflow', 0, generate: true) {
    Layout {
        Act('Prepare', $prepare_Act)
        AndSplit {
            Block { Act('ParallelA', $actA) }
            Block { Act('ParallelB', $actB) }
        }
        Act('Finalize', $finalize_Act)
    }
}
```

**Execution flow**: Prepare → (ParallelA AND ParallelB) → Finalize

---

## 12. Nested Splits

Multiple levels of split nesting.

```groovy
Workflow('NestedSplits_Workflow', 0, generate: true) {
    Layout {
        AndSplit {
            Block {
                AndSplit {
                    Block {Act('Nested1', $n1)}
                    Block {Act('Nested2', $n2)}
                }
            }
            Block {
                OrSplit(groovy: 'condition') {
                    Block(Alias: 'yes') { Act('Yes', $yesAct) }
                    Block(Alias: 'no')  { Act('No', $noAct) }
                }
            }
        }
    }
}
```

---

## 13. Testing Workflows

Complete test example using WorkflowTestBuilder.

```groovy
import org.cristalise.dsl.test.builders.WorkflowTestBuilder

def wfBuilder = new WorkflowTestBuilder()

def wf = wfBuilder.build {
    Layout {
        AndSplit {
            Block { ElemAct("left") }
            Block { ElemAct("right") }
        }
    }
}

// Verify workflow structure
wf.verify()
wfBuilder.checkActPath('left', 'workflow/domain/left')
wfBuilder.checkActPath('right', 'workflow/domain/right')
wfBuilder.checkSplit('AndSplit', ['left', 'right'])
wfBuilder.checkJoin('AndJoin', ['left', 'right'])
```

---

## 14. Inline Activity Definitions

Workflow with inline Activity definitions.

```groovy
Workflow('Inline_Workflow', 0, generate: true) {
    Layout {
        ElemActDef('InlineEA', 0) {
            Property((OUTCOME_INIT): 'Empty')
            Property((AGENT_ROLE): 'UserCode')
        }
        CompActDef('InlineCA', 0) {
            Property(key: 'value')
            Layout {
                Act('Nested', $some_Activity)
            }
        }
    }
}
```

---

## 15. Composite ActivityDef for Subflows

Using CompositeActivityDef to group and name complex subflows.

```groovy
Workflow('OrderProcessing_Workflow', 0, generate: true) {
    Layout {
        Act('ReceiveOrder', $receiveOrder_ActivityDef)
        
        // Inline Composite ActivityDef for payment processing subflow
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

**Key Points:**
- `ProcessPayment` appears as a single vertex in the main workflow
- Internal complexity (3 Activities) is hidden within the Composite Activity
- The `name` parameter defines the vertex name in the parent workflow

---

## 16. Reusing ActivityDef with Explicit Vertex Names

Using the same ActivityDef multiple times with different vertex names.

```groovy
Workflow('DataProcessing_Workflow', 0, generate: true) {
    Layout {
        // First validation step
        Act('InitialValidation', $validate_ActivityDef) {
            Property(AGENT_ROLE: 'System')
        }
        
        AndSplit {
            Block { Act('ProcessData', $process_ActivityDef) }
            Block { 
                // Second validation step using same ActivityDef
                Act('FinalValidation', $validate_ActivityDef) {
                    Property(AGENT_ROLE: 'Manager')
                }
            }
        }
        
        // Creates two distinct vertices: InitialValidation and FinalValidation
        // Both use the same $validate_ActivityDef but with different roles
    }
}
```

**Key Points:**
- Same ActivityDef (`$validate_ActivityDef`) used twice
- Different vertex names: `InitialValidation` and `FinalValidation`
- Each can have different properties (e.g., different AGENT_ROLE)

---

## 17. Reusing Existing Composite ActivityDef

Referencing an existing Composite ActivityDef by its bound variable.

```groovy
// In one module file (e.g., Common.groovy)
CompActDef(name: 'StandardApproval', version: 0) {
    Layout {
        Act('Validate', $validate_ActivityDef)
        XOrSplit(javascript: 'item.properties.status == "approved"') {
            Block(Alias: 'yes') { Act('Approve', $approve_ActivityDef) }
            Block(Alias: 'no')  { Act('Reject', $reject_ActivityDef) }
        }
        Act('Notify', $notify_ActivityDef)
    }
}

// In another module file
Workflow('PurchaseOrder_Workflow', 0, generate: true) {
    Layout {
        Act('CreateOrder', $createOrder_ActivityDef)
        // Reference the existing Composite ActivityDef
        CompAct('GetApproval', $standardApproval_CompositeActivityDef)
        Act('FulfillOrder', $fulfillOrder_ActivityDef)
    }
}

Workflow('ExpenseReport_Workflow', 0, generate: true) {
    Layout {
        Act('SubmitReport', $submitReport_ActivityDef)
        // Same Composite ActivityDef reused with a different vertex name
        CompAct('ManagerApproval', $standardApproval_CompositeActivityDef)
        Act('Reimburse', $reimburse_ActivityDef)
    }
}
```

**Key Points:**
- `StandardApproval` Composite ActivityDef is defined once
- Reused in multiple workflows with different vertex names
- Vertex names (`GetApproval`, `ManagerApproval`) describe the role in each workflow
- Internal logic remains consistent across all uses

---

## Template: Minimal Workflow

```groovy
Workflow('WorkflowName', 0, generate: true) {
    Layout {
        Act('ActivityName', $activity_Def)
    }
}
```

---

## Template: Workflow with Properties

```groovy
Workflow('WorkflowName', 0, generate: true) {
    Layout {
        Act('ActivityName', $activity_Def) {
            Property(AGENT_ROLE: 'UserCode')
            Property((OUTCOME_INIT): 'Empty')
        }
    }
}
```

---

## See Also

- [Workflow](../references/workflow.md) — Workflow definition
- [Split Patterns](../references/splits.md) — All split types
- [Activity](../references/activity.md) — Activity references and reuse patterns
- [Block](../references/block.md) — Block usage
- [Routing Scripts](../references/routing.md) — Routing configuration
