---
name: cristalise-dsl-role
description: Complete reference for defining Role Items in CRISTAL-iSE DSL. Use when developing or modifying Role and Permission, or when another cristalise-dsl skill references roles.
---

# CRISTAL-iSE Role DSL

**Role** is a Description Item that defines a named collection of **Permissions** (Shiro WildcardPermission strings) that can be assigned to Agents. This skill is the complete reference for defining Role constructs using the CRISTAL-iSE Groovy DSL.

**Bold terms** are defined in [GLOSSARY.md](GLOSSARY.md) or in [kernel/CONTEXT.md](../../../kernel/CONTEXT.md).

## Quick Start

### Minimal Role

```groovy
Role(name: 'User')
```

### Role with Hierarchical Name

```groovy
Role(name: 'Admin/SuperUser', jobList: true)
```

### Role with String Permission

```groovy
Role(name: 'PrinterUser') {
    Permission('printer:print')
}
```

### Role with Wildcard Permission

```groovy
Role(name: 'PrinterAdmin') {
    Permission('printer:*')
}
```

### Role with 3-Part Permission

```groovy
Role(name: 'ItemManager') {
    Permission('Customer:Create:*')
}
```

### Role with Map Permission

```groovy
Role(name: 'QA') {
    Permission(domain: 'Batch', actions: 'Review', targets: '*')
}
```

## Core Concepts

| Concept | File | Description |
|---------|------|-------------|
| **Role** | Inline | A Description Item that defines a named collection of Permissions |
| **Permission** | [references/permission.md](references/permission.md) | Shiro WildcardPermission string in format `domain:action:target` |
| **jobList** | Inline | Boolean flag indicating if the Role can view the job list |

## Reference Files

- **[references/permission.md](references/permission.md)** — Permission syntax, string vs map format, WildcardPermission rules, CRISTAL-iSE domain:action:target mapping

## Key Rules

1. **Name is mandatory**: Every Role MUST have a `name` attribute
2. **jobList defaults to false**: If not specified, `jobList` is set to `false`
3. **Role can have many Permissions**: A single Role can contain zero or more Permission entries
4. **Admin Role in kernel**: The Admin Role is predefined in the kernel with permission `*` (all access)

## Best Practices

- **No default Role**: There is no default Role for any new Agent; permissions must be given explicitly
- **Meaningful names**: Use Role names that are meaningful to users (e.g., `Admin`, `QA`, `PrinterUser`)
- **Restrictive Permissions**: Keep Permissions as restrictive as possible; use `*` sparingly

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Items and Agents that use Roles
- [cristalise-dsl-module](../cristalise-dsl-module/SKILL.md) — Modules define Roles
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Domain definitions of Leading Words
- [Apache Shiro Permissions](https://shiro.apache.org/permissions.html) — WildcardPermission syntax reference
