# Property Reference

This file documents the `Property` and `InmutableProperty` DSL constructs for defining Item metadata.

## Overview

Properties are key-value pairs used as metadata by the kernel for identification, typing, and configuration of Items. There are two types:

- **InmutableProperty**: Immutable key-value metadata that cannot be changed after Item creation
- **Property**: Mutable key-value metadata that can be modified during the Item's lifecycle using the **PrdefinedStep** called `WriteProperty`.

## Syntax

### Property & InmutableProperty

```groovy
Property('key': 'value')
InmutableProperty('key': 'value')
```

The value can be any type supported by groovy:
```groovy
Property('Status': 'Active')
Property('Count': 42)
Property('Enabled': true)
ImmutableProperty('Root': '/module/MyModule')
```

## When to Use Each

In case of uncertainty, ask the user about the purpose of the property.

### Use **InmutableProperty** for:
- Type-defining metadata (`Type`, `Module`, `Root`)
- Configuration that should never change
- Identification and Namespace properties
- Static User-defined metadata

### Use **Property** for:
- Mutable configuration that can change during lifecycle, like Name
- Status information, e.g. State, Version
- Runtime configuration
- Dynamic User-defined metadata

### BuiltInItemProperties
Use values of enum `org.cristalise.kernel.property.BuiltInItemProperties` when defining Properties. The Javadoc contains all necessary information.

### Mandatory Properties
- **Name**: It is automatically added by the system using the **name** attribute of the **Item**
- **Type**
- **Module**
- **Version**

### Optional Properties
- **State**

## Common Property Patterns

### Type Classification
```groovy
InmutableProperty('Type': 'ImportItem')
InmutableProperty('Type': 'Factory')
InmutableProperty('Type': 'Customer')
InmutableProperty('Type': 'PurchaseOrder')
```

### Root Path
```groovy
InmutableProperty('Root': '/module/CRM')
InmutableProperty('Root': '/desc/ActivityDesc/kernel')
```

### Schema References
```groovy
InmutableProperty('UpdateSchema': 'Customer:0')
InmutableProperty('UpdateSchema': SCRIPT_RESOURCE.schemaName + ':0')
```

## See Also

- [Item Reference](item.md) — Item constructor and attributes
- [kernel/CONTEXT.md](../../../../kernel/CONTEXT.md) — Core CRISTAL-iSE domain definitions
