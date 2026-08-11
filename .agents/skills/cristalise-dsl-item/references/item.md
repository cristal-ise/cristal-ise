# Item Reference

This file documents the **Item** and **Agent** DSL keywords with its attributes, the verification strategy togther with basic explanation of java classes implementing these concenpts in the kernel.

## Item

The **Item** keyword is the entry point for defining Items in the CRISTAL-iSE DSL. It accepts a map of attributes and a closure for defining the Item's contents. The DSL is handled by ModuleDelegate and ItemDelegate classes to create ImportItem instances.

```groovy
Item(name: 'MyItem', folder: '/module/MyModule', workflow: 'NoWorkflow') {
    // Item contents: properties, outcomes, dependencies
}
```

### Attributes

The **Item**  accepts these attributes:

**name** (String, *required*): The unique name of the Item within its Module. This identifies the Item type and is used in domain paths.

**version** (Integer, *optional*, default: 0): The version number of the Item. Start with 0 and increment when structural changes occur.

**folder** (DomainPath or String, *required*): The initial domain path where the Item is created. This can be a `DomainPath` object or a string path like `'/module/MyModule'`.

**workflow** (String, Workflow, or CompositeActivityDef, *optional*): Reference to the Workflow defining the Item's lifecycle. Can be:
- A string name of an existing Workflow. In this case, the workflowVer attribute must also be specified.
- A `Workflow` object
- A `CompositeActivityDef` object (using `$variable` syntax)
- The special value `'NoWorkflow'` for Items without lifecycle

**workflowVer** (Integer, *optional*): the version of the must be provided in case the workflow attribute specifies the Name.

**namespace** (String, *optional*): The namespace for the Item, used for organisation and scoping. Namespace is defined in the **Module**.

### ImportItem

**ImportItem** is a Description Item that defines the complete structure, properties, collections, workflow, and lifecycle for creating new Items. Using the **Item** in the DSL an ImportItem instance is created.


## Agent

The **Agent** keyword is the entry point for defining Agent in the CRISTAL-iSE DSL. The DSL is handled by ModuleDelegate and AgentDelegate classes to create an ImportAgent instance. An **Agent** is an Item with Roles that is entitled to execute Activities. Agents must be authenticated and authorized in the system.

```groovy
Agent(name: 'testUser1', password: '12345', folder: '//Agents') {
  // Agent-specific properties and Role dependencies
}
```

### Attributes

The **Agent** accepts all the attributes of **Item** together with these extra attributes:

**password** (String, *required*): 

### ImportAgent

**ImportAgent** is a Description Item that defines the complete structure, properties, collections, workflow, and lifecycle for creating new Agant. Using the **Agent** in the DSL an ImportAgent instance is created.

--

## Verification

Always verify Item/Agent DSL definitions after creating them to ensure they are correctly defined. DSL implementation classes also assert provided data for consistency checks.

```groovy
def module = ModuleBuilder.build('ttt', 'integtest', 0) {
  Item(name: 'MyItem', folder: '/module/MyModule', workflow: 'NoWorkflow') {
    // Item contents: properties, outcomes, dependencies
  }

  Agent(name: 'testUser1', password: '12345', folder: '//Agents') {
    // Agent-specific properties and Role dependencies
  }
}

assert module.getImports().list.size() == 2
assert module.getImports().findImport('MyItem', 'item')
assert module.getImports().findImport('testUser1', 'agent')
```

## See Also

- [Property Reference](properties.md) — Property and InmutableProperty syntax
- [Outcome Reference](outcomes.md) — Outcome configuration
- [Dependency Reference](dependencies.md) — Dependency and DependencyDescription
- [kernel/CONTEXT.md](../../../../kernel/CONTEXT.md) — Core CRISTAL-iSE domain definitions
