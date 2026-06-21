# Structural Elements: struct, attribute, and field

These elements form the backbone of the CRISTAL-iSE Schema DSL, defining the XML structure and data types.

## struct
Defines an XML element which contains other elements (equivalent to `xs:complexType`).

| Property | Type | Purpose |
| :--- | :--- | :--- |
| `name` | String | Name of the XML element. Often the root element. |
| `documentation` | String | Defines `xs:documentation` within `xs:annotation`. |
| `useSequence` | Boolean | If `true`, generates `xs:sequence` (ordered); if `false` (default), generates `xs:all`. |
| `multiplicity` | String | Specifies `minOccurs` and `maxOccurs` (e.g., '0..1', '1..*'). |
| `closure` | Closure | Contains nested `struct`, `field`, `attribute`, or `dynamicForms`. |

---

## attribute
Defines an XML attribute of the parent element. Restricted to simple types and single values.

| Property | Type | Purpose |
| :--- | :--- | :--- |
| `name` | String | Name of the XML attribute. |
| `type` | String | Data type: `string`, `decimal`, `integer`, `boolean`, `date`, `time`, `dateTime`, `anyType`. |
| `multiplicity` | String | Can only be `1`, `0..1` or `1..1`. Determines if the attribute is required. |
| `values` | List | Provides accepted values for `xs:enumeration`. |
| `pattern` | String | Regex pattern to evaluate the value. |
| `default` | String | Default value for the attribute. |
| `range` | String | Defines limits using notation like `[0..10)`. Maps to `min/maxInclusive/Exclusive`. |
| `totalDigits` | Integer | Specifies the exact number of digits allowed (must be > 0). |
| `fractionDigits` | Integer | Specifies the maximum number of decimal places allowed (must be >= 0). |

---

## field
Defines an XML element within a `struct`. It inherits all properties from `attribute` and adds capabilities for complex multiplicity and nested logic.

| Property | Type | Purpose |
| :--- | :--- | :--- |
| `name` | String | Name of the XML element. |
| `type` | String | Data type (same as attribute). |
| `multiplicity` | String | Supports `1`, `0..1`, `1..*`, `*`, `0..*`, or fixed numbers (e.g., `5`). |
| `values` | List | Static list of allowed values (Enum). |
| `pattern` | String | Regex for validation. |
| `default` | String | Default value. |
| `range` | String | Numeric limits (e.g., `[0..100]`). |
| `totalDigits` | Integer | Total digits allowed. |
| `fractionDigits` | Integer | Decimal places allowed. |
| `closure` | Closure | Includes nested `attribute`, `unit`, `listOfValues`, `dynamicForms`, `warning`, `expression`, or `reference`. |

---

## reference
Defines a reference to another Item or a collection.

| Property | Type | Purpose |
| :--- | :--- | :--- |
| `itemType` | String | The type of the referenced Item. |
| `collectionName` | String | (Optional) The name of the collection if referencing a collection. |
