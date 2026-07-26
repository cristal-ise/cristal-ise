---
name: cristalise-dsl-module
description: Module definition reference in CRISTAL-iSE DSL. Use when creating or modifying module definitions, dependencies, and module-level configurations, or when another skill needs module organization details.
---

# CRISTAL-iSE Module DSL

A **Module** is the top-level container construct in CRISTAL-iSE Groovy DSL used to package and organize application resources, Items, Agents, Roles, DomainContexts, configurations, and cross-module dependencies.

## Quick Start

Modules are created via `ModuleBuilder.build(ns, name, version) { ... }` or by writing module scripts. Inside the closure, define metadata (`Info`), configuration (`Config`), resource URL (`Url`), resource references (`Schema`, `Workflow`, `Script`), entity definitions (`Item`, `Agent`, `Role`), or modular sub-files (`include`).

See [Examples](#examples) below for 5 complete patterns.

## Core Concepts

| Concept                 | Description                                                                                            |
|-------------------------|--------------------------------------------------------------------------------------------------------|
| **Module**              | Root container defining namespace (`ns`), `name`, and integer `version`.                               |
| **Info**                | Metadata block declaring module `description`, `version`, `kernel` version, and module `dependencies`. |
| **Config**              | Scoped module key-value configurations (e.g., debug settings, script engine overrides).                |
| **Url**                 | Base resource directory URL (`resURL`) for exporting and referencing module assets.                    |
| **Bootstrap**           | System startup process where modules are loaded, sorted by dependencies, and registered.               |
| **include**             | Inclusion of modular sub-scripts using `include` or `mandatoryInclude`.                                |

## Bootstrap & Module Loading

Modules are orchestrated by the **Bootstrap** process during system startup:

1.  **Loading**: `Gateway.init()` scans the classpath for module definitions.
2.  **Sorting**: `ModuleManager` resolves the loading order using the **dependencies closure** provided in the `Info` block.
3.  **Registration**: `Bootstrap.run()` imports all defined Items, Agents, and Roles into the kernel.

### The Importance of Import Order
Since modules can reference resources from other modules, the import order must be managed. If Module A is required by Module B, ensure Module A is listed in the dependencies closure of Module B's `Info` block.

## Key Rules

1. **Mandatory Module Attributes**: `ns` (namespace), `name`, and `version` are required constructor attributes.
2. **Explicit Dependencies**: Declare Module dependencies inside the closure of `Info` to specify the order of loading and import calculated within `Bootstrap`.
3. **Resource Order**: Declared resources and imports preserve their declaration order in `module.xml`.
4. **Namespace Inheritance**: Items created within a module inherit their parent module's namespace (`ns`).
5. **Configuration Namespacing**: Use dot-notation keys for `Config` entries to avoid property collisions.

## Examples

### 1. Minimal Module with Info & Dependencies

```groovy
ModuleBuilder.build('crm', 'core', 0) {
    Info(description: 'CRM Core Module', version: '1.0', kernel: '3.0') {
        ['CristaliseDev', 'CristalTrigger']
    }
}
```

### 2. Module Configurations & Resource URL

```groovy
ModuleBuilder.build('crm', 'core', 0) {
    Info(description: 'CRM Core Module', version: '1.0')
    Url('cristal/resources/crm/')
    Config(name: 'Module.debug', value: 'false')
    Config(name: 'OverrideScriptLang.javascript', value: 'rhino')
}
```

### 3. Module Referencing Existing Resources

```groovy
ModuleBuilder.build('crm', 'core', 0) {
    Script('ServerNewEntity', 0)
    Schema('Item', 0)
    StateMachine('Default', 0)
    Activity('EditDefinition', 0)
    Workflow('ManageScript', 0)
}
```

### 4. Module Defining Items, Agents, Roles, and Contexts

```groovy
ModuleBuilder.build('crm', 'core', 0) {
    DomainContext('/desc/crm', 0)

    Roles {
        Role(name: 'CRMAdmin', description: 'CRM Administrator')
    }

    Item(name: 'CustomerFactory', folder: '/crm', workflow: 'FactoryWorkflow') {
        InmutableProperty('Type': 'Factory')
    }

    Agent(name: 'CRMBot', password: 'secret') {
        Role('CRMAdmin')
    }
}
```

### 5. Modular Script Inclusion

```groovy
ModuleBuilder.build('crm', 'core', 0) {
    Info(description: 'CRM Modular Setup', version: '1.0')

    // Order is important; include PropertyDescriptionLists first as forward declarations
    include('./src/main/module/Properties.groovy') // Forward declarations for all Items
    include('./src/main/module/Customer.groovy') // Item declaration referencing Properties
}
```

## Best Practices

- **Grouping Declarations**: Place `Info`, `Url`, and `Config` at the top of the module definition closure. Also group related resource declarations (Schemas, Workflows, Scripts) together.
- **Forward Declarations**: Declare `PropertyDescriptionList` in a separate file (e.g., `Properties.groovy`) for all Items defined in the Module and `include` it first. Since an Item Property (e.g., `Type`) can be a `classIdentifier`, the `PropertyDescriptionList` acts as a forward declaration of the Item itself.
- **File Organization**: For better maintainability, create a new sub-file named after the Item's name (e.g., `Customer.groovy`) to house all associated declarations (Schemas, Activities, etc.), ensuring it is included after the forward declarations.
- **Inclusion Order**: The order of `include` statements is critical. Ensure that files containing prerequisite definitions (like base Schemas, Contexts, or forward declarations) are included before the files that reference them.

## Error Handling

| Error                                                 | Cause                                                     | Fix                                                              |
|-------------------------------------------------------|-----------------------------------------------------------|------------------------------------------------------------------|
| `assert args.ns && args.name && args.version != null` | Missing `ns`, `name`, or `version`                        | Provide `ns`, `name`, and `version` when instantiating Module    |
| `Specify only configDir or connect/config`            | Both `configDir` and explicit `connect`/`config` supplied | Specify `configDir` or `connect`/`config`, but not both          |
| `Directory '...' must exists`                         | Referenced workflow XML missing when `generate: false`    | Ensure XML file exists in boot directory or set `generate: true` |

## Information Hierarchy

```
cristalise-dsl-module/
└── SKILL.md
    ├── Quick Start
    ├── Core Concepts
    ├── Bootstrap & Module Loading
    ├── Key Rules
    ├── Examples (5 max)
    ├── Best Practices
    ├── Error Handling
    └── See Also
```

## See Also

- [writing-cristalise-dsl](../writing-cristalise-dsl/SKILL.md) — Router for all DSL skills
- [cristalise-dsl-item](../cristalise-dsl-item/SKILL.md) — Item DSL reference
- [cristalise-dsl-domain](../cristalise-dsl-domain/SKILL.md) — DomainContext DSL reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Core CRISTAL-iSE domain definitions
