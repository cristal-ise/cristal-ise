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
package org.cristalise.dsl.test.persistency.outcome

import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.persistency.outcome.generator.JsonSchemaGenerator
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals

class JsonSchemaGeneratorSpecs extends Specification implements CristalTestSetup {

    def 'Generates standard JSON Schema draft 2020-12 root structure'() {
        when:
        def sb = SchemaBuilder.build('TestModule', 'Person', 1) {
            struct(name: 'Person', documentation: 'Person record') {
                field(name: 'name', type: 'string')
                field(name: 'age', type: 'integer')
            }
        }
        String actualJson = sb.toJsonSchema()

        then:
        assertJsonEquals('''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Person",
          "description": "Person record",
          "type": "object",
          "properties": {
            "name": {
              "type": "string"
            },
            "age": {
              "type": "integer"
            },
            "PredefinedSteps": {
              "type": "object",
              "x-cristal-dynamicForms": {
                "hidden": true,
                "required": false
              }
            }
          },
          "required": ["name", "age"],
          "additionalProperties": false
        }
        ''', actualJson)
    }

    def 'Maps data types accurately to JSON Schema types'() {
        when:
        def sb = SchemaBuilder.build('Test', 'DataTypes', 0) {
            struct(name: 'DataTypes') {
                field(name: 'stringField', type: 'string')
                field(name: 'intField', type: 'integer')
                field(name: 'decimalField', type: 'decimal')
                field(name: 'boolField', type: 'boolean')
                field(name: 'dateField', type: 'date')
                field(name: 'timeField', type: 'time')
                field(name: 'dateTimeField', type: 'dateTime')
                field(name: 'anyField', type: 'anyType')
            }
        }
        String actualJson = sb.toJsonSchema()

        then:
        assertJsonEquals('''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "DataTypes",
          "type": "object",
          "properties": {
            "stringField": { "type": "string" },
            "intField": { "type": "integer" },
            "decimalField": { "type": "number" },
            "boolField": { "type": "boolean" },
            "dateField": { "type": "string", "format": "date" },
            "timeField": { "type": "string", "format": "time" },
            "dateTimeField": { "type": "string", "format": "date-time" },
            "anyField": { "type": "object" },
            "PredefinedSteps": {
              "type": "object",
              "x-cristal-dynamicForms": {
                "hidden": true,
                "required": false
              }
            }
          },
          "required": [
            "stringField", "intField", "decimalField", "boolField",
            "dateField", "timeField", "dateTimeField", "anyField"
          ],
          "additionalProperties": false
        }
        ''', actualJson)
    }

    def 'Maps field multiplicity and array wrappers correctly'() {
        when:
        def sb = SchemaBuilder.build('Test', 'Multiplicity', 0) {
            struct(name: 'Multiplicity', useSequence: true) {
                field(name: 'singleField', type: 'string')
                field(name: 'optionalField', type: 'string', multiplicity: '0..1')
                field(name: 'arrayField', type: 'string', multiplicity: '0..*')
                field(name: 'boundedArray', type: 'integer', multiplicity: '2..5')
            }
        }
        String actualJson = sb.toJsonSchema()

        then:
        assertJsonEquals('''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Multiplicity",
          "type": "object",
          "properties": {
            "singleField": {
              "type": "string"
            },
            "optionalField": {
              "type": "string"
            },
            "arrayField": {
              "type": "array",
              "items": {
                "type": "string"
              }
            },
            "boundedArray": {
              "type": "array",
              "items": {
                "type": "integer"
              },
              "minItems": 2,
              "maxItems": 5
            },
            "PredefinedSteps": {
              "type": "object",
              "x-cristal-dynamicForms": {
                "hidden": true,
                "required": false
              }
            }
          },
          "required": ["singleField", "boundedArray"],
          "additionalProperties": false
        }
        ''', actualJson)
    }

    def 'Maps restrictions (enum, pattern, min/max, minLength/maxLength) to JSON Schema'() {
        when:
        def sb = SchemaBuilder.build('Test', 'Constraints', 0) {
            struct(name: 'Constraints') {
                field(name: 'status', type: 'string', values: ['OPEN', 'IN_PROGRESS', 'CLOSED'])
                field(name: 'code', type: 'string', pattern: '^[A-Z]{3}-\\d{3}$')
                field(name: 'score', type: 'integer', minInclusive: 0, maxInclusive: 100)
                field(name: 'temperature', type: 'decimal', minExclusive: -273.15, maxExclusive: 1000)
                field(name: 'fixedCode', type: 'string', length: 6)
                field(name: 'bio', type: 'string', minLength: 10, maxLength: 200)
            }
        }
        String actualJson = sb.toJsonSchema()

        then:
        assertJsonEquals('''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Constraints",
          "type": "object",
          "properties": {
            "status": {
              "type": "string",
              "enum": ["OPEN", "IN_PROGRESS", "CLOSED"]
            },
            "code": {
              "type": "string",
              "pattern": "^[A-Z]{3}-\\\\d{3}$"
            },
            "score": {
              "type": "integer",
              "minimum": 0,
              "maximum": 100
            },
            "temperature": {
              "type": "number",
              "exclusiveMinimum": -273.15,
              "exclusiveMaximum": 1000
            },
            "fixedCode": {
              "type": "string",
              "minLength": 6,
              "maxLength": 6
            },
            "bio": {
              "type": "string",
              "minLength": 10,
              "maxLength": 200
            },
            "PredefinedSteps": {
              "type": "object",
              "x-cristal-dynamicForms": {
                "hidden": true,
                "required": false
              }
            }
          },
          "required": ["status", "code", "score", "temperature", "fixedCode", "bio"],
          "additionalProperties": false
        }
        ''', actualJson)
    }

    def 'Maps nested structs and struct arrays'() {
        when:
        def sb = SchemaBuilder.build('Test', 'Company', 0) {
            struct(name: 'Company', useSequence: true) {
                field(name: 'companyName', type: 'string')
                struct(name: 'Address', multiplicity: '1..1') {
                    field(name: 'city', type: 'string')
                    field(name: 'street', type: 'string')
                }
                struct(name: 'Departments', multiplicity: '0..*', useSequence: true) {
                    field(name: 'deptName', type: 'string')
                    field(name: 'budget', type: 'decimal')
                }
            }
        }
        String actualJson = sb.toJsonSchema()

        then:
        assertJsonEquals('''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "Company",
          "type": "object",
          "properties": {
            "companyName": {
              "type": "string"
            },
            "Address": {
              "type": "object",
              "properties": {
                "city": { "type": "string" },
                "street": { "type": "string" }
              },
              "required": ["city", "street"],
              "additionalProperties": false
            },
            "Departments": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "deptName": { "type": "string" },
                  "budget": { "type": "number" }
                },
                "required": ["deptName", "budget"],
                "additionalProperties": false
              }
            },
            "PredefinedSteps": {
              "type": "object",
              "x-cristal-dynamicForms": {
                "hidden": true,
                "required": false
              }
            }
          },
          "required": ["companyName", "Address"],
          "additionalProperties": false
        }
        ''', actualJson)
    }

    def 'Preserves CRISTAL metadata in x-cristal-* vendor extensions'() {
        when:
        def sb = SchemaBuilder.build('Test', 'MetadataSchema', 0) {
            struct(name: 'MetadataSchema') {
                field(name: 'itemRef', type: 'string') {
                    reference(itemType: 'CustomItem', collectionName: 'MyCollection')
                }
                field(name: 'category', type: 'string') {
                    listOfValues(values: ['CatA', 'CatB'])
                }
                field(name: 'weight', type: 'decimal') {
                    unit(values: ['kg', 'lb'], default: 'kg')
                }
                field(name: 'salary', type: 'decimal') {
                    dynamicForms(label: 'Base Salary', container: 'ui-g-6') {
                        warning(pattern: '^[0-9]+$', message: 'Must be positive')
                    }
                }
            }
        }
        String actualJson = sb.toJsonSchema()

        then:
        assertJsonEquals('''
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "title": "MetadataSchema",
          "type": "object",
          "properties": {
            "itemRef": {
              "type": "string",
              "x-cristal-reference": {
                "itemType": "CustomItem",
                "collectionName": "MyCollection"
              }
            },
            "category": {
              "type": "string",
              "x-cristal-listOfValues": {
                "values": ["CatA", "CatB"]
              }
            },
            "weight": {
              "type": "number",
              "x-cristal-unit": {
                "values": ["kg", "lb"],
                "default": "kg"
              }
            },
            "salary": {
              "type": "number",
              "x-cristal-dynamicForms": {
                "label": "Base Salary",
                "container": "ui-g-6",
                "warning": {
                  "pattern": "^[0-9]+$",
                  "message": "Must be positive"
                }
              }
            },
            "PredefinedSteps": {
              "type": "object",
              "x-cristal-dynamicForms": {
                "hidden": true,
                "required": false
              }
            }
          },
          "required": ["itemRef", "category", "weight", "salary"],
          "additionalProperties": false
        }
        ''', actualJson)
    }
}
