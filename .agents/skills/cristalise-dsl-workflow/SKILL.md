---
name: cristalise-dsl-workflow
description: Complete reference for defining CompositeActivityDef Items, also known aa Workflow Items in CRISTAL-iSE DSL. Use when creating or modifying workflow definitions, or when another skill needs workflow pattern details.
---

# CRISTAL-iSE Workflow DSL

**Workflow** is the core construct for defining Item lifecycles as dependency graphs of Activities. This skill is the complete reference for defining workflows using the CRISTAL-iSE Groovy DSL.

## Quick Start

### Minimal Workflow

```groovy
Workflow('MyWorkflow', 0, generate: true) {
    Layout {
        Act('DoSomething', $my_ActivityDef)
    }
}
```

### With Patterns

```groovy
Workflow('ParallelWorkflow', 0, generate: true) {
    Layout {
        AndSplit {
            Block { Act('Task1', $task1_ActivityDef) }
            Block { Act('Task2', $task2_ActivityDef) }
        }
        XOrSplit(javascript: 'item.properties.status == "ready"') {
            Block(Alias: 'yes') { Act('Process', $process_ActivityDef) }
            Block(Alias: 'no')  { Act('Wait', $wait_ActivityDef) }
        }
    }
}
```

## Core Concepts

| Concept | File | Description |
|---------|------|-------------|
| **Workflow** | [references/workflow.md](references/workflow.md) | Root workflow definition |
| **Layout** | [references/layout.md](references/layout.md) | Container for pattern primitives |
| **Activity** | [references/activity.md](references/activity.md) | Units of work (EA/CA) |
| **Binding** | [references/binding.md](references/binding.md) | Auto-bound variables ($prefix) |
| **Block** | [references/block.md](references/block.md) | Sequential grouping container |
| **Splits** | [references/splits.md](references/splits.md) | Branching patterns (AndSplit, OrSplit, XOrSplit, Loop) |
| **Routing** | [references/routing.md](references/routing.md) | Split routing scripts |
| **Properties** | [references/properties.md](references/properties.md) | Configuration key-value pairs |
| **Examples** | [EXAMPLES.md](EXAMPLES.md) | Complete runnable examples |
| **Glossary** | [GLOSSARY.md](GLOSSARY.md) | Defined terms |

## Reference Files

### Concept References

- **[references/workflow.md](references/workflow.md)** — Workflow constructor, parameters, verification, Item integration
- **[references/layout.md](references/layout.md)** — Layout structure, sequential/parallel/conditional patterns
- **[references/activity.md](references/activity.md)** — Activity types, references (Act/ElemAct/CompAct/EA/CA), inline definitions (ElemActDef/CompActDef)
- **[references/binding.md](references/binding.md)** — Variable binding convention ($prefix for auto-bound variables)
- **[references/block.md](references/block.md)** — Block usage, aliases, nesting, requirements in splits
- **[references/splits.md](references/splits.md)** — Complete split reference: AndSplit, OrSplit, XOrSplit, Loop, LoopInfinitive
- **[references/routing.md](references/routing.md)** — Routing script configuration (Groovy/JavaScript/external)
- **[references/properties.md](references/properties.md)** — Property types, built-in keys, custom properties, abstract vs concrete

### Supporting Files

- **[EXAMPLES.md](EXAMPLES.md)** — 20+ complete, runnable examples covering all patterns
- **[GLOSSARY.md](GLOSSARY.md)** — Alphabetical glossary of 40+ defined terms

## Workflow Pattern Summary

| Pattern | Execution | Branches | Routing |
|---------|-----------|----------|--------|
| AndSplit | Parallel | All | N/A | 
| OrSplit | Conditional | 0 or more | Required |
| XOrSplit | Exclusive | Exactly 1 | Required |
| Loop | Repetitive | 1 (repeated) | Optional |
| LoopInfinitive | Continuous | 1 (continuous) | N/A |

## Key Rules

1. **Alias Required**: Blocks in OrSplit/XOrSplit MUST have Alias property
2. **Unique Names**: All vertex names must be unique
3. **Verification**: Always call `wf.verify()` or `caDef.verify()`
4. **No Direct Splits**: Splits can only contain Blocks, not direct Activity references
5. **Generate Flag**: Layout block required when `generate: true`

## Best Practices

### Naming
- Workflows: `PascalCase_Workflow`
- Activities: `PascalCase_Activity` or `PascalCase_ActivityDef`
- Splits: Auto-named (AndSplit, AndSplit1, etc.)
- Joins: Auto-generated from split names
- Block Aliases: Descriptive names for conditional branches

### Structure
1. Start with sequential Act statements
2. Add AndSplit for independent parallel operations
3. Add XOrSplit for exclusive branching
4. Add OrSplit for conditional (0+) branching
5. Add Loop for repetition
6. Use CompAct to group and name complex subflows
7. Keep nesting shallow (<3 levels)

### Testing
Use WorkflowTestBuilder:
```groovy
def wfBuilder = new WorkflowTestBuilder()
def wf = wfBuilder.build { Layout { /* DSL */ } }
wf.verify()
wfBuilder.checkActPath('name', 'path')
```

## Common Patterns

| Pattern | Use Case | Split Type |
|---------|----------|------------|
| Fan-Out/Fan-In | Parallel processing | AndSplit |
| Conditional Routing | Branch based on data | XOrSplit |
| Optional Processing | Skip if not needed | OrSplit |
| Retry Logic | Repeat with exit condition | Loop |
| Continuous Processing | Infinite loop | LoopInfinitive |

## Anti-Patterns

**AVOID:**
- Unnamed Activities in splits without Alias
- Missing Alias in OrSplit/XOrSplit blocks
- Deeply nested splits (>3 levels)
- Circular dependencies in workflow graph
- Workflows without verification
- Hardcoded script logic in workflow definitions
- Splits containing direct Activity references (use Blocks)

## Error Handling

| Error | Cause | Fix |
|-------|-------|-----|
| `Vertex name must be unique` | Duplicate vertex names | Use unique names |
| `Alias is required` | Block in OrSplit/XOrSplit without Alias | Add Alias property |
| `Workflow verification failed` | Invalid structure | Check all splits have joins, all paths connected |
| `RoutingScriptName is required` | Split without routing | Add routing script or use default |
| `UnsupportedOperationException` | Split contains non-Block | Wrap Activities in Blocks |

## Information Hierarchy

```
SKILL.md (this file)
├── Core concepts table
├── Quick start examples
├── Pattern summary
├── Key rules
├── Best practices
└── Pointers to reference files
    
references/
├── workflow.md    - Workflow definition
├── layout.md      - Layout container
├── activity.md    - Activity references
├── binding.md     - Variable binding convention
├── block.md       - Block container
├── splits.md      - All split patterns
├── routing.md     - Routing scripts
└── properties.md  - Properties

Root/
├── EXAMPLES.md    - 20+ complete examples
└── GLOSSARY.md    - 40+ defined terms
```

## Leading Words

- **Workflow** — The central concept for Item lifecycle management
- **Layout** — The container for execution sequences
- **Activity** — Units of work in workflows
- **Binding** — Automatic variable binding in DSL scripts
- **Split** — Branching primitives (AndSplit, OrSplit, XOrSplit, Loop)
- **Join** — Synchronization points
- **Block** — Sequential grouping within splits
- **Alias** — Edge naming for conditional splits
- **Routing** — Branch selection logic

## When to Use This Skill

- Creating new workflow definitions
- Modifying existing workflows
- Debugging workflow errors
- Understanding workflow pattern behavior
- Designing Item lifecycles
- Testing workflow structures

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Items and Agents that has Lifecycle 
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
