/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.persistency.outcome.generator

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.cristalise.dsl.persistency.outcome.Attribute
import org.cristalise.dsl.persistency.outcome.Field
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.property.BuiltInItemProperties
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.property.PropertyUtility

/**
 * Generator that transforms a Struct AST into an @ng-forge/dynamic-forms compatible configuration structure.
 */
@Slf4j @CompileStatic
class NgForgeConfigGenerator implements SchemaGenerator<Map<String, Object>> {

    @Override
    Map<String, Object> generate(Struct s, SchemaContext context) {
        if (!s) throw new InvalidDataException("Schema cannot be built from empty declaration")

        Map<String, Object> formConfig = new LinkedHashMap<>()
        formConfig['id'] = (context?.name ?: s.name) ?: ''
        formConfig['title'] = s.dynamicForms?.label ?: s.name
        if (s.documentation) {
            formConfig['description'] = s.documentation
        }

        if (s.dynamicForms?.width) {
            formConfig['width'] = s.dynamicForms.width
        }
        if (s.dynamicForms?.container) {
            formConfig['container'] = s.dynamicForms.container
        }

        formConfig['fields'] = buildFieldList(s)

        return formConfig
    }

    /**
     * Convenience method to generate ng-forge configuration as a JSON String.
     */
    String generateJson(Struct s, SchemaContext context = null, boolean pretty = true) {
        Map<String, Object> map = generate(s, context ?: new SchemaContext(s.name, 0))
        String json = JsonOutput.toJson(map)
        return pretty ? JsonOutput.prettyPrint(json) : json
    }

    private List<Map<String, Object>> buildFieldList(Struct s) {
        List<Map<String, Object>> fields = new ArrayList<>()

        s.orderOfElements.each { String name ->
            if (s.fields.containsKey(name)) {
                fields.add(buildFieldConfig(s.fields[name]))
            }
            else if (s.structs.containsKey(name)) {
                fields.add(buildStructConfig(s.structs[name]))
            }
        }

        if (s.attributes) {
            s.attributes.each { Attribute a ->
                fields.add(buildAttributeConfig(a))
            }
        }

        return fields
    }

    private Map<String, Object> buildStructConfig(Struct s) {
        Map<String, Object> groupConfig = new LinkedHashMap<>()
        groupConfig['key'] = s.name
        groupConfig['type'] = isMultiple(s.multiplicityString, s.maxOccurs) ? 'array' : 'group'
        groupConfig['label'] = s.dynamicForms?.label ?: s.name

        if (s.documentation) {
            groupConfig['description'] = s.documentation
        }

        Map<String, Object> props = new LinkedHashMap<>()
        if (s.dynamicForms?.container) props['container'] = s.dynamicForms.container
        if (s.dynamicForms?.width) props['width'] = s.dynamicForms.width
        if (s.dynamicForms?.hidden != null) props['hidden'] = s.dynamicForms.hidden
        if (!props.isEmpty()) {
            groupConfig['props'] = props
        }

        groupConfig['fields'] = buildFieldList(s)

        return groupConfig
    }

    private Map<String, Object> buildFieldConfig(Field f) {
        Map<String, Object> fieldConfig = new LinkedHashMap<>()
        fieldConfig['key'] = f.name
        fieldConfig['type'] = determineControlType(f)
        fieldConfig['label'] = f.dynamicForms?.label ?: f.name

        if (f.dynamicForms?.placeholder) {
            fieldConfig['placeholder'] = f.dynamicForms.placeholder
        }

        if (f.dynamicForms?.value != null || f.defaultVal != null) {
            fieldConfig['defaultValue'] = f.dynamicForms?.value ?: f.defaultVal
        }

        if (f.dynamicForms?.disabled != null) {
            fieldConfig['disabled'] = f.dynamicForms.disabled
        }
        if (f.dynamicForms?.hidden != null) {
            fieldConfig['hidden'] = f.dynamicForms.hidden
        }

        List<Map<String, Object>> options = extractOptions(f)
        if (options != null && !options.isEmpty()) {
            fieldConfig['options'] = options
        }

        Map<String, Object> validation = buildValidation(f)
        if (!validation.isEmpty()) {
            fieldConfig['validation'] = validation
        }

        Map<String, Object> props = buildProps(f)
        if (!props.isEmpty()) {
            fieldConfig['props'] = props
        }

        return fieldConfig
    }

    private Map<String, Object> buildAttributeConfig(Attribute a) {
        Map<String, Object> attrConfig = new LinkedHashMap<>()
        attrConfig['key'] = a.name
        attrConfig['type'] = 'input'
        attrConfig['label'] = a.name
        if (a.defaultVal != null) {
            attrConfig['defaultValue'] = a.defaultVal
        }
        if (a.values) {
            attrConfig['type'] = 'select'
            attrConfig['options'] = a.values.collect { [label: it, value: it] }
        }

        Map<String, Object> validation = new LinkedHashMap<>()
        if (a.required) validation['required'] = true
        if (a.pattern) validation['pattern'] = a.pattern
        if (!validation.isEmpty()) {
            attrConfig['validation'] = validation
        }

        attrConfig['props'] = [isAttribute: true]
        return attrConfig
    }

    private String determineControlType(Field f) {
        if (f.dynamicForms?.type) {
            String t = f.dynamicForms.type.toLowerCase()
            switch (t) {
                case 'select':
                case 'textarea':
                case 'radio':
                case 'checkbox':
                case 'password':
                    return t
                case 'date':
                    return 'datepicker'
                default:
                    return t
            }
        }

        if (f.dynamicForms?.inputType) {
            String it = f.dynamicForms.inputType.toLowerCase()
            if (it == 'password') return 'password'
            if (it == 'file') return 'file'
        }

        if (f.values || f.listOfValues) {
            return 'select'
        }

        String rawType = f.type ?: 'string'
        if (rawType.startsWith('xs:')) {
            rawType = rawType.substring(3)
        }
        String fieldType = rawType.toLowerCase()
        switch (fieldType) {
            case 'boolean':
                return 'checkbox'
            case 'integer':
            case 'int':
            case 'long':
            case 'short':
            case 'byte':
            case 'decimal':
            case 'float':
            case 'double':
            case 'number':
                return 'number'
            case 'date':
            case 'datetime':
            case 'time':
                return 'datepicker'
            default:
                return 'input'
        }
    }

    private List<Map<String, Object>> extractOptions(Field f) {
        if (f.values) {
            return f.values.collect { [label: it, value: it] as Map<String, Object> }
        }
        if (f.listOfValues?.values) {
            return f.listOfValues.values.collect { [label: it, value: it] as Map<String, Object> }
        }
        return null
    }

    private Map<String, Object> buildValidation(Field f) {
        Map<String, Object> validation = new LinkedHashMap<>()

        if (f.isRequired() || f.dynamicForms?.required == true) {
            validation['required'] = true
        }

        String pattern = f.pattern ?: f.dynamicForms?.pattern
        if (pattern) {
            validation['pattern'] = pattern
        }

        Object min = f.dynamicForms?.min ?: f.minInclusive ?: f.minExclusive
        if (min != null) {
            validation['min'] = min
        }

        Object max = f.dynamicForms?.max ?: f.maxInclusive ?: f.maxExclusive
        if (max != null) {
            validation['max'] = max
        }

        Object minLength = f.minLength ?: f.length
        if (minLength != null) {
            validation['minLength'] = minLength
        }

        Object maxLength = f.maxLength ?: f.length
        if (maxLength != null) {
            validation['maxLength'] = maxLength
        }

        if (f.dynamicForms?.warning) {
            Map<String, Object> warnMap = new LinkedHashMap<>()
            if (f.dynamicForms.warning.pattern) warnMap['pattern'] = f.dynamicForms.warning.pattern
            if (f.dynamicForms.warning.message) warnMap['message'] = f.dynamicForms.warning.message
            if (f.dynamicForms.warning.expression) warnMap['expression'] = f.dynamicForms.warning.expression
            validation['warning'] = warnMap
        }

        return validation
    }

    private Map<String, Object> buildProps(Field f) {
        Map<String, Object> props = new LinkedHashMap<>()

        if (f.dynamicForms) {
            def df = f.dynamicForms
            if (df.container) props['container'] = df.container
            if (df.control) props['control'] = df.control
            if (df.labelGrid) props['labelGrid'] = df.labelGrid
            if (df.width) props['width'] = df.width
            if (df.mask) props['mask'] = df.mask
            if (df.autoComplete) props['autoComplete'] = df.autoComplete
            if (df.showSeconds != null) props['showSeconds'] = df.showSeconds
            if (df.hideOnDateTimeSelect != null) props['hideOnDateTimeSelect'] = df.hideOnDateTimeSelect
            if (df.precision) props['precision'] = df.precision
            if (df.scale) props['scale'] = df.scale
            if (df.htmlAccept) props['accept'] = df.htmlAccept
            if (df.updateFields) props['updateFields'] = df.updateFields
            if (df.updateScriptRef != null) props['updateScriptRef'] = df.getUpdateScriptRefString()
            if (df.updateQuerytRef != null) props['updateQueryRef'] = df.getUpdateQueryRefString()
            if (df.additional?.fields) props['additional'] = df.additional.fields
        }

        if (f.listOfValues) {
            Map<String, Object> lovMap = new LinkedHashMap<>()
            if (f.listOfValues.scriptRef) lovMap['scriptRef'] = f.listOfValues.getScriptRefString()
            if (f.listOfValues.queryRef) lovMap['queryRef'] = f.listOfValues.getQueryRefString()
            if (f.listOfValues.propertyNames) lovMap['propertyNames'] = f.listOfValues.propertyNames
            if (f.listOfValues.inputName) lovMap['inputName'] = f.listOfValues.inputName
            props['listOfValues'] = lovMap
        }

        if (f.reference) {
            Map<String, Object> refMap = new LinkedHashMap<>()
            refMap['itemType'] = extractItemType(f.reference.itemType)
            if (f.reference.collectionName) refMap['collectionName'] = f.reference.collectionName
            props['reference'] = refMap
        }

        if (f.unit) {
            Map<String, Object> unitMap = new LinkedHashMap<>()
            if (f.unit.values) unitMap['values'] = f.unit.values
            if (f.unit.defaultVal) unitMap['default'] = f.unit.defaultVal
            props['unit'] = unitMap
        }

        if (f.expression) {
            Map<String, Object> exprMap = new LinkedHashMap<>()
            if (f.expression.name) exprMap['name'] = f.expression.name
            if (f.expression.expression) exprMap['expression'] = f.expression.expression
            if (f.expression.inputFields) exprMap['inputFields'] = f.expression.inputFields
            props['expression'] = exprMap
        }

        return props
    }

    private boolean isMultiple(String multiplicityString, String maxOccurs) {
        if (maxOccurs == 'unbounded') return true
        if (multiplicityString && multiplicityString.contains('*')) return true
        if (maxOccurs) {
            try {
                return Integer.parseInt(maxOccurs) > 1
            } catch (Exception ignored) {}
        }
        return false
    }

    private String extractItemType(Object itemTypeObj) {
        if (itemTypeObj instanceof String) return (String) itemTypeObj
        if (itemTypeObj instanceof PropertyDescriptionList) {
            PropertyDescriptionList propDesc = (PropertyDescriptionList) itemTypeObj
            return PropertyUtility.getDefaultValue(propDesc.list, BuiltInItemProperties.TYPE.getName())
        }
        return itemTypeObj?.toString() ?: ''
    }
}
