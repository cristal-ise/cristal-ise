---
name: cristalise-dsl-domain
description: Complete reference for DomainContext definitions in CRISTAL-iSE DSL. Use when working with domain, namespace, context, or path, or when another skill needs DomainContext details.
---

# CRISTAL-iSE Domain DSL

This skill will provide complete reference for defining **DomainContext** constructs in the CRISTAL-iSE Groovy DSL. DomainContext establishes domain namespaces and context for organizing Items and managing domain-level configurations.

## When to Use This Skill

- Defining DomainContext Items
- Configuring domain namespaces and paths
- Setting domain-level properties and permissions
- Organizing Items within domain hierarchies

## Quick Start

Define a minimal DomainContext with default version (0):
```groovy
DomainContext('/path')
```

Specify an explicit version:
```groovy
DomainContext('/path', 2)
```

Use within a Module's Contexts block:
```groovy
Contexts {
    DomainContext('/desc/dev', 0)
}
```

## Core Concepts

DomainContext has two fundamental concepts: **Path** and **Name**. The **Path** is a string representing the domain path (e.g., `/desc/PropertyDesc/ttt`) that defines the context's location in the domain hierarchy. The **Name** is automatically generated from the path by capitalizing each segment and appending "Context" (e.g., `DescPropertyDescTttContext`).

## Key Rules

1. **Path is mandatory**: Must provide a path string to the DomainContext constructor
2. **Path must not have target**: Path cannot contain a target segment (throws `InvalidDataException`)
3. **Version defaults to 0**: If not specified, version defaults to 0
4. **Name is auto-generated**: Name is derived from path by capitalizing each segment and appending "Context"
5. **Namespace from parent**: Namespace is inherited from the parent Module or DomainContextBuilder

## Best Practices

### General

- Use version 0 for new DomainContexts
- Ensure namespace matches the module namespace
- Group related contexts together in the Contexts block

## Error Handling

| Error | Cause | Fix |
|-------|-------|-----|
| `cannot work with empty attributes (Map)` | Missing or empty attributes in DomainContextBuilder.build() | Provide valid attributes map |
| `DomainContext '...' has target` | Path contains a target segment | Use a path without target |

## Information Hierarchy

```
cristalise-dsl-domain/
└── SKILL.md
    ├── When to Use This Skill
    ├── Quick Start
    ├── Core Concepts
    ├── Key Rules
    ├── Best Practices
    ├── Error Handling
    └── See Also
```

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Complete Item DSL reference
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Module DSL reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
