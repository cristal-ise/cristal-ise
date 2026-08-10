# Error Handling

**Error Handling** in CRISTAL-iSE Scripts manages execution errors, validation failures, and error reporting.

## Error Info Class

The primary error handling mechanism uses `org.cristalise.kernel.scripting.ErrorInfo`:

### ErrorInfo Constructors

```java
// Basic error
ErrorInfo(String message)

// Error with details
ErrorInfo(String message, String details)

// Error with cause
ErrorInfo(String message, Throwable cause)

// Complete error
ErrorInfo(String message, String details, Throwable cause)
```

## Standard Error Output Pattern

Most Scripts should include an error output parameter:

```groovy
Script('Patient_DataProcessor', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('data', 'java.util.Map')
  output('result', 'java.util.Map')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')  // Standard error output
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_DataProcessor.groovy')
}
```

## Error Handling Patterns

### Simple Error Return

```groovy
// In script code
def inputData = data.get('inputField')
if (!inputData) {
    errors = new ErrorInfo("Input field 'inputField' is required")
    return
}

// Success case
errors = null
result = processData(inputData)
```

### Validation with Multiple Errors

```groovy
def validationErrors = []

if (!data.get('requiredField')) {
    validationErrors.add("Field 'requiredField' is required")
}

if (data.get('email') && !isValidEmail(data.get('email'))) {
    validationErrors.add("Field 'email' has invalid format")
}

if (validationErrors) {
    errors = new ErrorInfo("Validation failed", validationErrors.join(", "))
    return
}

// Validation passed
errors = null
result = processValidData(data)
```

### Exception Handling

```groovy
try {
    def processedData = complexOperation(data)
    result = processedData
    errors = null
} catch (Exception e) {
    errors = new ErrorInfo("Processing failed", e)
    log.error("Error in DataProcessor", e)
}
```

### Null Checks

```groovy
// Safe navigation with null checks
def itemName = item?.getName()
if (!itemName) {
    errors = new ErrorInfo("Item or item name is null")
    return
}

// Null check for binding variables
if (!item || !agent || !job) {
    errors = new ErrorInfo("Required binding variables are missing")
    return
}
```

## Error Types and Usage

### Validation Errors

Use for input validation and business rule violations:

```groovy
if (value < 0) {
    errors = new ErrorInfo("Value must be non-negative")
    return
}
```

### Business Logic Errors

Use for business process failures:

```groovy
def canProcess = checkBusinessRules(item)
if (!canProcess) {
    errors = new ErrorInfo("Item cannot be processed in current state")
    return
}
```

### System Errors

Use for unexpected system failures:

```groovy
try {
    // Database operation
    def dbResult = databaseService.query(data)
    result = dbResult
    errors = null
} catch (SQLException e) {
    errors = new ErrorInfo("Database operation failed", e)
    log.error("Database error", e)
}
```

### Permission Errors

Use for authorization failures:

```groovy
def hasPermission = agent.hasPermission('domain:action:target')
if (!hasPermission) {
    errors = new ErrorInfo("Agent lacks permission for this operation")
    return
}
```

## Standard Error Patterns by Script Type

### Aggregate Scripts

```groovy
// From item_aggregate_groovy.tmpl pattern
try {
    def detailsSchema = 'ItemName_Details'
    def name = item.getName()
    def state = item.getProperty('State')
    
    if (!name) {
        throw new IllegalStateException("Item name is required")
    }
    
    // Generate XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)
    
    xml."$type" {
        Name(name)
        State(state)
    }
    
    ItemNameXML = writer.toString()
    errors = null
    
} catch (Exception e) {
    errors = new ErrorInfo("Failed to generate aggregate XML", e)
}
```

### Query Scripts

```groovy
try {
    def properties = [new Property('Type', 'ItemName'), new Property('State', 'ACTIVE')]
    def result = Gateway.getLookup().search(new DomainPath(), properties, 0, 0)
    
    ItemNameMap = [:]
    for (DomainPath dp: result.rows) {
        ItemNameMap.put(dp.name, dp.itemPath.UUID)
    }
    
    errors = null
    
} catch (Exception e) {
    errors = new ErrorInfo("Query failed", e)
}
```

### Activity Scripts

```groovy
try {
    // Validate inputs
    if (!item || !agent || !job) {
        errors = new ErrorInfo("Missing required binding variables")
        return
    }
    
    // Process the activity
    def outcome = job.outcome
    def newValue = outcome.getField('NewValue')
    
    if (!newValue) {
        errors = new ErrorInfo("NewValue field is required")
        return
    }
    
    // Update the item
    def result = updateItemProperty(item, 'PropertyName', newValue)
    
    if (!result.success) {
        errors = new ErrorInfo("Failed to update property", result.errorMessage)
        return
    }
    
    errors = null
    
} catch (Exception e) {
    errors = new ErrorInfo("Activity execution failed", e)
}
```

## Error Propagation

### From Included Scripts

When using `include()`, errors from included scripts need to be handled:

```groovy
// Main script
Script('Patient_MainProcessor', 0) {
  include('Patient_Validation', 0)
  include('Patient_Processor', 0)
  
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('result', 'java.util.Map')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/Patient_MainProcessor.groovy')
}
```

**Patient_MainProcessor.groovy:**
```groovy
// Call validator
def validationResult = validator.validate(item)
if (validationResult.errors) {
    errors = validationResult.errors
    return
}

// Call processor
result = processor.process(item)
errors = null
```

### Error Chaining

```groovy
try {
    def step1Result = performStep1(data)
    def step2Result = performStep2(step1Result)
    result = step2Result
    errors = null
} catch (Step1Exception e) {
    errors = new ErrorInfo("Step 1 failed", e)
} catch (Step2Exception e) {
    errors = new ErrorInfo("Step 2 failed", e)
} catch (Exception e) {
    errors = new ErrorInfo("Processing failed with unexpected error", e)
}
```

## Error Logging

### Best Practices for Error Logging

```groovy
@Field final Logger log = LoggerFactory.getLogger(this.class)

try {
    // Script logic
    result = complexOperation(data)
    errors = null
} catch (Exception e) {
    errors = new ErrorInfo("Operation failed", e)
    log.error("Error in ComplexScript: {}", e.message, e)
}
```

### Log Levels

- **DEBUG**: Detailed troubleshooting information
- **INFO**: Normal operational messages
- **WARN**: Non-fatal issues that should be investigated
- **ERROR**: Fatal errors that prevent script completion

## Error Testing

### Unit Test Error Scenarios

```groovy
// Test validation error
void testValidationError() {
    def script = new Script('Patient_Validation', 0, null, null)
    def result = script.execute([inputData: null])
    
    assert result.errors != null
    assert result.errors.message.contains("required")
}

// Test exception handling
void testExceptionHandling() {
    def script = new Script('Patient_Processor', 0, null, null)
    def result = script.execute([inputData: invalidData])
    
    assert result.errors != null
    assert result.errors.cause instanceof ExpectedException
}
```

### Manual Testing

```groovy
// Test script execution manually
def testFile = moduleDir + '/healthcare/patient/script/Patient_TestScript.groovy'
def scriptFile = new File(testFile)

def scriptContent = scriptFile.text
assert scriptContent != null, "Script file not found or not readable"

// Execute and check for errors
def script = ScriptBuilder.build('', 'Patient_TestScript', 0) { 
    input('data', 'java.util.Map')
    output('result', 'java.lang.String')
    output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
    script('groovy', testFile)
}

// Check that script was created successfully
assert script != null
assert script.scriptXML != null
```

## Error Handling in MVEL Templates

The standard MVEL templates include basic error handling patterns:

### Aggregate Template Error Handling

```groovy
// From item_aggregate_groovy.tmpl pattern
try {
    // Template logic
    // ...
    ItemNameXML = writer.toString()
    errors = null
} catch (Exception e) {
    errors = new ErrorInfo("Aggregate generation failed", e)
}
```

### Query Template Error Handling

```groovy
// From item_queryList_groovy.tmpl pattern
try {
    // Template logic
    // ...
    ItemNameMap = resultMap
    errors = null
} catch (Exception e) {
    errors = new ErrorInfo("Query execution failed", e)
}
```

## Common Error Messages

### Standard Error Messages

| Scenario | Error Message |
|----------|---------------|
| Missing required field | "Field '{fieldName}' is required" |
| Invalid format | "Field '{fieldName}' has invalid format" |
| Permission denied | "Agent lacks permission for this operation" |
| Item not found | "Item not found: {itemPath}" |
| Invalid state | "Item must be in {expectedState} state" |
| Processing failed | "Processing failed: {reason}" |

### Error Message Best Practices

1. **Be specific**: Include field names, expected values, actual values
2. **Be actionable**: Tell the user what to do to fix the error
3. **Be consistent**: Use standard error message patterns
4. **Include context**: Reference the operation or data that caused the error

## See Also

- [Script Definition](script-definition.md) - DSL syntax and structure
- [Script Parameters](parameters.md) - Input/output parameter definitions
- [Binding Variables](binding-variables.md) - Automatic binding variables
- [File References](file-references.md) - File path handling
