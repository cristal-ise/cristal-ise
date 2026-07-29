# Block

**Block** is a grouping container for workflow elements that executes its contents sequentially.

## Basic Usage

```groovy
Block {
    Act('First', $act1)
    Act('Second', $act2)
}
```

## Short Alias

**B** is a short alias for Block:

```groovy
B { Act('Short', $act) }
```

## In Splits

Blocks are the primary container within Split patterns:

```groovy
AndSplit {
    Block { Act('Branch1', $act1) }
    Block { Act('Branch2', $act2) }
}

OrSplit {
    Block(Alias: 'true')  { Act('TrueAction', $trueAct) }
    Block(Alias: 'false') { Act('FalseAction', $falseAct) }
}
```

## Alias Requirement

**Alias is REQUIRED** for Blocks in OrSplit and XOrSplit:

```groovy
// CORRECT
OrSplit {
    Block(Alias: 'branch1') { Act('Action1', $act1) }
    Block(Alias: 'branch2') { Act('Action2', $act2) }
}

// INCORRECT - will fail
OrSplit {
    Block { Act('Action1', $act1) }  // Missing Alias
    Block { Act('Action2', $act2) }  // Missing Alias
}
```

## With Properties

Blocks can have properties:

```groovy
Block(Alias: 'branch1') {
    Property(AGENT_ROLE: 'UserCode')
    Act('Action', $act)
}
```

## Nesting

Blocks can contain any workflow elements:

```groovy
Block {
    Act('First', $act1)
    AndSplit {
        Block { Act('Parallel1', $p1) }
        Block { Act('Parallel2', $p2) }
    }
    Act('Last', $act2)
}
```

## Completion Criterion

All Activities in the Block execute in order, one after another. The Block completes when all its contained elements complete.

## See Also

- [Layout](layout.md) — Parent container
- [Split Patterns](splits.md) — Where Blocks are primarily used
- [../EXAMPLES.md](../EXAMPLES.md) — Block usage examples
