# CRISTAL-iSE Schema DSL Examples

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

### Complex UI Layout
```groovy
struct(name: 'ContactInfo', useSequence: true) {
    dynamicForms(label: 'Contact Information', container: 'ui-g-12')
    field(name: 'Email', type: 'string') {
        dynamicForms(label: 'Email Address', container: 'ui-g-6')
        warning(pattern: '^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$', message: 'Invalid email format')
    }
    field(name: 'Phone', type: 'string') {
        dynamicForms(label: 'Phone Number', container: 'ui-g-6', mask: '(999) 999-9999')
    }
}
```
