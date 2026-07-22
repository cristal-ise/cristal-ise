---
name: cristalise-dsl-property
description: Complete reference for defining PropertyDescriptionList Items in CRISTAL-iSE DSL. Use when creating or modifying PropertyDescriptionList, or when another skill needs property description details.
---

# CRISTAL-iSE PropertyDescriptionList DSL

**PropertyDescriptionList** defines metadata descriptions for Item types in CRISTAL-iSE. It specifies which properties an Item type can have, their default values, mutability, and whether they serve as class identifiers. PropertyDescriptionLists are referenced by Items via the `Outcome` construct and are used throughout the system for validation and UI rendering.

## Quick Start

### Minimal PropertyDescriptionList

```groovy
PropertyDescriptionList('Customer', 0) {
    PropertyDesc(name: 'Name', isMutable: true)
}
```

### PropertyDescriptionList with Type classification

```groovy
PropertyDescriptionList('Customer', 0) {
    PropertyDesc(name: 'Name',   isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true, defaultValue: 'Customer')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false)
}
```

### PropertyDescriptionList with defaults

```groovy
PropertyDescriptionList('Order', 0) {
    PropertyDesc(name: 'Name',   isMutable: true,  isClassIdentifier: false, defaultValue: '')
    PropertyDesc(name: 'Type',   isMutable: false, isClassIdentifier: true,  defaultValue: 'Order')
    PropertyDesc(name: 'State',  isMutable: true,  isClassIdentifier: false, defaultValue: 'Draft')
    PropertyDesc(name: 'Module', isMutable: false, isClassIdentifier: false, defaultValue: 'Sales')
}
```

### PropertyDescriptionList with transitive properties

```groovy
PropertyDescriptionList('Task', 0) {
    PropertyDesc(name: 'Name',       isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',       isMutable: false, isClassIdentifier: true, defaultValue: 'Task')
    PropertyDesc(name: 'Priority',   isMutable: true,  isClassIdentifier: false, isTransitive: true)
    PropertyDesc(name: 'Module',     isMutable: false, isClassIdentifier: false)
}
```

### PropertyDescriptionList without namespace/version (module context)

```groovy
PropertyDescriptionList {
    PropertyDesc(name: 'Name', isMutable: true)
    PropertyDesc(name: 'Type', isMutable: false, isClassIdentifier: true, defaultValue: 'GenericItem')
}
```

## Core Concepts

| Concept                     | Description                                                                                     |
|-----------------------------|-------------------------------------------------------------------------------------------------|
| **PropertyDescriptionList** | Container for PropertyDesc definitions. Establishes the metadata schema for an Item type.       |
| **PropertyDesc**            | Individual property descriptor with configuration for mutability, identification, and defaults. |
| **name**                    | Property key (mandatory). Must be unique within the PropertyDescriptionList.                    |
| **defaultValue**            | Default value for the property. Must be String type.                                            |
| **isMutable**               | Whether the property can be modified after Item creation (default: true).                       |
| **isClassIdentifier**       | Whether the property is a class identifier (default: false).                                    |
| **isTransitive**            | Whether the property is converted to VertexProperties (default: false).                         |

## Key Rules

1. **name is mandatory**: Every PropertyDesc must have a `name` attribute
2. **defaultValue must be String**: Property values can only hold text; non-String defaultValues throw `InvalidDataException`
3. **isMutable defaults to true**: Properties are mutable unless explicitly set to false
4. **isClassIdentifier defaults to false**: Type Property is typically is a class identifier
5. **isTransitive defaults to false**: Transitive properties are converted to VertexProperties. Class identifiers are automatically transitive regardless of this flag
6. **Version defaults to 0**: If not specified, version defaults to 0
7. **Namespace from parent**: When defined in a Module, namespace is inherited from the parent Module

## Best Practices

### General
- Always include a `Type` PropertyDesc with `isClassIdentifier: true`
- Include `Name` and `Module` PropertyDescs for all Item types
- Use `isMutable: false` for type-defining metadata (Type, Module, Root)
- Use `isMutable: true` for runtime configuration (State, Name, Status)
- Set meaningful `defaultValue` for properties that should have sensible defaults
- Group related PropertyDescs together
- Do not set `isTransitive: true` on class identifiers (they are automatically transitive)

### PropertyDesc Ordering
- Place `Type` PropertyDesc first as the class identifier
- Follow with `Name` and `Module` for standard metadata
- Add domain-specific properties after standard ones
- Order properties by importance/usage frequency

### Naming
- Property names: `PascalCase` for custom properties
- Type values: `PascalCase` for type identifiers
- Default values: match the expected data type (but must be String)

## Anti-Patterns

**AVOID:**
- Missing `name` attribute on PropertyDesc
- Using non-String `defaultValue` (will throw InvalidDataException)
- PropertyDescriptionList without any `isClassIdentifier: true` PropertyDesc
- Duplicate property names within a single PropertyDescriptionList
- Setting `isTransitive: true` on class identifiers (they are automatically transitive)
- Overusing `isTransitive: true` (class identifiers are already transitive)

## Error Handling

| Error                                      | Cause                          | Fix                                     |
|--------------------------------------------|--------------------------------|-----------------------------------------|
| `PropertyDesc must have the name set`      | Missing name attribute         | Provide name parameter                  |
| `defaultValue must be String type`         | Non-String defaultValue        | Use String value or remove defaultValue |
| `PropertyDescriptionList '...' has target` | Path contains a target segment | Use a path without target               |

## Information Hierarchy

```
cristalise-dsl-property/
└── SKILL.md
    ├── Quick Start
    ├── Core Concepts
    ├── Key Rules
    ├── Best Practices
    ├── Anti-Patterns
    └── Error Handling
```

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [writing-great-skills](../writing-great-skills/SKILL.md) — Blueprint for skill writing principles
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Complete Item DSL reference
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Module DSL reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
