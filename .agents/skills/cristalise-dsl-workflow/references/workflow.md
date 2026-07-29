# Workflow

A **Workflow** creates a kernel Workflow, defining the dependency graph of Activities for an Item's Lifecycle. Each Item has exactly one Workflow that dictates its valid Activity sequences.

## Constructor

```groovy
Workflow(name: 'WorkflowName', version: 0, generate: true) {
    // Layout definition
}
```

### Parameters

| Parameter | Required | Type | Description |
|-----------|----------|------|-------------|
| `name` | Yes | String | Workflow identifier |
| `version` | Yes | Integer | Version number |
| `generate` | No | Boolean | If true, generates layout automatically |

## In Item Definition

Workflows are referenced in Item definitions:

```groovy
Item(name: 'MyItem', version: 0, folder: '/myPath', 
      workflow: 'MyItem_Workflow', workflowVer: 0) {
    
    Dependency('workflow') {
        Member(itemPath: '/desc/Workflow/myPath/MyItem_Workflow') {
            Property('Version': 0)
        }
    }
}
```

## Verification

Always verify workflows:

```groovy
wf.verify()
```

## See Also

- [Layout](layout.md) — Container for workflow pattern primitives
- [Activity](activity.md) — Activity references in workflows
- [Split Patterns](splits.md) — AndSplit, OrSplit, XOrSplit, Loop
- [../EXAMPLES.md](../EXAMPLES.md) — Complete workflow examples
