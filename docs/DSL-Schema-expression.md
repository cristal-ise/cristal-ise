# Field: Expression 
Defines an expression to compute the value of the field

| Property | Type (default) | Description |
| -------- | -------------- | ----------- |
| name | String | the name of the generated Script item |
| version | Integer | the version of the generated Script item  |
| inputFields | List<String> | list of field names used to comput ethe value |
| imports | List<String> | list of imported classes required to compile/execute the expression |
| loggerName | String | e.g.: org.cristalise.template.Script.Patient.ComputeAgeUpdateExpression |
| expression | String | the actual expression (currently only groovy is supported) |

**Example Schema:**
```groovy
Schema('Patient_Details', 0) {
  struct(name: 'Patient_Details') {
    field(name: 'DateOfBirth', type: 'date')
    field(name: 'Age', type: 'integer') {
    expression(
      name: 'Patient_DetailsComputeAgeUpdateExpression',
      version: 10,
      imports: ['java.time.Period', 'java.time.LocalDate'],
      inputFields: ['DateOfBirth'],
      loggerName : 'org.cristalise.test.Script.Patient.ComputeAgeUpdateExpression',
      expression: 'Period.between(DateOfBirth, LocalDate.now()).getYears()'
    )
  }
}
```

**Generated groooy script:**
```groovy
import static org.cristalise.kernel.persistency.outcomebuilder.utils.OutcomeUtils.getValueOrNull;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.utils.LocalObjectLoader
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.xml.MarkupBuilder

import java.time.Period
import java.time.LocalDate

final Logger log = LoggerFactory.getLogger("org.cristalise.test.Script.Patient.DetailsAgeUpdateExpression")

def getAge(JSONObject json) {
    def builder = new OutcomeBuilder(LocalObjectLoader.getSchema('TestItemExcel_Details', 0))
    def DateOfBirth = getValueOrNull(json, 'DateOfBirth', builder)

    if (DateOfBirth != null) {
        //expression comes here, use Outcome, OutcomeBuilder, OutcomeUtils, ItemProxy and other utility classes
        Period.between(DateOfBirth, LocalDate.now()).getYears()
    }
    else {
        return null
    }
}

if (!TestItemExcel_Details) throw new InvalidDataException('Undefined inputs TestItemExcel_Details for script TestItemExcel_DetailsAgeUpdateExpression')

JSONObject jsonInput = (JSONObject)TestItemExcel_Details
log.debug 'TestItemExcel_Details:{}', jsonInput

StringWriter writer = new StringWriter()
MarkupBuilder xml = new MarkupBuilder(writer)

// returned XML shall only contain fields that is updated by the script
xml.TestItemExcel_Details {
    def AgeValue = getAge(jsonInput)

    if (AgeValue != null) {
        Age(AgeValue)
    }
    else {
        Age()
    }
}

TestItemExcel_DetailsXml = writer.toString()

log.debug('returning TestItemExcel_DetailsXml:{}', TestItemExcel_DetailsXml)
```

**Generated XSD:**
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
                  <updateScriptRef>TestItemExcel_DetailsAgeUpdateExpression:0</updateScriptRef>
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
