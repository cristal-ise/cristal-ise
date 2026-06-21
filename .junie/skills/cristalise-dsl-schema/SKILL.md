---
name: cristalise-dsl-schema
description: CRISTAL-iSE Schema DSL guidelines. Use when developing or modifying CRISTAL-iSE Schemas using the Groovy-based Domain Specific Language (DSL) to ensure fluent and valid construct creation.
---

# CRISTAL-iSE Schema DSL

This skill encodes the standards for defining CRISTAL-iSE Schemas using the Groovy DSL. It focuses on maintainability, UI integration, and correctness of the generated XSD.

## MUST DO

- **Always provide a unique name and version** in the `Schema` function.
- **Use `struct` for complex types** and **`field` or `attribute` for simple types**.
- **Set `useSequence: true` in `struct`** when the order of child elements matters (generates `xs:sequence`).
- **Provide `documentation`** for all major elements to help future developers and users.
- **Use `listOfValues`** to link fields to dynamic data sources (Queries or Scripts).
- **Implement validation** using `warning` with either regex `pattern` or JavaScript `expression`.
- **Use `expression`** to define derived fields that are automatically computed based on other field values.
- **Keep `dynamicForms` configuration** consistent with the project's layout standards (e.g., `ui-g-12` for full width).

## MUST NOT DO

- **Do not define `field` at the top level** of a Schema; always wrap it in a `struct`.
- **Do not use `attribute` for new Schema**, only when explicitly instructed.
- **Do not hardcode static values in validation scripts** if they can be defined via `values` or `listOfValues`.
- **Avoid complex logic inside `warning` expressions**; if logic is reused, consider a server-side script.

## Reference Guide

| Topic | Description | File |
|---|---|---|
| **Structural Elements** | Detailed properties for `struct`, `attribute`, and `field`. | `references/structural-elements.md` |
| **Expressions** | How to define computed fields, injected variables, and generated scripts. | `references/expressions.md` |
| **UI Customization** | Using `dynamicForms` for layout, labels, and widget types. | `references/ui-customization.md` |
| **Data Types** | Supported types and multiplicity formats. | `references/data-types.md` |

## Examples

### Basic Employee Record
```groovy
Schema('Employee', 1) {
    struct(name: 'Employee', documentation: 'Core employee record', useSequence: true) {
        field(name: 'ID', type: 'integer')
        field(name: 'FullName', type: 'string') {
            dynamicForms(label: 'Full Name', container: 'ui-g-6')
        }
        field(name: 'Department', type: 'string') {
            listOfValues(queryRef: 'GetDepartments:0')
        }
    }
}
```

### Validation and Computed Fields
```groovy
field(name: 'Age', type: 'integer') {
    dynamicForms(disabled: true) // Computed, so user shouldn't edit
    expression(
        imports: ['java.time.Period', 'java.time.LocalDate'],
        inputFields: ['BirthDate'],
        expression: 'Period.between(BirthDate, LocalDate.now()).getYears()'
    )
    warning(expression: 'element.value >= 18', message: 'Employee must be at least 18 years old')
}
```

## Setup & Verification

1. **Verify availability** of `org.cristalise.dsl.persistency.outcome.SchemaBuilder`.
2. **Test generation** using `org.cristalise.dsl.test.builders.SchemaTestBuilder` in a Spock specification.
3. **Inspect output** `sb.schema.schemaData` to ensure the generated XSD matches expectations.
