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

## Agent & Security

### Example 9: ImportAgent Item

Defines an Agent with Roles and permissions.

```groovy
Item(name: 'SystemAdmin', folder: $descDevContext_DomainContext, workflow: $agentFactoryWf_CompositeActivityDef) {
    InmutableProperty('Type': 'ImportAgent')
    
    Outcome($agent_PropertyDescriptionList)
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageAgent') {
            Property('Version': 0)
        }
    }
    
    // Agent-specific dependencies would be added here
    // to define Roles and permissions
}
```

### Example 10: ImportRole Item

Defines a Role with permissions.

```groovy
Item(name: 'Administrator', version: 0, folder: ROLE_DESC_RESOURCE.typeRoot, 
     workflow: $crudFactory_Workflow) {
    InmutableProperty('Type': 'ImportRole')
    InmutableProperty('Root': ROLE_DESC_RESOURCE.typeRoot)
    InmutableProperty('UpdateSchema': ROLE_DESC_RESOURCE.schemaName + ':0')
    
    Outcome($roleDesc_PropertyDescriptionList)
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageRoleDesc') {
            Property('Version': 0)
        }
    }
}
```

---

## Advanced Items

### Example 11: Item with Multiple Outcomes

Item with primary and secondary outcome schemas.

```groovy
Item(name: 'Order', folder: '/module/Sales', workflow: $order_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    InmutableProperty('Module': 'Sales')
    InmutableProperty('Name': 'Order')
    
    // Primary outcome for order data
    Outcome(schema: 'Order', version: 0, viewname: 'last', path: 'boot/sales/Order_0.xml')
    
    // Secondary outcome for order history
    Outcome(schema: 'OrderHistory', version: 0, viewname: 'last', path: 'boot/sales/OrderHistory_0.xml')
    
    // Metadata outcome
    Outcome($order_PropertyDescriptionList)
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/sales/ManageOrder') {
            Property('Version': 0)
        }
    }
}
```

### Example 12: Item with Multiple Dependencies

Item with workflow, schema, script, and query dependencies.

```groovy
Item(name: 'Invoice', folder: '/module/Accounting', workflow: $invoice_Workflow) {
    InmutableProperty('Type': 'ImportItem')
    InmutableProperty('Module': 'Accounting')
    InmutableProperty('Name': 'Invoice')
    
    Outcome($invoice_PropertyDescriptionList)
    Outcome(schema: 'Invoice', version: 0, viewname: 'last', path: 'boot/accounting/Invoice_0.xml')
    
    // Workflow dependency
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/accounting/ManageInvoice') {
            Property('Version': 0)
        }
    }
    
    // Schema dependency
    Dependency(SCHEMA) {
        Member(itemPath: '/desc/Schema/accounting/Invoice') {
            Property('Version': 0)
        }
    }
    
    // Script dependencies
    Dependency(SCRIPT) {
        Member(itemPath: '/desc/Script/accounting/CalculateTax') {
            Property('Version': 0)
        }
    }
    
    // Query dependency
    Dependency(QUERY) {
        Member(itemPath: '/desc/Query/accounting/GetCustomerBalance') {
            Property('Version': 0)
        }
    }
}
```

### Example 13: Item with DependencyDescription

Factory Item with multiple DependencyDescription templates.

```groovy
Item(name: 'DocumentFactory', version: 0, folder: DOCUMENT_RESOURCE.typeRoot, 
     workflow: $crudFactory_Workflow) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': DOCUMENT_RESOURCE.typeRoot)
    InmutableProperty('UpdateSchema': DOCUMENT_RESOURCE.schemaName + ':0')
    
    Outcome($document_PropertyDescriptionList)
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageDocument') {
            Property('Version': 0)
        }
    }
    
    // Template collections for creating new Items
    DependencyDescription(SCHEMA) {
        Member($document_PropertyDescriptionList)
    }
    
    DependencyDescription(SCRIPT) {
        Member($validation_PropertyDescriptionList)
    }
    
    DependencyDescription(QUERY) {
        Member($query_PropertyDescriptionList)
    }
}
```

---

## Domain Patterns

### Example 14: DomainContext Item

Defines the domain namespace and context for Items.

```groovy
Item(name: 'Enterprise', version: 0, folder: DOMAIN_CONTEXT_RESOURCE.typeRoot, 
     workflow: $crudFactory_Workflow) {
    InmutableProperty('Type': 'DomainContext')
    InmutableProperty('Root': DOMAIN_CONTEXT_RESOURCE.typeRoot)
    InmutableProperty('UpdateSchema': DOMAIN_CONTEXT_RESOURCE.schemaName + ':0')
    
    Outcome($domainContext_PropertyDescriptionList)
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageDomainContext') {
            Property('Version': 0)
        }
    }
    
    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/kernel/DomainContext') {
            Property('Version': 0)
        }
    }
}
```

### Example 15: Complex Factory with All Collections

Comprehensive Factory Item demonstrating all collection types.

```groovy
Item(name: 'CompleteFactory', version: 0, folder: '/module/Complete', 
     workflow: $crudFactory_Workflow) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/module/Complete')
    
    Outcome($complete_PropertyDescriptionList)
    
    Dependency(WORKFLOW) {
        Member(itemPath: '/desc/ActivityDesc/kernel/ManageComplete') {
            Property('Version': 0)
        }
    }
    
    Dependency(SCHEMA) {
        Member(itemPath: '/desc/Schema/complete/Complete') {
            Property('Version': 0)
        }
    }
    
    Dependency(SCRIPT) {
        Member(itemPath: '/desc/Script/complete/Validate') {
            Property('Version': 0)
        }
    }
    
    Dependency(QUERY) {
        Member(itemPath: '/desc/Query/complete/Lookup') {
            Property('Version': 0)
        }
    }
    
    Dependency(STATE_MACHINE) {
        Member(itemPath: '/desc/StateMachine/complete/Process') {
            Property('Version': 0)
        }
    }
    
    // Template collections
    DependencyDescription(SCHEMA) {
        Member($schema_PropertyDescriptionList)
    }
    
    DependencyDescription(SCRIPT) {
        Member($script_PropertyDescriptionList)
    }
    
    DependencyDescription(QUERY) {
        Member($query_PropertyDescriptionList)
    }
    
    DependencyDescription(STATE_MACHINE) {
        Member($stateMachine_PropertyDescriptionList)
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
