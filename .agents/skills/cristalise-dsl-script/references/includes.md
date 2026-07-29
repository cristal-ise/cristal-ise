# Script Inclusion

**Script Inclusion** enables code reuse by allowing one Script resource to include dependencies on other Script resources.

## Include Methods

The Script DSL provides two ways to include dependent scripts:

### By Name and Version

```groovy
include(String name, Integer version)
```

### By Script Object

```groovy
include(org.cristalise.kernel.scripting.Script aScript)
```

## Usage Examples

### Basic Include

```groovy
Script('PatientData_Process', 0) {
  include('HealthcareUtils', 0)
  
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('result', 'java.lang.String')
  
  script('groovy', moduleDir+'/healthcare/patient/script/PatientData_Process.groovy')
}
```

### Multiple Includes

```groovy
Script('Patient_Processor', 0) {
  include('Patient_Validation', 0)
  include('DataTransformers', 0)
  include('ErrorHandlers', 0)
  
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('data', 'java.util.Map')
  output('processedData', 'java.util.Map')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Processor.groovy')
}
```

### Include with Script Object Reference

```groovy
// First define or reference the script to include
def utilitiesScript = new Script('HealthcareUtils', 0, null, null)

Script('Patient_CustomScript', 0) {
  include(utilitiesScript)
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_CustomScript.groovy')
}
```

## How Includes Work

### Internal Processing

When a Script includes another Script:

1. **Dependency declaration**: The `include()` method adds a reference to the included Script
2. **XML generation**: The ScriptDelegate generates `<include name="ScriptName" version="0"/>` elements
3. **Dependency resolution**: At runtime, CRISTAL-iSE resolves and loads the included scripts
4. **Execution context**: Included scripts become available in the execution context

### Generated XML Example

For a Script that includes another:

```xml
<cristalscript>
  <include name="CommonUtilities" version="0"/>
  <script language="groovy" name="ComplexProcessing">
    <![CDATA[
    // Script content
    ]]>
  </script>
</cristalscript>
```

## Include Patterns

### Utility Library Pattern

Create a dedicated utility Script that can be included by multiple other Scripts:

**Utility Script:**
```groovy
Script('HealthcareUtils', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('utils', 'java.util.Map')
  
  script('groovy', moduleDir+'/healthcare/script/HealthcareUtils.groovy')
}
```

**Consumer Scripts:**
```groovy
Script('Patient_ScriptA', 0) {
  include('HealthcareUtils', 0)
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_ScriptA.groovy')
}

Script('Patient_ScriptB', 0) {
  include('HealthcareUtils', 0)
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_ScriptB.groovy')
}
```

### Validation Library Pattern

Create a shared validation Script:

```groovy
Script('Patient_Validation', 0) {
  input('data', 'java.util.Map')
  output('isValid', 'boolean')
  output('validationErrors', 'java.util.List')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Validation.groovy')
}

Script('Patient_DataProcessor', 0) {
  include('Patient_Validation', 0)
  
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('inputData', 'java.util.Map')
  output('result', 'java.util.Map')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_DataProcessor.groovy')
}
```

### Modular Script Architecture

Organize scripts into layers with clear dependencies:

```groovy
// Low-level utilities
Script('Healthcare_StringUtils', 0) {
  script('groovy', moduleDir+'/healthcare/script/utils/Healthcare_StringUtils.groovy')
}

Script('Healthcare_DateUtils', 0) {
  include('Healthcare_StringUtils', 0)
  script('groovy', moduleDir+'/healthcare/script/utils/Healthcare_DateUtils.groovy')
}

// Business logic
Script('Healthcare_DataProcessor', 0) {
  include('Healthcare_StringUtils', 0)
  include('Healthcare_DateUtils', 0)
  script('groovy', moduleDir+'/healthcare/script/Healthcare_DataProcessor.groovy')
}

// High-level operations
Script('Healthcare_Workflow', 0) {
  include('Healthcare_DataProcessor', 0)
  script('groovy', moduleDir+'/healthcare/script/Healthcare_Workflow.groovy')
}
```

## Include Best Practices

### Version Management

1. **Explicit versions**: Always specify version numbers for included scripts
2. **Version consistency**: Ensure included script versions are compatible
3. **Version updates**: Test thoroughly when updating included script versions

### Dependency Organization

1. **Minimize includes**: Only include scripts that are actually needed
2. **Avoid circular dependencies**: Script A includes Script B, Script B includes Script A
3. **Document dependencies**: Use clear naming to indicate what each script provides

### Include Order

- **Order doesn't matter** for include statements - CRISTAL-iSE resolves dependencies
- **Order matters for readability** - Group related includes together
- **Alphabetical order** can improve maintainability for many includes

## Dependency Resolution

### Runtime Behavior

1. **Automatic loading**: CRISTAL-iSE automatically loads included scripts when needed
2. **Caching**: Scripts are cached after first load for performance
3. **Error handling**: Missing included scripts cause execution errors

### Error Scenarios

**Missing Script:**
```
Error: Script 'NonExistentScript' version 0 not found
```

**Version Mismatch:**
```
Error: Script 'SomeScript' version 5 not found (available: version 0, 1, 2)
```

## MVEL Template Integration

The MVEL templates automatically generate include statements for standard utility scripts:

```groovy
// From item_groovy.tmpl
Script('CrudEntity_ChangeName', 0) {
  // No includes - this is a standalone script
  script('groovy', moduleDir+'/dev/script/CrudEntity_ChangeName.groovy')
}
```

For custom templates, includes can be added to share common functionality:

```groovy
Script('Patient_CustomScript', 0) {
  include('HealthcareUtils', 0)
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_CustomScript.groovy')
}
```

## Debugging Includes

### Verify Include Resolution

```groovy
// In script code, you can access included script functionality
def result = utils.someUtilityFunction()  // utils comes from included script
```

### Check Dependencies

```groovy
// Test that included scripts are available
def includedScript = ScriptBuilder.getScript('HealthcareUtils', 0)
assert includedScript != null, "Required script 'HealthcareUtils' not found"
```

## See Also

- [Script Definition](script-definition.md) - DSL syntax and structure
- [Script Parameters](parameters.md) - Input/output parameter definitions
- [File References](file-references.md) - File path handling
- [Binding Variables](binding-variables.md) - Automatic binding variables
