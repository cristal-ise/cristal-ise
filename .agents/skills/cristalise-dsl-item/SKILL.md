---
name: cristalise-dsl-item
description: Complete reference for defining Items, Agents, Entities, and Resources in CRISTAL-iSE DSL. Use when creating or modifying Item, Agent, or Description definitions, configuring CRUD operations, properties, outcomes, dependencies, or when another skill needs Item structure details.
---

# CRISTAL-iSE Item DSL

**Item** is the fundamental business object in CRISTAL-iSE. This skill is the complete reference for defining **ImportItem** Description Items using the CRISTAL-iSE Groovy DSL, including Item constructor attributes, Property configuration, Outcome references, and Dependency/DependencyDescription definitions.

## Quick Start

### Minimal Item

```groovy
Item(name: 'MyItem', folder: '/module/MyModule', workflow: 'NoWorkflow') {
}
```

### Item with Properties

```groovy
Item(name: 'Customer', folder: '/module/CRM', workflow: $customer_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    InmutableProperty('Module': 'CRM')
    InmutableProperty('Root': '/module/CRM')
    Property('State': 'Active')
}
```

### Item with Outcome

```groovy
Item(name: 'Customer', folder: '/module/CRM', workflow: $customer_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    
    Outcome($customer_PropertyDescriptionList)
    Outcome(schema: 'CustomerData', version: 0, viewname: 'last', path: 'boot/crm/Customer_0.xml')
}
```

### Item with Dependencies

```groovy
Item(name: 'Customer', folder: '/module/CRM', workflow: $customer_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageCustomer') {
            Property('Version': 0)
        }
    }
    
    DependencyDescription(SCHEMA) {
        Member($customer_PropertyDescriptionList)
    }
}
```

## Core Concepts

| Concept | File | Description |
|---------|------|-------------|
| **Item** | [references/item.md](references/item.md) | Root Item definition and attributes |
| **ImportItem** | [references/item.md](references/item.md) | Description Item that defines Item structure |
| **Property** | [references/properties.md](references/properties.md) | Mutable key-value metadata |
| **InmutableProperty** | [references/properties.md](references/properties.md) | Immutable key-value metadata |
| **Outcome** | [references/outcomes.md](references/outcomes.md) | Schema-based data references |
| **Dependency** | [references/dependencies.md](references/dependencies.md) | Variable-member collection references |
| **DependencyDescription** | [references/dependencies.md](references/dependencies.md) | Template for creating Dependency collections |
| **Agent** | [references/item.md](references/item.md) | Item with Roles for Activity execution |
| **Module** | [references/item.md](references/item.md) | Namespace and version container |

## Reference Files

### Concept References

- **[references/item.md](references/item.md)** — Item constructor, attributes, verification, ImportItem, Agent, Module
- **[references/properties.md](references/properties.md)** — Property vs InmutableProperty, syntax, constraints, mandatory properties
- **[references/outcomes.md](references/outcomes.md)** — Outcome references, binding, paths, multiple outcomes
- **[references/dependencies.md](references/dependencies.md)** — Dependency, DependencyDescription, Member syntax with BuiltInCollections

### Supporting Files

- **[EXAMPLES.md](EXAMPLES.md)** — 10+ complete, runnable Item definitions
- **[GLOSSARY.md](GLOSSARY.md)** — Alphabetical glossary of defined terms

## Key Rules

1. **Required Attributes**: `name` and `folder` are mandatory constructor parameters
2. **Mandatory Properties**: Every Item MUST have `Module`, `Name`, and `Type` properties
3. **Type Property First**: `Type` property should be set first to establish Item classification
4. **Workflow Binding**: If `workflow` is a string, it must reference an existing Workflow or CompositeActivityDef
5. **Verification**: Always verify Item definitions

## Best Practices

### Naming
- Items: `PascalCase`
- Modules: `lowercase` or `PascalCase` (follow existing convention in your codebase)
- Types: `PascalCase` for custom type values

### Properties
- Group related properties together
- Place `Type`, `Module`, `Name` at the top of the property list
- Use `InmutableProperty` for type-defining metadata (Type, Module, Root)
- Use `Property` for mutable configuration

### Dependencies
- Declare `Dependency(WORKFLOW)` first, as it defines the Item's lifecycle
- Group `DependencyDescription` calls together at the end
- Use meaningful collection names from `BuiltInCollections`

### Versioning
- Start with `version: 0` for new Items
- Increment version when structural changes occur
- Maintain backward compatibility where possible

## Common Patterns

| Pattern | Use Case | Example |
|---------|----------|---------|
| **ImportItem** | Defines the complete structure, properties, collections, workflow, and lifecycle for creating new Items | `Item(...) { InmutableProperty('Type': 'ImportItem') }` |
| **Factory** | A simplified form of Description that only contains Activities for creating Items | `Item(...) { InmutableProperty('Type': 'Factory') }` |
| **ImportAgent** | Defines an Agent, including its Roles and permissions | `Item(...) { InmutableProperty('Type': 'ImportAgent') }` |
| **ImportRole** | Defines a Role, including its permissions | `Item(...) { InmutableProperty('Type': 'ImportRole') }` |
| **Module** | Defines a collection of Items that implement a set of functionalities, with its own namespace and version | `Item(...) { InmutableProperty('Type': 'Module') }` |
| **Description** | Contains all the logic (Lifecycle) to maintain the data describing other Items | `Item(...) { InmutableProperty('Type': 'Description') }` |
| **DomainContext** | Defines the domain namespace and context for Items | `Item(...) { InmutableProperty('Type': 'DomainContext') }` |

## Anti-Patterns

**AVOID:**
- Missing mandatory `Module`, `Name`, or `Type` properties
- Duplicate property keys in a single Item
- Circular dependency references between Items
- Hardcoded item paths (use variables like `$my_Workflow` where possible)
- Items without any `Type` property
- `Dependency` without at least one `Member`

## Error Handling

| Error | Cause | Fix |
|-------|-------|-----|
| `name is required` | Missing name attribute | Provide name parameter |
| `folder is required` | Missing folder attribute | Provide folder parameter |
| `Module property is mandatory` | Missing Module property | Add InmutableProperty('Module': ...) |
| `Type property is mandatory` | Missing Type property | Add InmutableProperty('Type': ...) |
| `Workflow not found` | Invalid workflow reference | Verify workflow exists and is accessible |
| `Duplicate property key` | Same property key used twice | Use unique property keys |

## Information Hierarchy

```
cristalise-dsl-item/
├── SKILL.md
│   ├── Quick Start
│   ├── Core Concepts
│   ├── Reference Files
│   ├── Key Rules
│   ├── Best Practices
│   ├── Common Patterns
│   ├── Anti-Patterns
│   └── Error Handling
├── EXAMPLES.md
├── GLOSSARY.md
└── references/
    ├── item.md
    ├── properties.md
    ├── outcomes.md
    └── dependencies.md
```

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [writing-great-skills](../writing-great-skills/SKILL.md) — Blueprint for skill writing principles
- [cristalise-dsl-workflow](../cristalise-dsl-workflow/SKILL.md) — Complete workflow DSL reference
- [cristalise-dsl-schema](../cristalise-dsl-schema/SKILL.md) — Complete schema DSL reference
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Module DSL reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Core CRISTAL-iSE domain definitions
