# Routing Scripts

**Routing Scripts** determine which branches of a Split should execute. They are essential for conditional splits (OrSplit, XOrSplit).

## Routing Configuration Options

### Inline Groovy

```groovy
OrSplit(groovy: 'item.properties.get("value") > 10') {
    Block(Alias: 'high')  { Act('HighAction', $highAct) }
    Block(Alias: 'low')   { Act('LowAction', $lowAct) }
}
```

### Inline JavaScript

```groovy
XOrSplit(javascript: 'data.status == "active"') {
    Block(Alias: 'active')   { Act('ActiveAction', $activeAct) }
    Block(Alias: 'inactive') { Act('InactiveAction', $inactiveAct) }
}
```

### External Script Reference

```groovy
OrSplit(RoutingScriptName: 'MyRoutingScript', RoutingScriptVersion: 0) {
    Block(Alias: 'branch1') { Act('Action1', $act1) }
    Block(Alias: 'branch2') { Act('Action2', $act2) }
}
```

### Using Script Object

```groovy
def script = new Script('RoutingScript42', 13, new ItemPath(), null)

OrSplit(RoutingScript: script) {
    Block(Alias: 'branch1') { Act('Action1', $act1) }
    Block(Alias: 'branch2') { Act('Action2', $act2) }
}
```

## Default Behavior

If no routing script is specified:
- **OrSplit**: Defaults to `javascript:"true";` — **all branches execute**
- **XOrSplit**: Requires a routing script (will error if missing)
- **AndSplit**: No routing script needed (all branches always execute)
- **Loop/LoopInfinitive**: Use built-in loop logic

## Routing Script Context

Routing scripts have access to:
- `item` — The current Item
- `activity` — The current Activity context
- Any variables in scope

### Common Patterns

#### Property Check

```groovy
javascript: 'item.properties.get("priority") == "high"'
groovy: 'item.properties.priority == "high"'
```

#### Numeric Comparison

```groovy
javascript: 'item.properties.get("count") > 5'
groovy: 'item.properties.count > 5'
```

#### Multiple Conditions

```groovy
javascript: 'item.properties.status == "ready" && item.properties.priority == "high"'
groovy: 'item.properties.status == "ready" && item.properties.priority == "high"'
```

#### XPath Evaluation

```groovy
javascript: 'xpath.evaluate("//data/value > 10", item.xml)'
```

## Custom Properties

Add custom properties to splits:

```groovy
OrSplit {
    Property((ROUTING_SCRIPT_NAME): 'CustomScript')
    Property((ROUTING_SCRIPT_VERSION): 1)
    Property(counter: 'activity//./first:/TestData/counter')
    Block(Alias: 'b1') { Act('Action1', $act1) }
    Block(Alias: 'b2') { Act('Action2', $act2) }
}
```

## Important Notes

1. **Alias is Required**: Each Block in OrSplit/XOrSplit must have an Alias
2. **Script Evaluation**: Routing scripts are evaluated at runtime for each execution
3. **Performance**: Inline scripts are faster than external script references
4. **Error Handling**: Invalid routing scripts will cause workflow errors

## See Also

- [Split Patterns](splits.md) — Where routing scripts are used
- [../EXAMPLES.md](../EXAMPLES.md) — Routing script examples
- [Workflow](workflow.md) — Root workflow definition
