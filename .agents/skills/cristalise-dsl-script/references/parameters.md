# Script Parameters

**Script Parameters** define the input and output contracts for Script resources in CRISTAL-iSE.

## Input Parameters

Input parameters declare the variables that will be available to the script during execution.

### Syntax

```groovy
input(String name, String type)
```

### Standard Input Types

| Type | Description | Usage |
|------|-------------|-------|
| `org.cristalise.kernel.entity.proxy.ItemProxy` | The current Item being processed | Required for most scripts |
| `org.cristalise.kernel.entity.proxy.AgentProxy` | The Agent executing the script | Required for activity scripts |
| `org.cristalise.kernel.entity.Job` | The Job context | Required for activity scripts |
| `java.lang.String` | String input parameter | Custom inputs |
| `java.util.Map` | Map input parameter | Complex data structures |
| `java.util.List` | List input parameter | Collections |

### Example

```groovy
Script('Patient_DataProcessor', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
  input('job', 'org.cristalise.kernel.entity.Job')
  input('configData', 'java.util.Map')
  input('userInput', 'java.lang.String')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_DataProcessor.groovy')
}
```

## Output Parameters

Output parameters declare the variables that the script will produce and make available to the calling context.

### Syntax

```groovy
// Named output
output(String name, String type)

// Unnamed output (less common)
output(String type)
```

### Standard Output Types

| Type | Description | Usage |
|------|-------------|-------|
| `java.lang.String` | String output | General purpose |
| `java.util.Map` | Map output | Query results, collections |
| `org.cristalise.kernel.scripting.ErrorInfo` | Error information | Error handling |

### Example

```groovy
Script('Patient_GenerateReport', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  
  output('reportXML', 'java.lang.String')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_GenerateReport.groovy')
}
```

## Parameter Contract

The parameter contract is the agreement between the Script definition and the actual script code:

1. **Input parameters** become available as variables in the script
2. **Output parameter names** must match the variable names assigned in the script code
3. **Type declarations** enable runtime type checking and documentation

### Contract Example

**DSL Definition:**
```groovy
Script('Patient_CalculateTotal', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('totalAmount', 'java.math.BigDecimal')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_CalculateTotal.groovy')
}
```

**Script Code (Patient_CalculateTotal.groovy):**
```groovy
// Input variables are automatically available
def itemData = item.getOutcome(...)

// Output variables must match DSL output names
totalAmount = calculateTotal(itemData)
errors = null  // No errors
```

## Param Method

The `param` method is an alias for `input` and can be used interchangeably:

```groovy
Script('Patient_Example', 0) {
  param('param1', 'java.lang.String')  // Same as input('param1', 'java.lang.String')
  input('param2', 'java.lang.Integer')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Example.groovy')
}
```

## Best Practices

1. **Always declare all inputs** - Even if they seem obvious
2. **Use specific types** - Prefer `java.util.Map` over generic `Object`
3. **Document error outputs** - Always include error output for validation scripts
4. **Match variable names** - Ensure script code uses exact output names from DSL
5. **Order inputs logically** - Group related inputs together

## Common Patterns

### Aggregate Script Pattern

```groovy
Script('Patient_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('PatientXML', 'java.lang.String')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Aggregate.groovy')
}
```

### Query Script Pattern

```groovy
Script('Patient_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('PatientMap', 'java.util.Map')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_QueryList.groovy')
}
```

### Activity Script Pattern

```groovy
Script('Patient_Update', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
  input('job', 'org.cristalise.kernel.entity.Job')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Update.groovy')
}
```

## See Also

- [Script Definition](script-definition.md) - DSL syntax and structure
- [File References](file-references.md) - File path handling
- [Binding Variables](binding-variables.md) - Automatic binding variables
