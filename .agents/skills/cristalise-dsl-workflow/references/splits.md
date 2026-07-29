# Split Patterns

**Splits** are workflow primitives for branching execution paths. CRISTAL-iSE supports four split types with corresponding joins.

## Split Types Overview

| Split Type | Execution | Branches | Join Type |
|------------|-----------|----------|-----------|
| AndSplit | Parallel | All | AndJoin |
| OrSplit | Conditional | 0 or more | Join |
| XOrSplit | Exclusive | Exactly 1 | Join |
| Loop | Repetitive | 1 (repeated) | LoopJoin |
| LoopInfinitive | Continuous | 1 (continuous) | LoopJoin |

## Common Properties

All splits share these characteristics:
- Create a split vertex
- Create a corresponding join vertex
- Use Blocks to define branches
- Can be nested within other Blocks

## AndSplit

**AndSplit** enables parallel execution of multiple branches. All branches execute concurrently, and the workflow waits for all to complete before continuing.

```groovy
AndSplit {
    Block { Act('Branch1', $activity1) }
    Block { Act('Branch2', $activity2) }
    Block { Act('Branch3', $activity3) }
}
```

### Key Properties

- Creates an **AndSplit** vertex and corresponding **AndJoin**
- All outgoing edges are traversed simultaneously
- Join waits for all incoming branches
- No routing script required (all branches always execute)

### Completion Criterion

All parallel branches must complete before the join point.

## OrSplit

**OrSplit** enables conditional execution based on data evaluation. Can execute zero, one, or multiple branches based on routing conditions.

```groovy
OrSplit(groovy: 'condition') {
    Block(Alias: 'trueBranch')  { Act('TrueAction', $trueAct) }
    Block(Alias: 'falseBranch') { Act('FalseAction', $falseAct) }
}
```

### Routing Configuration

- `groovy: 'expression'` — inline Groovy expression
- `javascript: 'expression'` — inline JavaScript expression
- `RoutingScriptName: 'ScriptName', RoutingScriptVersion: 0` — reference to external Script Item
- Default: `javascript:"true";` (all branches execute)

### Alias Requirement

**Each Block in an OrSplit MUST have an Alias property** for edge naming.

### Completion Criterion

Branches execute based on routing script evaluation; join fires when all active branches complete.

## XOrSplit

**XOrSplit** enables exclusive conditional execution where only ONE branch is taken based on routing evaluation.

```groovy
XOrSplit(javascript: 'item.properties.get("status") == "approved"') {
    Block(Alias: 'approved') { Act('ProcessApproval', $approveAct) }
    Block(Alias: 'rejected') { Act('ProcessRejection', $rejectAct) }
}
```

### Completion Criterion

Exactly one branch executes based on routing condition; join fires after that single branch completes.

## Loop

**Loop** enables repetitive execution of Activities with a predefined end condition.

```groovy
Loop {
    Act('Iterate', $iteration_Activity)
}
```

### Generated Vertices

Loop creates:
- `LoopSplit` — the split vertex
- `LoopJoin_first` — first join (true condition)
- `LoopJoin_last` — last join (false condition)

### Completion Criterion

Loop continues until exit condition is met, then proceeds to join.

## LoopInfinitive

**LoopInfinitive** enables continuous repetitive execution without a predefined end condition.

```groovy
LoopInfinitive {
    Act('Continuous', $continuous_Activity)
}
```

### Completion Criterion

Loop continues indefinitely or until externally interrupted.

## Split Configuration

### Shortcut Properties

```groovy
OrSplit(groovy: '...')    // Sets RoutingScriptName to "groovy:..."
OrSplit(javascript: '...') // Sets RoutingScriptName to "javascript:..."
```

### Using Script Objects

```groovy
OrSplit(RoutingScript: $myScript) { ... }
```

### External Script References

```groovy
OrSplit(RoutingScriptName: 'MyRoutingScript', RoutingScriptVersion: 0) {
    Property(counter: 'activity//./first:/TestData/counter')
    Block(Alias: 'left')  { Act(left)  }
    Block(Alias: 'right') { Act(right) }
}
```

## Nesting Splits

Splits can be nested within Blocks:

```groovy
AndSplit {
    Block {
        OrSplit {
            Block(Alias: 'a1') { Act('A1', $a1) }
            Block(Alias: 'a2') { Act('A2', $a2) }
        }
    }
    Block {
        XOrSplit {
            Block(Alias: 'b1') { Act('B1', $b1) }
            Block(Alias: 'b2') { Act('B2', $b2) }
        }
    }
}
```

## See Also

- [Workflow](workflow.md) — Root workflow definition
- [Layout](layout.md) — Container for splits
- [Block](block.md) — Branch container
- [Routing Scripts](routing.md) — Split routing configuration
- [../EXAMPLES.md](../EXAMPLES.md) — Split usage examples
