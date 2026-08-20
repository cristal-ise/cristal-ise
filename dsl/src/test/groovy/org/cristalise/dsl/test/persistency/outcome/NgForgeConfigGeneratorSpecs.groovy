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
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals

class NgForgeConfigGeneratorSpecs extends Specification implements CristalTestSetup {

    def 'Generates ng-forge form configuration root and field list'() {
        when:
        def sb = SchemaBuilder.build('Test', 'UserForm', 1) {
            struct(name: 'UserForm', documentation: 'User registration form') {
                dynamicForms(label: 'Register User', width: '600px')
                field(name: 'username', type: 'string') {
                    dynamicForms(label: 'User Name', placeholder: 'Enter your name')
                }
                field(name: 'password', type: 'string') {
                    dynamicForms(inputType: 'password')
                }
                field(name: 'role', type: 'string', values: ['ADMIN', 'USER', 'GUEST'])
            }
        }
        String actualJson = sb.toNgForgeJson()

        then:
        assertJsonEquals('''
        {
          "id": "UserForm",
          "title": "Register User",
          "description": "User registration form",
          "width": "600px",
          "fields": [
            {
              "key": "username",
              "type": "input",
              "label": "User Name",
              "placeholder": "Enter your name",
              "validation": {
                "required": true
              }
            },
            {
              "key": "password",
              "type": "password",
              "label": "password",
              "validation": {
                "required": true
              }
            },
            {
              "key": "role",
              "type": "select",
              "label": "role",
              "options": [
                { "label": "ADMIN", "value": "ADMIN" },
                { "label": "USER", "value": "USER" },
                { "label": "GUEST", "value": "GUEST" }
              ],
              "validation": {
                "required": true
              }
            },
            {
              "key": "PredefinedSteps",
              "type": "input",
              "label": "PredefinedSteps",
              "hidden": true
            }
          ]
        }
        ''', actualJson)
    }

    def 'Maps UI controls and DynamicForms properties accurately'() {
        when:
        def sb = SchemaBuilder.build('Test', 'ControlsForm', 0) {
            struct(name: 'ControlsForm') {
                field(name: 'bio', type: 'string') {
                    dynamicForms(type: 'TEXTAREA', label: 'Biography', disabled: true)
                }
                field(name: 'agree', type: 'boolean') {
                    dynamicForms(label: 'I Agree')
                }
                field(name: 'birthDate', type: 'date') {
                    dynamicForms(label: 'Date of Birth')
                }
                field(name: 'phone', type: 'string') {
                    dynamicForms(mask: '(999) 999-9999', container: 'ui-g-6', labelGrid: 'ui-g-4', control: 'ui-g-8')
                }
            }
        }
        String actualJson = sb.toNgForgeJson()

        then:
        assertJsonEquals('''
        {
          "id": "ControlsForm",
          "title": "ControlsForm",
          "fields": [
            {
              "key": "bio",
              "type": "textarea",
              "label": "Biography",
              "disabled": true,
              "validation": {
                "required": true
              }
            },
            {
              "key": "agree",
              "type": "checkbox",
              "label": "I Agree",
              "validation": {
                "required": true
              }
            },
            {
              "key": "birthDate",
              "type": "datepicker",
              "label": "Date of Birth",
              "validation": {
                "required": true
              }
            },
            {
              "key": "phone",
              "type": "input",
              "label": "phone",
              "props": {
                "mask": "(999) 999-9999",
                "container": "ui-g-6",
                "labelGrid": "ui-g-4",
                "control": "ui-g-8"
              },
              "validation": {
                "required": true
              }
            },
            {
              "key": "PredefinedSteps",
              "type": "input",
              "label": "PredefinedSteps",
              "hidden": true
            }
          ]
        }
        ''', actualJson)
    }

    def 'Maps validations and warning rules'() {
        when:
        def sb = SchemaBuilder.build('Test', 'ValidationForm', 0) {
            struct(name: 'ValidationForm') {
                field(name: 'code', type: 'string', pattern: '^[A-Z]{3}$') {
                    dynamicForms {
                        warning(expression: "element.value != 'XXX'", message: 'XXX is deprecated')
                    }
                }
                field(name: 'age', type: 'integer', minInclusive: 18, maxInclusive: 120)
            }
        }
        String actualJson = sb.toNgForgeJson()

        then:
        assertJsonEquals('''
        {
          "id": "ValidationForm",
          "title": "ValidationForm",
          "fields": [
            {
              "key": "code",
              "type": "input",
              "label": "code",
              "validation": {
                "required": true,
                "pattern": "^[A-Z]{3}$",
                "warning": {
                  "expression": "element.value != 'XXX'",
                  "message": "XXX is deprecated"
                }
              }
            },
            {
              "key": "age",
              "type": "number",
              "label": "age",
              "validation": {
                "required": true,
                "min": 18,
                "max": 120
              }
            },
            {
              "key": "PredefinedSteps",
              "type": "input",
              "label": "PredefinedSteps",
              "hidden": true
            }
          ]
        }
        ''', actualJson)
    }

    def 'Maps nested structs into groups and arrays'() {
        when:
        def sb = SchemaBuilder.build('Test', 'OrderForm', 0) {
            struct(name: 'OrderForm', useSequence: true) {
                struct(name: 'BillingAddress', multiplicity: '1..1') {
                    dynamicForms(label: 'Billing Information')
                    field(name: 'city', type: 'string')
                }
                struct(name: 'OrderItems', multiplicity: '0..*', useSequence: true) {
                    dynamicForms(label: 'Order Line Items')
                    field(name: 'sku', type: 'string')
                    field(name: 'qty', type: 'integer')
                }
            }
        }
        String actualJson = sb.toNgForgeJson()

        then:
        assertJsonEquals('''
        {
          "id": "OrderForm",
          "title": "OrderForm",
          "fields": [
            {
              "key": "BillingAddress",
              "type": "group",
              "label": "Billing Information",
              "fields": [
                {
                  "key": "city",
                  "type": "input",
                  "label": "city",
                  "validation": {
                    "required": true
                  }
                }
              ]
            },
            {
              "key": "OrderItems",
              "type": "array",
              "label": "Order Line Items",
              "fields": [
                {
                  "key": "sku",
                  "type": "input",
                  "label": "sku",
                  "validation": {
                    "required": true
                  }
                },
                {
                  "key": "qty",
                  "type": "number",
                  "label": "qty",
                  "validation": {
                    "required": true
                  }
                }
              ]
            }
          ]
        }
        ''', actualJson)
    }

    def 'Preserves CRISTAL metadata and cascade dependencies in props'() {
        when:
        def sb = SchemaBuilder.build('Test', 'AdvancedForm', 0) {
            struct(name: 'AdvancedForm') {
                field(name: 'itemRef', type: 'string') {
                    reference(itemType: 'CustomItem', collectionName: 'MyCollection')
                }
                field(name: 'country', type: 'string') {
                    listOfValues(scriptRef: 'getCountries', inputName: 'region')
                    dynamicForms(updateFields: ['city', 'zip'], updateScriptRef: 'updateCityOptions')
                }
                field(name: 'weight', type: 'decimal') {
                    unit(values: ['kg', 'lb'], default: 'kg')
                }
            }
        }
        String actualJson = sb.toNgForgeJson()

        then:
        assertJsonEquals('''
        {
          "id": "AdvancedForm",
          "title": "AdvancedForm",
          "fields": [
            {
              "key": "itemRef",
              "type": "input",
              "label": "itemRef",
              "props": {
                "reference": {
                  "itemType": "CustomItem",
                  "collectionName": "MyCollection"
                }
              },
              "validation": {
                "required": true
              }
            },
            {
              "key": "country",
              "type": "select",
              "label": "country",
              "props": {
                "listOfValues": {
                  "scriptRef": "getCountries",
                  "inputName": "region"
                },
                "updateFields": ["city", "zip"],
                "updateScriptRef": "updateCityOptions"
              },
              "validation": {
                "required": true
              }
            },
            {
              "key": "weight",
              "type": "number",
              "label": "weight",
              "props": {
                "unit": {
                  "values": ["kg", "lb"],
                  "default": "kg"
                }
              },
              "validation": {
                "required": true
              }
            },
            {
              "key": "PredefinedSteps",
              "type": "input",
              "label": "PredefinedSteps",
              "hidden": true
            }
          ]
        }
        ''', actualJson)
    }
}
