# Item DSL Examples

This file contains **10+ complete, runnable Item definitions** organized by category, demonstrating all major patterns of the CRISTAL-iSE Item DSL.

## Table of Contents

- [Basic Items](#basic-items)
- [Description Items](#description-items)
- [Agent & Security](#agent--security)
- [Advanced Items](#advanced-items)
- [Domain Patterns](#domain-patterns)

---

## Basic Items

### Example 1: Minimal Item

The simplest possible Item with only required attributes and no lifecycle.

```groovy
Item(name: 'SimpleItem', folder: '/module/Test', workflow: 'NoWorkflow') {
}
```

### Example 2: Item with Mandatory Properties

Item with all mandatory properties explicitly defined.

```groovy
Item(name: 'MyItem', folder: '/module/Test', workflow: 'NoWorkflow') {
    InmutableProperty('Type': 'ImportItem')
    InmutableProperty('Module': 'Test')
}
```

### Example 3: Item with Properties

Item demonstrating both immutable and mutable properties.

```groovy
Item(name: 'Product', folder: '/module/Inventory', workflow: $product_Workflow) {
    // Immutable properties for identification
    InmutableProperty('Type': 'ImportItem')
    InmutableProperty('Module': 'Inventory')
    InmutableProperty('Root': '/module/Inventory')
    
    // Mutable properties for configuration
    Property('State': 'Active')
    Property('Category': 'Electronics')
}
```

---

## Description Items

### Example 4: Factory Item - variation 1

Factory is a simplified form of Description Item (see later examples and CONTEXT.md of kernel). This variation uses variables created during the full module generation executing the `Module.groovy` DSL script. It defines the Item ro create Patients.

The 
- Outcome is 

```groovy
Item(name: 'PatientFactory', version: 0, folder: '/integTest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty('Type': 'Factory')
  InmutableProperty('Root': '/integTest/Patients')

  Outcome($patient_PropertyDescriptionList)

  Dependency('workflow') {
    Member($patient_Workflow)
  }

  Dependency(SCHEMA_INITIALISE) {
    Member($patient_Details_Schema)
  }

  Dependency(MASTER_SCHEMA) {
    Member($patient_Schema)
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member($atient_Aggregate_Script)
  }

  DependencyDescription('Doctor') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): ManyToOne.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'Patients')
    }
    Member($doctor_PropertyDescriptionList)
  }
}
```

### Example 6 A: DescriptionFactory variation A

This is a Factory Item that creates Descriptions Items using text to reference Items required for its functionalities:
- `CrudFactory_Workflow` is the Lifecycle of the DescriptionFactory itself
- Outcome `boot/property/ItemDescription_0.xml` is used as the `PropertyDescription` of the new Description Item
- Dependency `workflow` referencing `/desc/ActivityDesc/dev/Description_Workflow` CompActDesc Item which is used as the workflow of the new Description Item
- DependencyDescription `workflow'` referencing `/desc/PropertyDesc/dev/CompositeActivityDesc` PropertyDescription Item which is used as the workflow of the new Description Item

```groovy
Item(name: 'DescriptionFactory', version: 0, folder: '/desc/dev', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/desc/dev')

    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/ItemDescription_0.xml')

    Dependency('workflow') {
      Member(itemPath: '/desc/ActivityDesc/dev/Description_Workflow') {
        Property('Version': 0)
      }
    }

    DependencyDescription("workflow'") {
        Member(itemPath: '/desc/PropertyDesc/dev/CompositeActivityDesc') {
          Property('Version': 0)
        }
    }
}
```

### Example 6 B: DescriptionFactory variation B

This is a Factory Item that creates Descriptions Items.

```groovy
Item(name: 'DescriptionFactory', version: 0, folder: $descDevContext_DomainContext, workflow: $crudFactory_Workflow_CompositeActivityDef) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': $descDevContext_DomainContext)

    Outcome($itemDescription_PropertyDescriptionList)

    Dependency(WORKFLOW) {
        Member($description_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }
    DependencyDescription(WORKFLOW_PRIME) {
        Member($compositeActivityDesc_PropertyDescriptionList)
    }
}
```

---

## Notes

All examples follow the CRISTAL-iSE DSL conventions:
- Mandatory `Module`, `Name`, `Type` properties on all Items
- `InmutableProperty` for structural metadata
- `Property` for mutable configuration
- `Dependency(WORKFLOW)` declared first
- `DependencyDescription` calls grouped at the end
- Consistent path naming for outcomes

These examples can be copied directly into `.groovy` files in the CRISTAL-iSE `dev` module or any DSL module.

## See Also

- [Item DSL SKILL.md](SKILL.md) — Complete Item DSL reference
- [kernel/CONTEXT.md](../../../kernel/CONTEXT.md) — Core domain definitions
- [dev/Item.groovy](../../../../dev/src/main/module/org/cristalise/dev/Item.groovy) — Real-world examples from the codebase
