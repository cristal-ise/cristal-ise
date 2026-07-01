# Script Definition

**Script Definition** refers to the DSL syntax for creating Script resources in CRISTAL-iSE.

## Script Constructor

The `Script` constructor creates a Script resource with the following signature:

```groovy
Script(String name, int version, @DelegatesTo(ScriptDelegate) Closure cl)
```

### Parameters

- **name**: String - The name of the Script resource
- **version**: int - The version number of the Script (typically starts at 0)
- **closure**: Closure - The DSL closure containing script configuration

## Basic Structure

```groovy
Script('ScriptName', 0) {
  // Parameter declarations
  input('param1', 'java.lang.String')
  output('result', 'java.lang.String')
  
  // Script file reference
  script('groovy', moduleDir+'/path/to/ScriptName.groovy')
}
```

## Script Builder Methods

The Script DSL is implemented through `ScriptDelegate` which provides the following methods:

### Core Methods

- `script(Map attrs, Closure cl)` - Generic script definition with attributes
- `script(String lang, String fileName)` - Script with file reference
- `script(String lang, Closure cl)` - Script with inline closure (less common)
- `javascript(Closure cl)` - Convenience method for JavaScript scripts
- `groovy(Closure cl)` - Convenience method for Groovy scripts  
- `jython(Closure cl)` - Convenience method for Jython (Python) scripts

### Parameter Methods

- `input(String name, String type)` - Define an input parameter
- `output(String type)` - Define an unnamed output
- `output(String name, String type)` - Define a named output parameter
- `param(String name, String type)` - Alias for input parameter

### Dependency Methods

- `include(String name, Integer version)` - Include another script by name and version
- `include(Script aScript)` - Include another script by Script object

## Standard Script Types

### Aggregate Scripts

Used for generating aggregated XML data from an Item. Follow the naming pattern `ItemName_Aggregate`:

```groovy
Script('Patient_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('PatientXML', 'java.lang.String')
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Aggregate.groovy')
}
```

### Query List Scripts

Used for querying and returning collections of data. Follow the naming pattern `ItemName_QueryList`:

```groovy
Script('Patient_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('PatientMap', 'java.util.Map')
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_QueryList.groovy')
}
```

### Custom Business Logic Scripts

Used for implementing custom business operations. Use descriptive names:

```groovy
Script('Patient_Update', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
  input('job', 'org.cristalise.kernel.entity.Job')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Update.groovy')
}
```

## Internal Implementation

The Script DSL internally:

1. Creates a `ScriptDelegate` instance to process the closure
2. Uses `MarkupBuilder` to generate cristalscript XML
3. Embeds script file contents as CDATA sections
4. Validates the generated XML against the Script schema
5. Creates a `Script` object with the generated XML

## See Also

- [Script Parameters](parameters.md) - Input/output parameter definitions
- [File References](file-references.md) - File path handling
- [Script Inclusion](includes.md) - Script reuse patterns
