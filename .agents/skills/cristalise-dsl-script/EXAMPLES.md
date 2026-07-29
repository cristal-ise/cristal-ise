# CRISTAL-iSE Script DSL Examples

This file contains **complete, runnable examples** for all Script DSL patterns identified in the CRISTAL-iSE codebase.

## Table of Contents

- [Minimal Examples](#minimal-examples)
- [Standard Patterns](#standard-patterns)
- [Activity-Linked Scripts](#activity-linked-scripts)
- [Advanced Patterns](#advanced-patterns)
- [Error Handling Examples](#error-handling-examples)

---

## Minimal Example

### Hello World Script

**DSL Definition:**
```groovy
Script('HelloWorld', 0) {
  input('name', 'java.lang.String')
  output('greeting', 'java.lang.String')
  script('groovy', moduleDir+'/examples/scripts/HelloWorld.groovy')
}
```

**Script Code (HelloWorld.groovy):**
```groovy
package org.cristalise.examples.scripts

def greeting = "Hello, ${name}!"
```

---

## Standard Patterns

### Aggregate Script

**DSL Definition:**
```groovy
Script('ClubMember_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('ClubMemberXML', 'java.lang.String')
  script('groovy', moduleDir+'/devtest/clubMember/script/ClubMember_Aggregate.groovy')
}
```

**Script Code (ClubMember_Aggregate.groovy):**
```groovy
package org.cristalise.devtest.clubMember.script

import org.cristalise.kernel.persistency.outcome.Outcome

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.xml.MarkupBuilder
import groovy.transform.Field

@Field final Logger log = LoggerFactory.getLogger(this.class)

def detailsSchema = 'ClubMember_Details'

def name  = item.getName()
def state = item.getProperty('State')
def type  = item.getType()

Outcome details = null

if (item.checkViewpoint(detailsSchema, 'last')) {
  details = item.getOutcome(item.getViewpoint(detailsSchema, 'last'))
}

def writer = new StringWriter()
def xml = new MarkupBuilder(writer)

xml."$type" {
  Name(  name  )
  State( state )

  if (details) {
    details.getRecord().each {field, value ->
      if (field != 'Name') "$field"(value)
    }
  }
}

//check if this variable was defined as output
ClubMemberXML = writer.toString()
```

---

### Query List Script

**DSL Definition:**
```groovy
Script('ClubMember_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('ClubMemberMap', 'java.util.Map')
  script('groovy', moduleDir+'/devtest/clubMember/script/ClubMember_QueryList.groovy')
}
```

**Script Code (Patient_QueryList.groovy):**
```groovy
package org.cristalise.devtest.clubMember.script

import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.property.Property

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.Field

@Field final Logger log = LoggerFactory.getLogger(this.class)

def properties = [new Property('Type', 'ClubMember'), new Property('State', 'ACTIVE')]

def result = Gateway.getLookup().search(new DomainPath(), properties, 0, 0)
ClubMemberMap = [:]

for (DomainPath dp: result.rows) {
  ClubMemberMap.put(dp.name, dp.itemPath.UUID)
}

return ClubMemberMap
```

---

## Activity-Linked Scripts

### Change Name Script (From Dev Module)

**DSL Definition:**
```groovy
Script('CrudEntity_ChangeName', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
  input('job', 'org.cristalise.kernel.entity.Job')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  script('groovy', moduleDir+'/dev/script/CrudEntity_ChangeName.groovy')
}
```

**Activity Linkage:**

Scripts linked to Activities receive `item`, `agent`, and `job` as binding variables. Two syntax options are valid:

**Option 1: Variable Reference**
```groovy
Activity('Patient_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')
  
  Schema($patient_Details_Schema)
  Script($crudEntity_ChangeName_Script)
}
```

**Option 2: Direct Reference**
```groovy
Activity('Patient_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')
  
  Schema($patient_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
```

**Note**: Both patterns work. Variable reference (`$scriptVar`) requires the Script to be defined in the same module. Direct reference uses the Script name and version.

**Script Code (CrudEntity_ChangeName.groovy):**
```groovy
package org.cristalise.dev.script

import org.cristalise.kernel.lifecycle.instance.predefined.CreateAgentFromDescription
import org.cristalise.kernel.common.InvalidDataException

def name   = job.getOutcome().getField("Name")
def folder = job.getOutcome().getField("SubFolder")
def roles  = job.getOutcome().getField("InitialRoles")
def pwd    = job.getOutcome().getField("Password")

def root = job.getActPropString("Root")
if (root == null) root = item.getProperty("Root", null, null)

def domPath = (root != null ? root : "") + "/" + (folder != null ? folder : "")

// Create new Item
String[] params = [name, domPath, roles, pwd] as String[]

agent.execute(item, CreateAgentFromDescription, params)
errors = null
```

---

## Advanced Patterns

### Script with Multiple Includes

**DSL Definition:**
```groovy
Script('PatientRegistration', 0) {
  include('PatientValidation', 0)
  include('MedicalRecordUtils', 0)
  include('InsuranceProcessing', 0)
  
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('agent', 'org.cristalise.kernel.entity.proxy.AgentProxy')
  input('job', 'org.cristalise.kernel.entity.Job')
  input('registrationData', 'java.util.Map')
  output('patientId', 'java.lang.String')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/PatientRegistration.groovy')
}
```

---

### Script with Complex Parameters

**DSL Definition:**
```groovy
Script('DataTransformer', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('sourceData', 'java.util.Map')
  input('mappingRules', 'java.util.List')
  input('options', 'java.util.Map')
  output('transformedData', 'java.util.Map')
  output('stats', 'java.util.Map')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/examples/scripts/DataTransformer.groovy')
}
```

---

## Error Handling Examples

### Validation Script with Error Handling

**DSL Definition:**
```groovy
Script('PatientValidation', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('patientData', 'java.util.Map')
  output('isValid', 'boolean')
  output('validationErrors', 'java.util.List')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/healthcare/patient/script/PatientValidation.groovy')
}
```

**Script Code (PatientValidation.groovy):**
```groovy
package org.cristalise.healthcare.patient.script

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.transform.Field

@Field final Logger log = LoggerFactory.getLogger(this.class)

def validationErrors = []

// Required field validation
if (!patientData.get('firstName')) {
    validationErrors.add("First name is required")
}

if (!patientData.get('lastName')) {
    validationErrors.add("Last name is required")
}

if (!patientData.get('dateOfBirth')) {
    validationErrors.add("Date of birth is required")
} else {
    def dob = patientData.get('dateOfBirth')
    if (dob > new Date()) {
        validationErrors.add("Date of birth cannot be in the future")
    }
}

// Format validation
if (patientData.get('email') && !isValidEmail(patientData.get('email'))) {
    validationErrors.add("Email has invalid format")
}

if (patientData.get('phone') && !isValidPhone(patientData.get('phone'))) {
    validationErrors.add("Phone number has invalid format")
}

// Business rule validation
def existingPatient = findPatientByEmail(patientData.get('email'))
if (existingPatient && existingPatient.id != item?.id) {
    validationErrors.add("Patient with this email already exists")
}

// Set outputs
isValid = validationErrors.empty
validationErrors = validationErrors

if (isValid) {
    errors = null
} else {
    errors = new ErrorInfo("Validation failed", validationErrors.join(", "))
}

// Helper methods
def isValidEmail(String email) {
    email ==~ /^[^@]+@[^@]+\.[^@]+$/
}

def isValidPhone(String phone) {
    phone ==~ /^[\d\s\-\(\)\+]{10,20}$/
}

def findPatientByEmail(String email) {
    // Implementation omitted
    return null
}
```

---

### Exception Handling Script

**DSL Definition:**
```groovy
Script('DatabaseProcessor', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  input('query', 'java.lang.String')
  input('parameters', 'java.util.Map')
  output('queryResult', 'java.util.List')
  output('errors', 'org.cristalise.kernel.scripting.ErrorInfo')
  
  script('groovy', moduleDir+'/examples/scripts/DatabaseProcessor.groovy')
}
```

**Script Code (DatabaseProcessor.groovy):**
```groovy
package org.cristalise.examples.scripts

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.transform.Field
import java.sql.SQLException

@Field final Logger log = LoggerFactory.getLogger(this.class)

try {
    def connection = getDatabaseConnection()
    def preparedStatement = connection.prepareStatement(query)
    
    // Set parameters
    parameters.each { key, value ->
        preparedStatement.setObject(key as int + 1, value)
    }
    
    def resultSet = preparedStatement.executeQuery()
    
    // Process results
    queryResult = []
    while (resultSet.next()) {
        def row = [:]
        def metaData = resultSet.metaData
        for (int i = 1; i <= metaData.columnCount; i++) {
            row[metaData.getColumnName(i)] = resultSet.getObject(i)
        }
        queryResult.add(row)
    }
    
    // Clean up
    resultSet.close()
    preparedStatement.close()
    connection.close()
    
    errors = null
    
} catch (SQLException e) {
    errors = new ErrorInfo("Database operation failed", e)
    log.error("Database error in DatabaseProcessor", e)
} catch (Exception e) {
    errors = new ErrorInfo("Processing failed with unexpected error", e)
    log.error("Unexpected error in DatabaseProcessor", e)
}

// Helper method
def getDatabaseConnection() {
    // Implementation omitted
    return null
}
```

---

## Best Practices Demonstrated

These examples demonstrate the best practices established in the Script DSL:

1. **Use external file references** - All scripts reference external .groovy files
2. **Declare inputs and outputs** - Every script has proper parameter declarations
3. **Use moduleDir for portability** - File paths use moduleDir variable
4. **Include error outputs** - Most scripts include ErrorInfo output
5. **Follow naming conventions** - Script names match file names
6. **Use consistent package structure** - Scripts organized by domain
7. **Include dependencies** - Scripts that depend on others use include()

---

## See Also

- [SKILL.md](SKILL.md) - Main Script DSL reference
- [references/script-definition.md](references/script-definition.md) - Script definition details
- [references/parameters.md](references/parameters.md) - Parameter handling
- [references/file-references.md](references/file-references.md) - File reference patterns
- [references/includes.md](references/includes.md) - Script inclusion patterns
