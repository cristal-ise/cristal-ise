# Layout

**Layout** is the container for workflow pattern primitives that defines the possible sequences of Activity execution. It is the root element within a Workflow definition.

## Basic Structure

```groovy
Workflow('MyWorkflow', 0, generate: true) {
    Layout {
        // Workflow pattern primitives go here
    }
}
```

## Requirements

- The `Layout` block is **required** when `generate: true`
- Contains all workflow pattern definitions
- Defines the execution sequence of Activities

## Sequential Execution

By default, elements in Layout execute sequentially:

```groovy
Layout {
    Act('First', $first_ActivityDef)
    Act('Second', $second_ActivityDef)
    Act('Third', $third_ActivityDef)
}
```

This executes: First → Second → Third

## With Parallel Patterns

```groovy
Layout {
    Act('Prepare', $prepare_Act)
    AndSplit {
        Block { Act('Parallel1', $act1) }
        Block { Act('Parallel2', $act2) }
    }
    Act('Finalize', $finalize_Act)
}
```

This executes: Prepare → (Parallel1 AND Parallel2) → Finalize

## With Conditional Patterns

```groovy
Layout {
    Act('Check', $check_Act)
    XOrSplit(javascript: 'condition') {
        Block(Alias: 'yes') { Act('YesPath', $yesAct) }
        Block(Alias: 'no')  { Act('NoPath', $noAct) }
    }
    Act('Complete', $complete_Act)
}
```

## Nesting Rules

- Layouts can contain any workflow pattern primitive
- Splits can contain Blocks
- Blocks can contain any workflow elements
- Deep nesting (>3 levels) is discouraged

## See Also

- [Workflow](workflow.md) — Root workflow definition
- [Block](block.md) — Sequential grouping container
- [Split Patterns](splits.md) — Pattern primitives
- [../EXAMPLES.md](../EXAMPLES.md) — Complete layout examples
