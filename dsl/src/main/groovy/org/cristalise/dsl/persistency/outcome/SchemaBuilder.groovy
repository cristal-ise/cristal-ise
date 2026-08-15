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
package org.cristalise.dsl.persistency.outcome

import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.csv.TabularGroovyParserBuilder
import org.cristalise.dsl.persistency.outcome.generator.*
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.ResourceImportHandler
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.LocalObjectLoader

import static org.cristalise.dsl.persistency.outcome.SchemaBuilder.FileType.*
import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE

/**
 * Fluent builder for compiling CRISTAL Schema definitions and generating multiple target representations.
 */
@CompileStatic @Slf4j
class SchemaBuilder {
    public enum FileType {XLSX, CSV, XSD}

    String module = ''
    String name = ''
    int version = -1

    DomainPath domainPath = null

    Schema schema = null
    Struct rootStruct = null
    SchemaContext context = null
    Collection<Script> expressionScipts = []

    public SchemaBuilder() {}

    /**
     * 
     * @param name
     * @param version
     */
    public SchemaBuilder(String name, int version) {
        this.name    = name
        this.version = version
    }

    /**
     *
     * @param module
     * @param name
     * @param version
     */
    public SchemaBuilder(String module, String name, int version) {
        this.module  = module
        this.name    = name
        this.version = version
    }

    /**
     *
     * @param xsdFile
     * @return
     */
    public SchemaBuilder loadXSD(String xsdFile) {
        return loadXSD(new File(xsdFile))
    }

    public SchemaBuilder loadXSD(File xsdFile) {
        log.debug "loadXSD() - From file:$xsdFile"

        schema = new Schema(name, version, xsdFile.text)
        schema.validate()

        return this
    }
    
    public SchemaBuilder generateSchema(Closure cl) {
        def schemaD = new SchemaDelegate(name: name, version: version, module: module)
        schemaD.processClosure(cl)

        this.rootStruct = schemaD.rootStruct
        this.context = schemaD.context

        log.debug "generated xsd:\n" + schemaD.xsdString

        schema = new Schema(name, version, schemaD.xsdString)
        String errors = schema.validate()
        schema.namespace = module

        if (errors) {
            log.error "generateSchema() - validation errors:\n{}", errors
            log.error "generateSchema() - validation error xsd:\n{}", schemaD.xsdString
            throw new InvalidDataException(errors)
        }

        if (schemaD.expressionScripts) {
            expressionScipts = schemaD.expressionScripts.values()
        }

        return this
    }

    public SchemaBuilder generateSchema(TabularGroovyParser parser) {
        def schemaD = new SchemaDelegate(name: name, version: version, module: module)
        schemaD.processTabularData(parser)

        this.rootStruct = schemaD.rootStruct
        this.context = schemaD.context

        schema = new Schema(name, version, schemaD.xsdString)
        String errors = schema.validate()

        if (errors) {
            log.error "generateSchema() - xsd:\n{}", schemaD.xsdString
            throw new InvalidDataException(errors)
        }

        if (schemaD.expressionScripts) {
            expressionScipts = schemaD.expressionScripts.values()
        }

        return this
    }

    /**
     * Generates a target output using the specified SchemaGenerator strategy.
     *
     * @param generator the generator strategy to use
     * @return the generated output of type T
     */
    public <T> T generate(SchemaGenerator<T> generator) {
        if (!rootStruct) {
            throw new InvalidDataException("Cannot generate output from uninitialized or XSD-only loaded Schema")
        }
        return generator.generate(rootStruct, context ?: new SchemaContext(module, name, version))
    }

    /**
     * Returns the XML Schema (XSD) string representation.
     */
    public String toXsd() {
        if (schema?.schemaData) return schema.schemaData
        if (rootStruct) return generate(new XsdSchemaGenerator())
        return ''
    }

    /**
     * Generates a JSON Schema (Draft 2020-12) string representation.
     *
     * @param pretty whether to format with indentation
     * @return formatted JSON Schema string
     */
    public String toJsonSchema(boolean pretty = true) {
        if (!rootStruct) {
            throw new InvalidDataException("Cannot generate JSON Schema from uninitialized or XSD-only loaded Schema")
        }
        return new JsonSchemaGenerator().generateJson(rootStruct, context ?: new SchemaContext(module, name, version), pretty)
    }

    /**
     * Generates a JSON Schema as a structured Map.
     */
    public Map<String, Object> toJsonSchemaMap() {
        return generate(new JsonSchemaGenerator())
    }

    /**
     * Generates an @ng-forge/dynamic-forms compatible configuration JSON string.
     *
     * @param pretty whether to format with indentation
     * @return formatted ng-forge form configuration JSON string
     */
    public String toNgForgeJson(boolean pretty = true) {
        if (!rootStruct) {
            throw new InvalidDataException("Cannot generate ng-forge config from uninitialized or XSD-only loaded Schema")
        }
        return new NgForgeConfigGenerator().generateJson(rootStruct, context ?: new SchemaContext(module, name, version), pretty)
    }

    /**
     * Generates an @ng-forge/dynamic-forms compatible configuration as a structured Map.
     */
    public Map<String, Object> toNgForgeMap() {
        return generate(new NgForgeConfigGenerator())
    }

    /**
     * Exports generated output from the specified generator to a File.
     *
     * @param generator the generator to use
     * @param outputFile destination file
     * @return the output File
     */
    public File export(SchemaGenerator generator, File outputFile) {
        if (outputFile.parentFile) {
            outputFile.parentFile.mkdirs()
        }
        def result = generate(generator)
        if (result instanceof Map || result instanceof List) {
            outputFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(result))
        } else {
            outputFile.text = result.toString()
        }
        return outputFile
    }

    /**
     * Exports generated output from the specified generator to a file path.
     */
    public File export(SchemaGenerator generator, String outputPath) {
        return export(generator, new File(outputPath))
    }

    /**
     * Exports all supported formats (XSD, JSON Schema, ng-forge JSON) to the specified directory.
     *
     * @param outputDir destination directory
     * @param baseName base file name (defaults to schema name)
     */
    public void exportAll(File outputDir, String baseName = null) {
        outputDir.mkdirs()
        String fileName = baseName ?: (name ?: 'schema')
        export(new XsdSchemaGenerator(), new File(outputDir, "${fileName}.xsd"))
        export(new JsonSchemaGenerator(), new File(outputDir, "${fileName}.schema.json"))
        export(new NgForgeConfigGenerator(), new File(outputDir, "${fileName}.forge.json"))
    }

    /**
     * Builds the Schema and creates the Resource Item
     *
     * @param module
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public static SchemaBuilder create(String module, String name, int version, Closure cl) {
        def sb = build(module, name, version, cl)
        sb.create()
        return sb
    }

    /**
     * Loads and parses the file to 'build' Schema and creates the ResourceItem
     *
     * @param module
     * @param name
     * @param version
     * @param fileName
     * @return
     */
    public static SchemaBuilder create(String module, String name, int version, String fileName) {
        def sb = build(module, name, version, fileName)
        sb.create()
        return sb
    }

    /**
     * 
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public static SchemaBuilder build(String name, int version, Closure cl) {
        return build('', name, version, cl)
    }

    /**
     *
     * @param cl
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, Closure cl) {
        log.debug("build(closure) - module:{} name:{} version:{}", module, name, version)

        def sb = new SchemaBuilder(module, name, version)
        sb.generateSchema(cl)
        return sb
    }

    /**
     * 
     * @param module
     * @param name
     * @param version
     * @param fileName
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, String fileName) {
        return build(module, name, version, new File(fileName))
    }

    /**
     * 
     * @param name
     * @param version
     * @param file
     * @return
     */
    public static SchemaBuilder build(String name, int version, File file) {
        return build('', name, version, file)
    }

    /**
     * 
     * @param module
     * @param name
     * @param version
     * @param type
     * @param fileName
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, File file) {
        log.debug("build(file) - module:{} name:{} version:{} file:{}", module, name, version, file.name)

        def sb = new SchemaBuilder(module, name, version)
        def fileName = file.name
        def type = fileName.substring(fileName.lastIndexOf('.')+1).toUpperCase() as FileType

        switch(type) {
            case XSD: return sb.loadXSD(file)
            case XLSX: return sb.generateSchema(new TabularGroovyParserBuilder().excelParser(file, name).withHeaderRowCount(2).build())
            case CSV: return sb.generateSchema(new TabularGroovyParserBuilder().csvParser(file).withHeaderRowCount(2).build())
            default: throw new UnsupportedOperationException("Unsupported file type:$type module:$module name:$name")
        }
    }

    /**
      * Bootstrap method to create the ResourceItem from a fully configured SchemaBuilder
     *  
     * @return the DomainPath of the newly created resource Item
    */
    public DomainPath create() {
        Schema schemaSchema = LocalObjectLoader.getSchema("Schema", 0)
        ResourceImportHandler importHandler = Gateway.getResourceImportHandler(SCHEMA_RESOURCE);
        return domainPath = importHandler.createResource(module, name, version, new Outcome(-1, schema.schemaData, schemaSchema), false, null)
    }
}
