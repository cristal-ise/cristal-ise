# Dependency Reference

This file documents the `Dependency` and `DependencyDescription` DSL constructs for defining collection relationships between Items, with reference to `BuiltInCollections`.

## Overview

**Dependency** and **DependencyDescription** are DSL constructs for defining collections — abstract relationship containers that reference other Items. These collections can restrict membership by type and define how Items relate to each other.

- **Dependency**: A concrete Collection implementation containing a variable number of members (like an array or list). Dependencies never contain empty slots or duplicated members.
- **DependencyDescription**: A template collection that can be instantiated to create actual Dependency collections. Used in Description Items to define collection structure and member constraints.

See `org.cristalise.kernel.collection.BuiltInCollections` for the complete list of built-in collection types.

## BuiltInCollections Reference
Use values of enum `org.cristalise.kernel.collection.BuiltInCollections` when defining Dependencies. The Javadoc contains all necessary information.

## Dependency Syntax

Use `Dependency` to create a concrete collection with variable members:

```groovy
Dependency(COLLECTION_TYPE) {
    Member(itemPath: '/path/to/item') {
        Property('key': 'value')
    }
    Member(itemPath: '/path/to/another/item') {
        Property('key': 'value')
    }
}
```

### Example: Workflow Dependency

```groovy
Dependency(WORKFLOW) {
    Member(itemPath: '/desc/ActivityDesc/kernel/ManageCustomer') {
        Property('Version': 0)
    }
}
```

### Example: Schema Dependency

```groovy
Dependency(SCHEMA) {
    Member(itemPath: '/desc/Schema/kernel/Customer') {
        Property('Version': 0)
    }
}
```

## DependencyDescription Syntax

Use `DependencyDescription` to create a template for collections:

```groovy
DependencyDescription(COLLECTION_TYPE) {
    Member(itemPath: '/path/to/item') {
        Property('key': 'value')
    }
}
```

### Example: Schema Description

```groovy
DependencyDescription(SCHEMA) {
    Member($customer_PropertyDescriptionList)
}
```

### Example: Include Description

```groovy
DependencyDescription(INCLUDE) {
    Member(itemPath: '/desc/Script/kernel/Utils') {
        Property('Version': 0)
    }
}
```

## Member Syntax

The `Member` construct defines an individual member within a Dependency or DependencyDescription:

```groovy
Member(itemPath: '/path/to/item') {
    Property('key1': 'value1')
    Property('key2': 'value2')
}
```

**itemPath** (String, *required*): The domain path to the Item being referenced. Can be a string path or a bound variable.

**Properties** (optional): Additional key-value pairs that define the member's properties within the collection. These are specific to the collection type and Item being referenced.

### Member Property Examples

For a WORKFLOW dependency member:
```groovy
Member(itemPath: '/desc/ActivityDesc/kernel/ManageCustomer') {
    Property('Version': 0)
}
```

For a SCHEMA dependency member:
```groovy
Member(itemPath: '/desc/Schema/kernel/Customer') {
    Property('Version': 0)
    Property('Viewpoint': 'last')
}
```

## Common Dependency Patterns

### Workflow Dependency (Most Common)

Every Item that has a lifecycle should have a WORKFLOW dependency:

```groovy
Item(name: 'Customer', folder: '/module/CRM', workflow: $customer_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageCustomer') {
            Property('Version': 0)
        }
    }
}
```

### Schema Dependencies

Link to Schemas used by the Item:

```groovy
Dependency(SCHEMA) {
    Member(itemPath: '/desc/Schema/kernel/Customer') {
        Property('Version': 0)
    }
}
```

### Multiple Dependencies

An Item can have multiple Dependency declarations:

```groovy
Item(name: 'Customer', folder: '/module/CRM', workflow: $customer_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageCustomer') {
            Property('Version': 0)
        }
    }
    
    Dependency(SCHEMA) {
        Member(itemPath: '/desc/Schema/kernel/Customer') {
            Property('Version': 0)
        }
    }
    
    Dependency(SCRIPT) {
        Member(itemPath: '/desc/Script/kernel/ValidateCustomer') {
            Property('Version': 0)
        }
    }
}
```

## DependencyDescription Patterns

### Schema Description

```groovy
DependencyDescription(SCHEMA) {
    Member($customer_PropertyDescriptionList)
}
```

### Include Description

```groovy
DependencyDescription(INCLUDE) {}
```

### Multiple Descriptions

```groovy
Item(name: 'CustomerFactory', folder: '/module/CRM', workflow: $factory_Workflow) {
    InmutableProperty('Type': 'Factory')
    
    DependencyDescription(SCHEMA) {
        Member($customer_PropertyDescriptionList)
    }
    
    DependencyDescription(SCRIPT) {
        Member($validation_PropertyDescriptionList)
    }
    
    DependencyDescription(QUERY) {
        Member($query_PropertyDescriptionList)
    }
}
```

## Best Practices

1. **Workflow First**: Always declare `Dependency(WORKFLOW)` first, as it defines the Item's lifecycle
2. **Group by Type**: Group dependencies of the same type together
3. **Use BuiltInCollections**: Prefer constants from `BuiltInCollections` over string literals for collection types
4. **Specify Versions**: Always include `Version` property for versioned references
5. **Descriptions Last**: Place `DependencyDescription` calls at the end of the Item definition

## See Also

- [Item Reference](item.md) — Item constructor and attributes
- [BuiltInCollections.java](../../../../kernel/src/main/java/org/cristalise/kernel/collection/BuiltInCollections.java) — Complete list of built-in collection types
- [Collection DSL](../../../../dsl/CONTEXT.md#Collection%20DSL) — Collection domain definitions
- [kernel/CONTEXT.md](../../../../kernel/CONTEXT.md) — Core CRISTAL-iSE domain definitions
