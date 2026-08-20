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
import org.cristalise.dsl.persistency.outcome.Struct
import org.cristalise.dsl.persistency.outcome.generator.SchemaContext
import org.cristalise.dsl.persistency.outcome.generator.SchemaGenerator
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification

class SchemaBuilderMultiOutputSpecs extends Specification implements CristalTestSetup {

    def 'SchemaBuilder provides fluent methods for multiple formats from a single DSL definition'() {
        when:
        def sb = SchemaBuilder.build('InventoryModule', 'Product', 1) {
            struct(name: 'Product', documentation: 'Product item') {
                dynamicForms(label: 'Product Details')
                field(name: 'sku', type: 'string', pattern: '^[A-Z0-9]{8}$')
                field(name: 'price', type: 'decimal', minInclusive: 0)
                field(name: 'inStock', type: 'boolean')
            }
        }

        String xsd = sb.toXsd()
        String jsonSchema = sb.toJsonSchema()
        String ngForge = sb.toNgForgeJson()

        then:
        xsd.contains("<xs:element name='Product'")
        xsd.contains("<xs:element name='sku'")

        jsonSchema.contains('"title": "Product"')
        jsonSchema.contains('"https://json-schema.org/draft/2020-12/schema"')
        jsonSchema.contains('"sku"')

        ngForge.contains('"title": "Product Details"')
        ngForge.contains('"key": "sku"')
        ngForge.contains('"type": "number"') // for price
    }

    def 'SchemaBuilder supports pluggable custom SchemaGenerator implementations'() {
        given:
        SchemaGenerator<List<String>> fieldNameCollector = new SchemaGenerator<List<String>>() {
            @Override
            List<String> generate(Struct rootStruct, SchemaContext context) {
                return new ArrayList<>(rootStruct.fields.keySet())
            }
        }

        when:
        def sb = SchemaBuilder.build('Test', 'Sample', 0) {
            struct(name: 'Sample') {
                field(name: 'first')
                field(name: 'second')
                field(name: 'third')
            }
        }
        List<String> names = sb.generate(fieldNameCollector)

        then:
        names.contains('first')
        names.contains('second')
        names.contains('third')
    }

    def 'SchemaBuilder exports all formats to directory'() {
        given:
        File tempDir = File.createTempDir('schema-export-test', '')

        when:
        def sb = SchemaBuilder.build('Test', 'ExportTest', 0) {
            struct(name: 'ExportTest') {
                field(name: 'alpha', type: 'string')
                field(name: 'beta', type: 'integer')
            }
        }
        sb.exportAll(tempDir)

        then:
        new File(tempDir, 'ExportTest.xsd').exists()
        new File(tempDir, 'ExportTest.schema.json').exists()
        new File(tempDir, 'ExportTest.forge.json').exists()
        new File(tempDir, 'ExportTest.schema.json').text.contains('"title": "ExportTest"')
        new File(tempDir, 'ExportTest.forge.json').text.contains('"id": "ExportTest"')

        cleanup:
        tempDir.deleteDir()
    }

    def 'Tabular Schema supports JSON Schema and ng-forge generation'() {
        when:
        def sb = SchemaBuilder.build('Test', 'Types', 0, 'src/test/data/ExcelSchemaBuilderField.xlsx')

        String xsd = sb.toXsd()
        Map<String, Object> jsonSchema = sb.toJsonSchemaMap()
        Map<String, Object> ngForge = sb.toNgForgeMap()

        then:
        xsd.contains('xs:schema')
        jsonSchema['type'] == 'object'
        jsonSchema['properties'] instanceof Map
        ngForge['fields'] instanceof List
    }
}
