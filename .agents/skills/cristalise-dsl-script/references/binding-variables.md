# Binding Variables

**Binding Variables** are the pre-defined variables automatically available in Script execution contexts.

## Core Binding Variables

CRISTAL-iSE automatically provides these binding variables to scripts:

### Item Context

| Variable | Type | Description | Availability |
|----------|------|-------------|-------------|
| `item` | `org.cristalise.kernel.entity.proxy.ItemProxy` | The current Item being processed | Most scripts |
| `agent` | `org.cristalise.kernel.entity.proxy.AgentProxy` | The Agent executing the script | Activity scripts |
| `job` | `org.cristalise.kernel.entity.Job` | The Job context | Activity scripts |

## Variable Availability by Script Type

### Activity-Linked Scripts

When a Script is linked to an Activity (via `Script($scriptVar)` in Activity definition), it receives:

```groovy
// All three variables are automatically available
Script('Patient_Update', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
  input('job', 'org.cristalise.kernel.entity.Job')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Update.groovy')
}
```

**Script Code (Patient_Update.groovy):**
```groovy
// Binding variables are automatically available
def currentName = item.getName()
def user = agent.getName()
def jobId = job.id

// Process the update
errors = updateItemLogic(item, user, jobId)
```

### Standalone Scripts (GET Endpoints)

For Scripts that execute as standalone endpoints, only declared inputs are available:

```groovy
Script('Patient_GenerateReport', 0) {
  input('reportType', 'java.lang.String')
  input('params', 'java.util.Map')
  output('report', 'java.lang.String')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_GenerateReport.groovy')
}
```

**Script Code (Patient_GenerateReport.groovy):**
```groovy
// Only declared inputs are available
def type = reportType  // From input declaration
def configuration = params  // From input declaration

report = generateReport(type, configuration)
```

## ScriptDevelopment Base Class

For scripts that extend `ScriptDevelopment` (development/testing scripts), additional binding variables are set:

### Additional Binding Variables

From `ScriptDevelopment.groovy`:

```groovy
// These are the default binding variables created by the Script class
AgentProxy agent = Gateway.connect(user, pwd)
ItemProxy  item  = agent.getItem(itemPath)

binding.setProperty('agent', agent)
binding.setProperty('item',  item)

// For activity scripts
if (activityName) {
    Job job = item.getJobByTransitionName(activityName, transitionName, agent)
    binding.setProperty('job',   job)
}
```

## Variable Usage Examples

### Item Operations

```groovy
// Get item properties
def itemName = item.getName()
def itemType = item.getType()
def itemState = item.getProperty('State')

// Access item outcomes
def lastViewpoint = item.getViewpoint('SchemaName', 'last')
def outcome = item.getOutcome(lastViewpoint)

// Check item collections
def hasDependency = item.checkDependency('DependencyName')
def members = item.getDependencyMembers('DependencyName')
```

### Agent Operations

```groovy
// Get agent information
def agentName = agent.getName()
def agentPath = agent.path

// Check agent permissions
def canExecute = agent.hasPermission('domain:action:target')

// Access agent properties
def role = agent.getProperty('Role')
```

### Job Operations

```groovy
// Get job information
def jobId = job.id
def activityName = job.activity.name
def transitionName = job.transition.name

// Access job outcomes
def jobOutcome = job.outcome
def schema = job.schema

// Get job properties
def propertyValue = job.getActPropString('PropertyName')
```

## Common Patterns

### Aggregate Script Pattern

From the standard `item_aggregate_groovy.tmpl`:

```groovy
def detailsSchema = 'ItemName_Details'

def name  = item.getName()
def state = item.getProperty('State')
def type  = item.getType()

Outcome details = null

if (item.checkViewpoint(detailsSchema, 'last')) {
  details = item.getOutcome(item.getViewpoint(detailsSchema, 'last'))
}

// Generate XML output
// ...

// Assign to output variable matching DSL declaration
ItemNameXML = writer.toString()
```

### Query List Script Pattern

From the standard `item_queryList_groovy.tmpl`:

```groovy
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.process.Gateway

def properties = [new Property('Type', 'ItemName'), new Property('State', 'ACTIVE')]
def result = Gateway.getLookup().search(new DomainPath(), properties, 0, 0)

ItemNameMap = [:]
for (DomainPath dp: result.rows) {
  ItemNameMap.put(dp.name, dp.itemPath.UUID)
}
```

### Validation Pattern

```groovy
// Validate item state
def currentState = item.getProperty('State')
if (currentState != 'ACTIVE') {
    errors = new ErrorInfo("Item must be in ACTIVE state")
    return
}

// Validate input parameters
def requiredField = inputParams.get('requiredField')
if (!requiredField) {
    errors = new ErrorInfo("requiredField is mandatory")
    return
}

// Validation passed
errors = null
result = processItem(item, inputParams)
```

## Variable Scope and Lifetime

### Script Execution Scope

- **Input variables**: Available throughout script execution
- **Binding variables**: Available throughout script execution
- **Output variables**: Must be assigned before script completes
- **Local variables**: Only available within their declaration scope

### Best Practices for Variable Usage

1. **Declare all dependencies**: Even if binding variables seem obvious, declare them as inputs
2. **Use descriptive names**: Binding variables have specific purposes - use them appropriately
3. **Don't overwrite binding variables**: Create local variables instead
4. **Initialize output variables**: Always assign output variables, even to `null`

## Error Handling with Binding Variables

### Common Error Patterns

```groovy
// Check for null item
def itemName = item?.getName()
if (!itemName) {
    errors = new ErrorInfo("Item is null")
    return
}

// Check agent permissions
def hasPermission = agent?.hasPermission('domain:action:target')
if (!hasPermission) {
    errors = new ErrorInfo("Agent lacks required permission")
    return
}

// Check job validity
def isValidJob = job?.outcome != null
if (!isValidJob) {
    errors = new ErrorInfo("Job has no outcome")
    return
}
```

### Error Object Creation

```groovy
// Simple error
errors = new ErrorInfo("Something went wrong")

// Error with details
errors = new ErrorInfo("Validation failed", "Field 'name' cannot be empty")

// No error
errors = null
```

## Debugging Binding Variables

### Check Variable Availability

```groovy
// Debug binding variables
log.debug("item: ${item}")
log.debug("agent: ${agent}")
log.debug("job: ${job}")

// Check input variables
log.debug("inputParam: ${inputParam}")
```

### Assert Variable Availability

```groovy
// Assert required variables exist
assert item != null, "item binding variable is null"
assert agent != null, "agent binding variable is null"
assert job != null, "job binding variable is null"
```

## MVEL Template Binding Variable Patterns

The standard MVEL templates use binding variables consistently:

### Aggregate Template Pattern

```groovy
// From item_aggregate_groovy.tmpl
def name  = item.getName()
def state = item.getProperty('State')
def type  = item.getType()

// Uses item binding variable exclusively
```

### Query Template Pattern

```groovy
// From item_queryList_groovy.tmpl
// Uses no binding variables - operates on search parameters
def properties = [new Property('Type', 'ItemName')]
def result = Gateway.getLookup().search(new DomainPath(), properties, 0, 0)
```

## See Also

- [Script Definition](script-definition.md) - DSL syntax and structure
- [Script Parameters](parameters.md) - Input/output parameter definitions
- [File References](file-references.md) - File path handling
- [Error Handling](error-handling.md) - Error handling patterns
