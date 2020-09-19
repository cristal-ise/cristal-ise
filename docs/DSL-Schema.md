# Schema
Defines the XML Schema which is stored in description item of type `Schema`. In priciple it is equivalent of an xsd file.

Creating an XSD with few fields (elements) is relatively easy but as number or the complexity of elements grows the readability of the XSD becoming a problem. The CRISTAL-iSE DSL based on Groovy scripting and Excel was developed to address this challenge.

**Comprehensive Example: Structural and Funtional elements**
```groovy
Schema('test', 'Employee', 0) {
  struct(name: 'Employee', documentation: 'Employee data', useSequence: true) {
    dynamicForms(width: '50%')
    field(name: 'Name', type: 'string') {
      dynamicForms(label: 'UserName', updateFields:['Title','Department']) {
        warning (pattern: '^[A-Za-z]{1,12}$', message: 'Can only contain letters, maximum 12')
      }
    }
    field(name: 'Title', type: 'string', values: ['Mr','Mrs','Dr'])
    field(name: 'Department', type: 'string') {
      listOfValues(scriptRef: 'Department_QueryList:0')
    }
    field(name: 'DateOfHire', type: 'date', multiplicity: '0..1') {
      dynamicForms {
        warning (expression: "var m = moment(element.value, 'YYYY-MM-DD'); m.isValid();", message: 'Has to be in YYYY-MM-DD format')
      }
    }
  }
}
```

**Comprehensive Example: Field Groups for layout**
```groovy
def purchaseOrderDetails = Schema(updateSchemaName, 0) {
    struct(name: updateSchemaName, documentation: 'Defines PurchaseOrder entries', useSequence: true) {
        struct(name: 'Addresses', useSequence: true, multiplicity: '0..1') {
            field(name: 'BillingAddress',       type: 'string'){
                dynamicForms(type: 'SELECT', disabled: true, container: 'ui-g-12',  labelGrid: 'ui-g-2', control: 'ui-g-10')
            }
            field(name: 'ShippingAddress',      type: 'string'){
                dynamicForms(type: 'SELECT',  disabled: true)
            }
            dynamicForms(container: 'ui-g-6', label: 'Group Title Here')
        }
        field(name: 'CustomerReferences',    type: 'string'){dynamicForms(label: 'Customer PO #')}
        dynamicForms(container: 'ui-g-12', width: '81%')
    }
}
```

* namespace(optional) - normally the namespace of the module which is added to the DomainPath
* name - name of the Schema Item
* version - version of the Schema Item
* closure - includes the following elements to define the actual structure
    * [struct](#struct)

## struct
Defines an xml element which contains other elements (i.e. xs:complexType)

* `name` - name of the element, it is often the root element
* `documentation` - defines xs:documentation within the xs:annotation
* `useSequence` - whether the xs:complexType is a xs:sequence or xs:all (default)
* `multiplicity` - specifies the minOccurs and maxOccurs xsd elements
* `closure` - includes the following elements to define the actual content of the element
    * [struct](#struct)
    * [dynamicForms](#struct-dynamicForms)
    * [attribute](#attribute)
    * [field](#field) 

### *struct* dynamicforms
Provides customization capabilities for WebUI.

* `label` - the title of the group (panel)
* `width` - for the top-level struct it specifies the width of the form. The width can be in % or in px or whatever HTML allows. See 'Field Groups' example above
* `container`: defines the [Grid CSS Class](https://www.primefaces.org/showcase/ui/panel/grid.xhtml) for the whole group (panel)

### *struct* attribute
Defines an xs:attribute of the xml element. it can be used for building struct and field

* `multiplicity` - can only be 1, 0..1 or 1..1. calculates the `required` flag of attribute
* `values` - provides accepted values for the xs:enumeration
* `type` - the schema element type. It could be one of the following: string, decimal, integer, boolean. date, time
* `pattern` - regex pattern to evaluate the value
* `default` - default value
* `range` - example: `[0..10)` - defines values for xs:minExclusive, xs:minInclusive, xs:maxExclusive, xs:maxInclusive
* `totalDigits` - Specifies the exact number of digits allowed. Must be greater than zero
* `fractionDigits` - Specifies the maximum number of decimal places allowed. Must be equal to or greater than zero

## **field**
Defines element within the struct. Inherits all functionalities of [attribute](#attribute)

* `multiplicity` - extends attribute to provides values for minOccurs and maxOccurs and to accept xs:maxOccurs greater than 1 or unbounded
* `closure` - includes the following elements to define extra functionality
  * [attribute](#attribute)
  * [unit](#field-unit)
  * [listOfValues](#field-listOfValues)
  * [dynamicForms](#field-dynamicForms)
  * [warning](#field-warning)
  * [expression](#field-expression)

### *field* unit
Defines an xs:attribute called `unit` within the element defined by field

* `values` - provides values for the xs:enumeration of the unit
* `default` - default value of the xs:attribute

### *field* listOfValues
Field argument used to get the label-value pair for the combobox ui widget

| Property | Type | Description |
| -------- | ---- | ----------- |
| scriptRef | String or Script | Defines the Script or the name and version of the refernced Script (e.g. GetShiftNames:0') |
| queryRef | String or Query | Defines the Query or the name and version of the refernced Query (e.g. QueryShiftNames:0') |
| propertyNames| String | Comma separated list of ItemProperty names |
| inputName| String | The name of the input variable |
| values | List<String> | Use these values as html option |

### *field* dynamicForms
Provides customization capabilities for WebUI

| Property | Type | Description |
| -------- | ---- | ----------- |
| hidden | Boolean | Makes the field visible or not |
| required | Boolean | Makes the field mandatory if true |
| disabled | Boolean | Field is disabled |
| multiple | Boolean | Allows multiple values or entries |
| grid     | String| Custom grid for grouping of fields |
| label | String | The custom label to be used for the field |
| type | String | A textfield, textarea, radio button etc |
| inputType | String | Examples are text and password|
| min | Integer | The minimum value |
| max | Integer | The maximum value |
| mask | String | String compatible with the Mask input component of primeng |
| value | String | Default value |
| updateScriptRef | String or Script | Defines the Script or the name and version of the refernced Script (e.g. GetShiftNames:0') which is executed when the form generated from the XML Schema has to be updated |
| updateQueryRef | String or Query | Defines the Query or the name and version of the refernced Query (e.g. QueryShiftNames:0') which is executed when the from generated from the XML Schema has to be updated |
| updateFields | List<String> | List the fields to update when this field is updated |
| container | **Default**: `ui-g-12` | Defines the [Grid CSS Class](https://www.primefaces.org/showcase/ui/panel/grid.xhtml) for the whole field container that contains the label and the control. It defines how many columns of the 12 columns of the struct (panel or form) are taken up by this field. For example `ui-g-6` means half of the width of the struct (panel or form) is allocated to this field container. |
| labelGrid / control | **Default**: `labelGrid: ui-g-4` / `control: ui-g-8` | `labelGrid` defines the [Grid CSS Class](https://www.primefaces.org/showcase/ui/panel/grid.xhtml) for the label, `control` defines it for the control, inside the container (container contains 12 columns, so `control + labelGrid = 12`). Both `control` and `labelGrid` have to be specified to be effective. |

### *field* warning
Defines acceptable limits in the field, A warning message is deplayed on the field, but the value can be saved, the field is still valid.  
The warning is shown if the field does not match the **pattern** or if the **expression** evaluates to false.

| Property | Type | Description |
| -------- | ---- | ----------- |
| pattern | String | Regex pattern to be tested in webui (e.g. `'^[0-9]{1,4}$'`) |
| expression | String | Javascript condition to be evaluated in webui (e.g. `'element.value < 5000'`). The values of other fields can be referenced in the following way: (e.g. `'element.value <= fieldValue("AllocatedQuantity")`) |
| message | String | Warning message if the pattern doesn't match or expression evaluates to false |

### *field* expression
