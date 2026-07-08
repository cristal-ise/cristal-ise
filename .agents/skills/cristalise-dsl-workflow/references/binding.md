# Variable Binding Convention

Variables prefixed with `$` (e.g., `$my_ActivityDef`, `$create_Activity`) are **automatically created** by the CRISTAL-iSE DSL when a complete module script is executed. This is handled by the `BindingConvention` trait.

## How Variables Are Created

When a module DSL script is executed, the system automatically adds DescriptionObjects to the script's binding using a consistent naming convention:

```groovy
// For an ActivityDef named "Create"
Activity('Create', 0) {
    // ... definition
}

// Automatically creates: $create_ActivityDef
```

## Naming Convention

### For DescriptionObjects

For objects that implement `DescriptionObject` (ActivityDef, Schema, Script, Query, etc.):

```
$ + convertToValidName(obj.name) + '_' + obj.class.simpleName
```

**Examples:**

| Object Type | Name | Variable Name |
|----------|------|---------------|
| Activity | `Create` | `$create_ActivityDef` |
| Workflow | `MyWorkflow` | `$myWorkflow_CompositeActivityDef` |
| Schema | `Patient` | `$patient_Schema` |
| Script | `Validate` | `$validate_Script` |
| Query | `FindItems` | `$findItems_Query` |

### Name Conversion Rules

The `convertToValidName()` method:
1. Converts the first character to lowercase
2. Removes all special characters: `[\s,./:!?;\$]+`

**Examples:**
- `"My Activity"` → `"myActivity"`
- `"Create-Item"` → `"createItem"`
- `"Test:Activity"` → `"testActivity"`

### For Other Objects

For objects that are not DescriptionObjects, the system uses **default bean keys** to extract a name:
- Default keys: `id`, `key`, `name` (configurable via `DSL_Module_BindingConvention_defaulBeanKeys`)
- The first non-null property value is used

## Configuration Properties

The binding convention is configurable via system properties:

| Property | Default | Description |
|----------|---------|-------------|
| `DSL.Module.BindingConvention.variablePrefix` | `$` | Prefix for all bound variables |
| `DSL.Module.BindingConvention.autoAddObject` | `true` | Whether to automatically add objects to bindings |
| `DSL.Module.BindingConvention.defaulBeanKeys` | `id,key,name` | Property names to try when extracting a name |

## Usage in Workflows

```groovy
Workflow('MyWorkflow', 0, generate: true) {
    Layout {
        // Reference the automatically bound variable
        Act('Create', $create_ActivityDef)
        Act('Validate', $validate_ActivityDef)
    }
}
```

## Module Script Example

```groovy
// dev/src/main/module/org/cristalise/dev/Activity.groovy

Activity('Create', 0) {
    Property((OUTCOME_INIT): 'Empty')
    Schema($create_Schema)
}

Activity('Validate', 0) {
    Property((OUTCOME_INIT): 'Empty')
}

// These variables are automatically created:
// $create_ActivityDef
// $validate_ActivityDef
// $create_Schema
```

## Disabling Auto-Binding

To disable automatic binding, set the system property:

```groovy
System.setProperty('DSL.Module.BindingConvention.autoAddObject', 'false')
```

## Custom Variable Prefix

To change the prefix from `$` to another character:

```groovy
System.setProperty('DSL.Module.BindingConvention.variablePrefix', '@')
// Now variables will be: @create_ActivityDef, @validate_ActivityDef, etc.
```

## See Also

- [Activity](activity.md) — Activity definitions that get auto-bound
- [Workflow](workflow.md) — Where bound variables are used
- [../EXAMPLES.md](../EXAMPLES.md) — Examples using bound variables
