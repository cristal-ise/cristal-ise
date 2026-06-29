# Properties

**Properties** are key-value pairs that configure and customize workflow elements.

## Setting Properties

### On Activities

```groovy
Act('MyAct', $actDef) {
    Property(AGENT_ROLE: 'UserCode')
    Property((OUTCOME_INIT): 'Empty')
    Property(stringVal: 'custom')
    Property(intVal: 42, booleanVal: true)
}
```

### On Blocks

```groovy
Block(Alias: 'branch1') {
    Property(AGENT_ROLE: 'UserCode')
    Act('Action', $act)
}
```

### On Splits

```groovy
OrSplit {
    Property((ROUTING_SCRIPT_NAME): 'CustomScript')
    Property((ROUTING_SCRIPT_VERSION): 1)
    Property(counter: 'activity//./first:/TestData/counter')
    Block(Alias: 'b1') { Act('Action1', $act1) }
    Block(Alias: 'b2') { Act('Action2', $act2) }
}
```

### On Inline Activity Definitions

```groovy
ElemActDef(name: 'MyEA', version: 0) {
    Property((OUTCOME_INIT): 'Empty')
    Property((AGENT_ROLE): 'UserCode')
    Schema($my_Schema)
}

CompActDef(name: 'MyCA', version: 0) {
    Property(concreteProp: 'dummy')
    Property(abstractProp: 'dummy')
    Layout { /* ... */ }
}
```

## Property Types

### Built-in Properties

These are standard CRISTAL-iSE property keys:

| Key | Type | Description | Applies To |
|-----|------|-------------|------------|
| `AGENT_ROLE` | String | Agent role for execution | Activities, Blocks |
| `AGENT_NAME` | String | Specific agent name | Activities, Blocks |
| `OUTCOME_INIT` | String | Initial outcome state | Activities |
| `ROUTING_SCRIPT_NAME` | String | Routing script name | Splits |
| `ROUTING_SCRIPT_VERSION` | Integer | Routing script version | Splits |
| `ROUTING_EXPR` | String | Routing expression | Splits |
| `DEPENDENCY_NAME` | String | Dependency name | Activities |
| `ACTIVITY_DEF_NAME` | String | Activity definition name | Activities |
| `PREDEFINED_STEP` | String | Predefined step type | Activities |
| `DEPENDENCY_CARDINALITY` | String | Dependency cardinality | Dependencies |
| `DEPENDENCY_TYPE` | String | Dependency type | Dependencies |
| `DEPENDENCY_TO` | String | Dependency target | Dependencies |

### Custom Properties

Add any custom key-value pairs:

```groovy
Act('MyAct', $actDef) {
    Property(customKey: 'customValue')
    Property(numberOfRetries: 3)
    Property(isCritical: true)
}
```

### Abstract vs Concrete Properties

In CompositeActivityDef:

```groovy
CompActDef(name: 'MyCA', version: 0) {
    // Concrete property (instance-specific)
    Property(concreteProp: 'dummy')
    
    // Abstract property (must be set by caller)
    AbstractProperty(abstractProp: 'dummy')
}
```

## Property Value Types

Properties support various value types:

```groovy
// String
Property(stringVal: 'text')

// Integer
Property(intVal: 42)

// Boolean
Property(booleanVal: true)

// Map
Property(mapVal: [key1: 'value1', key2: 'value2'])

// List
Property(listVal: ['a', 'b', 'c'])
```

## Property Access

Properties can be accessed in routing scripts and workflow logic:

```groovy
// In routing script
javascript: 'item.properties.get("myProp") == "value"'

// In Groovy script
groovy: 'item.properties.myProp == "value"'
```

## See Also

- [Activity](activity.md) — Where properties are commonly set
- [Split Patterns](splits.md) — Routing properties
- [../EXAMPLES.md](../EXAMPLES.md) — Property usage examples
