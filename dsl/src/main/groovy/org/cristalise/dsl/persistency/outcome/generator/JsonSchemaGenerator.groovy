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
 * Generator that transforms a Struct AST into a standard JSON Schema (Draft 2020-12) representation.
 */
@Slf4j @CompileStatic
class JsonSchemaGenerator implements SchemaGenerator<Map<String, Object>> {

    public static final String JSON_SCHEMA_DRAFT_2020_12 = "https://json-schema.org/draft/2020-12/schema"

    @Override
    Map<String, Object> generate(Struct s, SchemaContext context) {
        if (!s) throw new InvalidDataException("Schema cannot be built from empty declaration")

        Map<String, Object> root = new LinkedHashMap<>()
        root['$schema'] = JSON_SCHEMA_DRAFT_2020_12
        root['title'] = (context?.name ?: s.name) ?: ''

        if (s.documentation) {
            root['description'] = s.documentation
        }

        populateStructSchema(root, s)

        return root
    }

    /**
     * Convenience method to generate JSON Schema as a JSON String.
     */
    String generateJson(Struct s, SchemaContext context = null, boolean pretty = true) {
        Map<String, Object> map = generate(s, context ?: new SchemaContext(s.name, 0))
        String json = JsonOutput.toJson(map)
        return pretty ? JsonOutput.prettyPrint(json) : json
    }

    private void populateStructSchema(Map<String, Object> schemaMap, Struct s) {
        schemaMap['type'] = 'object'

        Map<String, Object> properties = new LinkedHashMap<>()
        List<String> requiredList = new ArrayList<>()

        s.orderOfElements.each { String name ->
            if (s.fields.containsKey(name)) {
                Field f = s.fields[name]
                properties[name] = buildFieldSchema(f)
                if (isFieldRequired(f)) {
                    requiredList.add(name)
                }
            }
            else if (s.structs.containsKey(name)) {
                Struct childStruct = s.structs[name]
                properties[name] = buildStructProperty(childStruct)
                if (isStructRequired(childStruct)) {
                    requiredList.add(name)
                }
            }
        }

        if (s.attributes) {
            s.attributes.each { Attribute a ->
                properties[a.name] = buildAttributeSchema(a)
                if (a.required) {
                    requiredList.add(a.name)
                }
            }
        }

        schemaMap['properties'] = properties

        if (!requiredList.isEmpty()) {
            schemaMap['required'] = requiredList
        }

        if (s.anyField != null) {
            schemaMap['additionalProperties'] = true
        }
        else {
            schemaMap['additionalProperties'] = false
        }

        if (s.dynamicForms) {
            schemaMap['x-cristal-dynamicForms'] = buildDynamicFormsMap(s.dynamicForms)
        }
    }

    private Map<String, Object> buildStructProperty(Struct s) {
        boolean isArray = isMultiple(s.multiplicityString, s.maxOccurs)

        Map<String, Object> structSchema = new LinkedHashMap<>()
        if (s.documentation) {
            structSchema['description'] = s.documentation
        }
        populateStructSchema(structSchema, s)

        if (isArray) {
            Map<String, Object> arraySchema = new LinkedHashMap<>()
            arraySchema['type'] = 'array'
            if (s.documentation) {
                arraySchema['description'] = s.documentation
            }
            arraySchema['items'] = structSchema

            if (s.minOccurs && s.minOccurs != '0') {
                try {
                    arraySchema['minItems'] = Integer.parseInt(s.minOccurs)
                } catch (Exception ignored) {}
            }
            if (s.maxOccurs && s.maxOccurs != 'unbounded') {
                try {
                    arraySchema['maxItems'] = Integer.parseInt(s.maxOccurs)
                } catch (Exception ignored) {}
            }
            return arraySchema
        }

        return structSchema
    }

    private Map<String, Object> buildFieldSchema(Field f) {
        boolean isArray = isMultiple(f.multiplicityString, f.maxOccurs)

        Map<String, Object> baseSchema = new LinkedHashMap<>()
        populateTypeAndRestrictions(baseSchema, f)

        if (f.documentation) {
            baseSchema['description'] = f.documentation
        }
        if (f.defaultVal != null) {
            baseSchema['default'] = coerceDefault(f.defaultVal, f.type)
        }

        if (f.unit) {
            Map<String, Object> unitMap = new LinkedHashMap<>()
            if (f.unit.values) unitMap['values'] = f.unit.values
            if (f.unit.defaultVal) unitMap['default'] = f.unit.defaultVal
            baseSchema['x-cristal-unit'] = unitMap
        }

        if (f.reference) {
            Map<String, Object> refMap = new LinkedHashMap<>()
            refMap['itemType'] = extractItemType(f.reference.itemType)
            if (f.reference.collectionName) refMap['collectionName'] = f.reference.collectionName
            baseSchema['x-cristal-reference'] = refMap
        }

        if (f.listOfValues) {
            Map<String, Object> lovMap = new LinkedHashMap<>()
            if (f.listOfValues.scriptRef) lovMap['scriptRef'] = f.listOfValues.getScriptRefString()
            if (f.listOfValues.queryRef) lovMap['queryRef'] = f.listOfValues.getQueryRefString()
            if (f.listOfValues.propertyNames) lovMap['propertyNames'] = f.listOfValues.propertyNames
            if (f.listOfValues.inputName) lovMap['inputName'] = f.listOfValues.inputName
            if (f.listOfValues.values) lovMap['values'] = f.listOfValues.values
            baseSchema['x-cristal-listOfValues'] = lovMap
        }

        if (f.dynamicForms) {
            baseSchema['x-cristal-dynamicForms'] = buildDynamicFormsMap(f.dynamicForms)
        }

        if (f.expression) {
            Map<String, Object> exprMap = new LinkedHashMap<>()
            if (f.expression.name) exprMap['name'] = f.expression.name
            if (f.expression.expression) exprMap['expression'] = f.expression.expression
            if (f.expression.inputFields) exprMap['inputFields'] = f.expression.inputFields
            baseSchema['x-cristal-expression'] = exprMap
        }

        if (isArray) {
            Map<String, Object> arraySchema = new LinkedHashMap<>()
            arraySchema['type'] = 'array'
            if (f.documentation) {
                arraySchema['description'] = f.documentation
            }
            arraySchema['items'] = baseSchema

            if (f.minOccurs && f.minOccurs != '0') {
                try {
                    arraySchema['minItems'] = Integer.parseInt(f.minOccurs)
                } catch (Exception ignored) {}
            }
            if (f.maxOccurs && f.maxOccurs != 'unbounded') {
                try {
                    arraySchema['maxItems'] = Integer.parseInt(f.maxOccurs)
                } catch (Exception ignored) {}
            }
            return arraySchema
        }

        return baseSchema
    }

    private Map<String, Object> buildAttributeSchema(Attribute a) {
        Map<String, Object> schema = new LinkedHashMap<>()
        populateTypeAndRestrictions(schema, a)
        if (a.defaultVal != null) {
            schema['default'] = coerceDefault(a.defaultVal, a.type)
        }
        schema['x-cristal-attribute'] = true
        return schema
    }

    private void populateTypeAndRestrictions(Map<String, Object> schema, Attribute a) {
        String rawType = a.type ?: 'string'
        if (rawType.startsWith('xs:')) {
            rawType = rawType.substring(3)
        }
        String type = rawType.toLowerCase()

        switch (type) {
            case 'integer':
            case 'int':
            case 'long':
            case 'short':
            case 'byte':
            case 'nonnegativeinteger':
            case 'positiveinteger':
            case 'nonpositiveinteger':
            case 'negativeinteger':
                schema['type'] = 'integer'
                break
            case 'decimal':
            case 'float':
            case 'double':
            case 'number':
                schema['type'] = 'number'
                break
            case 'boolean':
                schema['type'] = 'boolean'
                break
            case 'date':
                schema['type'] = 'string'
                schema['format'] = 'date'
                break
            case 'time':
                schema['type'] = 'string'
                schema['format'] = 'time'
                break
            case 'datetime':
                schema['type'] = 'string'
                schema['format'] = 'date-time'
                break
            case 'anytype':
            case 'any':
                schema['type'] = 'object'
                break
            default:
                schema['type'] = 'string'
                break
        }

        if (a.values) {
            schema['enum'] = a.values
        }

        if (a.pattern) {
            schema['pattern'] = a.pattern
        }

        if (a.minInclusive != null) {
            schema['minimum'] = parseNumber(a.minInclusive)
        }
        if (a.maxInclusive != null) {
            schema['maximum'] = parseNumber(a.maxInclusive)
        }
        if (a.minExclusive != null) {
            schema['exclusiveMinimum'] = parseNumber(a.minExclusive)
        }
        if (a.maxExclusive != null) {
            schema['exclusiveMaximum'] = parseNumber(a.maxExclusive)
        }

        if (a.length != null) {
            schema['minLength'] = a.length
            schema['maxLength'] = a.length
        }
        if (a.minLength != null) {
            schema['minLength'] = a.minLength
        }
        if (a.maxLength != null) {
            schema['maxLength'] = a.maxLength
        }
    }

    private Map<String, Object> buildDynamicFormsMap(org.cristalise.dsl.persistency.outcome.DynamicForms df) {
        Map<String, Object> dfMap = new LinkedHashMap<>()
        if (df.label != null) dfMap['label'] = df.label
        if (df.placeholder != null) dfMap['placeholder'] = df.placeholder
        if (df.hidden != null) dfMap['hidden'] = df.hidden
        if (df.required != null) dfMap['required'] = df.required
        if (df.disabled != null) dfMap['disabled'] = df.disabled
        if (df.multiple != null) dfMap['multiple'] = df.multiple
        if (df.type != null) dfMap['type'] = df.type
        if (df.inputType != null) dfMap['inputType'] = df.inputType
        if (df.min != null) dfMap['min'] = df.min
        if (df.max != null) dfMap['max'] = df.max
        if (df.value != null) dfMap['value'] = df.value
        if (df.mask != null) dfMap['mask'] = df.mask
        if (df.autoComplete != null) dfMap['autoComplete'] = df.autoComplete
        if (df.pattern != null) dfMap['pattern'] = df.pattern
        if (df.errmsg != null) dfMap['errmsg'] = df.errmsg
        if (df.showSeconds != null) dfMap['showSeconds'] = df.showSeconds
        if (df.container != null) dfMap['container'] = df.container
        if (df.control != null) dfMap['control'] = df.control
        if (df.labelGrid != null) dfMap['labelGrid'] = df.labelGrid
        if (df.width != null) dfMap['width'] = df.width
        if (df.precision != null) dfMap['precision'] = df.precision
        if (df.scale != null) dfMap['scale'] = df.scale
        if (df.updateFields != null) dfMap['updateFields'] = df.updateFields
        if (df.updateScriptRef != null) dfMap['updateScriptRef'] = df.getUpdateScriptRefString()
        if (df.updateQuerytRef != null) dfMap['updateQueryRef'] = df.getUpdateQueryRefString()
        if (df.warning != null) {
            Map<String, Object> warnMap = new LinkedHashMap<>()
            if (df.warning.pattern) warnMap['pattern'] = df.warning.pattern
            if (df.warning.message) warnMap['message'] = df.warning.message
            if (df.warning.expression) warnMap['expression'] = df.warning.expression
            dfMap['warning'] = warnMap
        }
        if (df.additional?.fields) {
            dfMap['additional'] = df.additional.fields
        }
        return dfMap
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

    private boolean isFieldRequired(Field f) {
        return f.isRequired()
    }

    private boolean isStructRequired(Struct s) {
        return s.minOccurs == null || s.minOccurs != '0'
    }

    private Object coerceDefault(Object val, String type) {
        if (val == null) return null
        if (val instanceof Number || val instanceof Boolean) return val
        String str = val.toString().trim()
        String t = type?.toLowerCase() ?: 'string'
        if (t == 'boolean') return Boolean.parseBoolean(str)
        if (t in ['integer', 'int', 'long', 'short', 'byte']) {
            try { return Long.parseLong(str) } catch (Exception ignored) { return val }
        }
        if (t in ['decimal', 'float', 'double', 'number']) {
            try { return Double.parseDouble(str) } catch (Exception ignored) { return val }
        }
        return val
    }

    private Number parseNumber(Object val) {
        if (val == null) return null
        if (val instanceof Number) return (Number) val
        String str = val.toString().trim()
        try {
            if (str.contains('.')) return Double.parseDouble(str)
            return Long.parseLong(str)
        } catch (Exception ignored) {
            return null
        }
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
