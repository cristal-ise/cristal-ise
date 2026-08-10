# File References

**File References** handle how Script DSL references external script files containing the actual executable code.

## File-Based Script Approach

CRISTAL-iSE Script DSL uses **external file references** rather than inline code. This approach provides:

- **Better maintainability**: Script code can be edited independently
- **Improved debugging**: Full IDE support for external files
- **Version control**: Script files can be tracked separately
- **Reusability**: Same script file can be referenced by multiple Script resources

## Syntax

```groovy
script(String language, String filePath)
```

### Parameters

- **language**: String - The scripting language (`'groovy'`, `'javascript'`, `'jython'`)
- **filePath**: String - Path to the script file, typically using `moduleDir`

## Using moduleDir

The `moduleDir` variable provides the base directory of the current module, enabling **portable file references**.

### Absolute vs Relative Paths

**❌ Do NOT use absolute paths:**
```groovy
script('groovy', '/absolute/path/to/script/ProcessData.groovy')  // Bad - not portable
```

**✅ Use moduleDir for relative paths:**
```groovy
script('groovy', moduleDir+'/scripts/ProcessData.groovy')  // Good - portable
```

## Path Patterns

### Standard Module Structure

For a module `healthcare` with package `org.cristalise.healthcare`:

```
module-root/
├── src/
│   └── main/
│       └── module/
│           └── org/
│               └── cristalise/
│                   └── healthcare/
│                       ├── Healthcare.groovy       # DSL definitions
│                       └── script/
│                           ├── Patient_Aggregate.groovy
│                           ├── Patient_QueryList.groovy
│                           └── CommonUtilities.groovy
```

### File Reference Examples

```groovy
// Simple script in examples module
Script('SimpleScript', 0) {
  script('groovy', moduleDir+'/examples/scripts/SimpleScript.groovy')
}

// Script in healthcare module
Script('Patient_Aggregate', 0) {
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_Aggregate.groovy')
}

// Script with full package path
Script('ComplexProcessing', 0) {
  script('groovy', moduleDir+'/org/cristalise/healthcare/patient/script/ComplexProcessing.groovy')
}
```

## Language Specifications

### Groovy Scripts (Recommended)

```groovy
Script('MyScript', 0) {
  script('groovy', moduleDir+'/examples/scripts/MyScript.groovy')
}
```

Groovy is the **recommended language** for CRISTAL-iSE scripts because:
- Full integration with CRISTAL-iSE Java APIs
- Better IDE support
- Rich type system
- Mature ecosystem

### JavaScript Scripts

```groovy
Script('JsScript', 0) {
  script('javascript', moduleDir+'/scripts/JsScript.js')
}
```

JavaScript is supported via JSR-223 and is useful for:
- Simple expressions and calculations
- Web-related scripting
- JSON processing

### Jython Scripts

```groovy
Script('PythonScript', 0) {
  script('jython', moduleDir+'/scripts/PythonScript.py')
}
```

Jython (Python) is supported via JSR-223 for:
- Python-based logic
- Integration with Python libraries
- Data science applications

## File Content Handling

The Script DSL **only uses external file references**. The actual script code lives in separate `.groovy` (or `.js`, `.py`) files.

**Internal Implementation Note**: ScriptDelegate reads the file content and embeds it as CDATA in the generated XML, but this is transparent to users. You only need to specify the file path.

This means:
- ✅ Script files can contain any valid Groovy/JavaScript/Python code
- ✅ Special characters (`<`, `>`, `&`, etc.) are handled correctly via CDATA
- ✅ Files must be readable at runtime
- ✅ File paths must resolve correctly from `moduleDir`

## File Organization Best Practices

### Directory Structure

```
module/
├── src/
│   └── main/
│       └── module/
│           ├── org/
│           │   └── cristalise/
│           │       └── healthcare/     # Module package
│           │           ├── Healthcare.groovy  # DSL definitions
│           │           └── script/
│           │               ├── patient/
│           │               │   ├── Patient_Aggregate.groovy
│           │               │   ├── Patient_QueryList.groovy
│           │               │   └── Patient_Validation.groovy
│           │               └── utils/
│           │                   └── HealthcareUtils.groovy
```

### Naming Conventions

- **Script files**: `ScriptName.groovy` (matching the Script resource name)
- **Aggregate scripts**: `ItemName_Aggregate.groovy` (e.g., `Patient_Aggregate.groovy`)
- **Query scripts**: `ItemName_QueryList.groovy` (e.g., `Patient_QueryList.groovy`)
- **Validation scripts**: `ItemName_Validation.groovy` (e.g., `Patient_Validation.groovy`)
- **Utility scripts**: `UtilityName.groovy` (e.g., `HealthcareUtils.groovy`)

### File Header Template

```groovy
/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 */
package org.cristalise.module.package.script

// Script implementation
```

## Error Handling for File References

### Common Issues

1. **File not found**: Check path is correct and file exists
2. **Permission denied**: Ensure file is readable by the process
3. **Invalid content**: Verify file contains valid script code
4. **Encoding issues**: Use UTF-8 encoding for script files

### Debugging Tips

```groovy
// Test file accessibility
def testFile = new File(moduleDir+'/scripts/Test.groovy')
assert testFile.exists(), "Script file not found: ${testFile.absolutePath}"
assert testFile.canRead(), "Script file not readable: ${testFile.absolutePath}"
```

## MVEL Template Integration

CRISTAL-iSE uses **MVEL templates** to generate standard script patterns. The templates use `@{variable}` syntax and automatically create the correct file paths and script structures.

### Template Examples

The `item_aggregate_groovy.tmpl` template generates:

```groovy
Script('@{item.name}_Aggregate', @{version}) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('@{item.name}XML', 'java.lang.String')
  script('groovy', moduleDir+'/@{rootPackage}.@{itemPackage}/script/@{item.name}_Aggregate.groovy')
}
```

The `item_queryList_groovy.tmpl` template generates:

```groovy
Script('@{item.name}_QueryList', @{version}) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('@{item.name}Map', 'java.util.Map')
  script('groovy', moduleDir+'/@{rootPackage}.@{itemPackage}/script/@{item.name}_QueryList.groovy')
}
```

This ensures **consistent file paths** and **standard naming conventions** across generated scripts.

## See Also

- [Script Definition](script-definition.md) - DSL syntax and structure
- [Script Parameters](parameters.md) - Input/output parameter definitions
- [Script Inclusion](includes.md) - Script reuse patterns
