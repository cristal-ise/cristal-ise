# Field: Expression 
Defines an expression to compute the value of the given field. It contains all information to perform 2 actions

1. generate the Script (UpdateScript only) to compute the value
1. update the input fields with the UpdateScriptRef - see [field dynamicForms](../DSL-Schema#field-dynamicforms)

The genearted script contains variables named after the inputFields which means that expression can use them to perform the required actions. The expression will only be triggered if all mandatory fields have valid values (i.e. not null). The script is based on the OutcomeUtils, a generic utility class of the framework to help script development. 

| Property      | Type (default)          | Description |
| ------------- | ----------------------- | ----------- |
| name          | String (optional)       | ovwewrites the generated name (i.e. '\<schemaName\>\<fieldName\>UpdateExpression' |
| version       | Integer (optional)      | ovwewrites the version of Schema  |
| inputFields   | List<String>            | list of field names used to comput ethe value |
| imports       | List<String>            | list of imported classes required to compile/execute the expression |
| loggerPrefix  | String (optional)       | the application package name, e.g. org.cristalise.template |
| loggerName    | String (optional)       | ovwewrites the generated loggerName from the name and loggerPrefix |
| expression    | String                  | the actual expression (currently only groovy is supported) |
| compileStatic | boolean (default: true) | set it to false when the expression requires dynamic groovy |

## Injected variable available to the expression

| Property       | Type (default)    | Description |
| -------------- | ------------------| ----------- |
| \<InputField\> | Type of the Field | variable(s) created from the 'inputFields' |
| item           | ItemProxy         | the actual Item for which the UpdateScript is executed |
| agent          | AgentProxy        | the user executing the Activity |
| schema         | Schema            | the Schema used to generate the form |
| builder        | OutcomeBuilder    | the builder initilaied with the Schema. |

## Limitations

- inputField can only reference fields in the same level
- only UpdateScript is generated. Generating SaveScript could be implemented as well.
- only groovy is supported

## Example
The age of the patient is computed from the DataOfBirth and from the DateOfDeath fields, where the DateOfDeath is optional (the patient is still alive). In case the patient is still alive the expression uses the current date (LocalDate.now()) for the calculation. The expression is based in the [Elvis operator of groovy](http://groovy-lang.org/operators.html#_elvis_operator).

### Schema DSL

- only the mandatory fields are specified in this example

```groovy
Schema('Patient_Details', 0) {
  struct(name: 'Patient_Details') {
    field(name: 'DateOfBirth', type: 'date')
    field(name: 'DateOfDeath', type: 'date', multiplicity: '0..1')
    field(name: 'Age', type: 'integer') {
    expression(
      imports: ['java.time.Period', 'java.time.LocalDate'],
      inputFields: ['DateOfBirth, DateOfDeath'],
      expression: 'Period.between(DateOfBirth, DateOfDeath ?: LocalDate.now()).getYears()'
    )
  }
}
```

### Generated XSD

- Both input fields, DateOfBirth and DateOfDeath, have the dynamicForms/additional/updateScriptRef assigned 
- The generated script name is Patient_DetailsAgeUpdateExpression with version 0

```xml
<?xml version='1.0' encoding='utf-8'?>
<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>
  <xs:element name='Patient_Details'>
    <xs:complexType>
      <xs:sequence>
        <xs:element name='DateOfBirth' type='xs:date' minOccurs='1' maxOccurs='1'>
          <xs:annotation>
            <xs:appinfo>
              <dynamicForms>
                <additional>
                  <updateScriptRef>Patient_DetailsAgeUpdateExpression:0</updateScriptRef>
                </additional>
              </dynamicForms>
            </xs:appinfo>
          </xs:annotation>
        </xs:element>
        <xs:element name='DateOfDeath' type='xs:date' minOccurs='0' maxOccurs='1'>
          <xs:annotation>
            <xs:appinfo>
              <dynamicForms>
                <additional>
                  <updateScriptRef>Patient_DetailsAgeUpdateExpression:0</updateScriptRef>
                </additional>
              </dynamicForms>
            </xs:appinfo>
          </xs:annotation>
        </xs:element>
        <xs:element name='Age' type='xs:integer' minOccurs='1' maxOccurs='1' />
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>
```

### Generated Updatescript

- The `getAge(json)` method is generated to execute the 'expression'.
- The 'expression' is only executed if the DateOfBirth is not null, because DateOfDeath is NOT mandatory. 
- Without 'loggerPrefix' the generated loggerName is Script.Patient.DetailsAgeUpdateExpression
- All the static methods of OutcomeUtils were imported

```groovy
import static org.cristalise.kernel.persistency.outcomebuilder.utils.OutcomeUtils.*

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.utils.LocalObjectLoader
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

import java.time.Period
import java.time.LocalDate

final Logger log = LoggerFactory.getLogger("Script.Patient.DetailsAgeUpdateExpression")

@CompileStatic
def getAge(JSONObject json) {
    def schema = LocalObjectLoader.getSchema('Patient_Details', 0)
    def builder = new OutcomeBuilder(schema)

    def DateOfBirth = getLocalDateOrNull(json, 'DateOfBirth')
    def DateOfDeath = getLocalDateOrNull(json, 'DateOfDeath')

    if (DateOfBirth != null) {
        //expression comes here, use Outcome, OutcomeBuilder, OutcomeUtils, ItemProxy and other utility classes
        Period.between(DateOfBirth, DateOfDeath ?: LocalDate.now()).getYears()
    }
    else {
        return null
    }
}

if (!Patient_Details) throw new InvalidDataException('Undefined inputs Patient_Details for script org.cristalise.dsl.test.Patient.DetailsAgeUpdateExpression')

JSONObject jsonInput = (JSONObject)Patient_Details
log.debug 'Patient_Details:{}', jsonInput

StringWriter writer = new StringWriter()
MarkupBuilder xml = new MarkupBuilder(writer)

// returned XML shall only contain fields that is updated by the script
xml.Patient_Details {
    def AgeValue = getAge(jsonInput)

    if (AgeValue != null) {
        Age(AgeValue)
    }
    else {
        Age()
    }
}

Patient_DetailsXml = writer.toString()

log.debug('returning Patient_DetailsXml:{}', Patient_DetailsXml)
```
