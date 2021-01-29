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

import static org.cristalise.dsl.persistency.outcome.SchemaBuilder.FileType.CSV
import static org.cristalise.dsl.persistency.outcome.SchemaBuilder.FileType.XLSX
import static org.cristalise.dsl.persistency.outcome.SchemaBuilder.FileType.XSD
import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE

import org.cristalise.dsl.csv.TabularGroovyParser
import org.cristalise.dsl.csv.TabularGroovyParserBuilder
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.ResourceImportHandler
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 *
 */
@CompileStatic @Slf4j
class SchemaBuilder {
    public enum FileType {XLSX, CSV, XSD}

    String module = ''
    String name = ''
    int version = -1

    DomainPath domainPath = null

    Schema schema = null
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
    
    public SchemaBuilder generateSchema(@DelegatesTo(SchemaDelegate) Closure cl) {
        def schemaD = new SchemaDelegate(name: name, version: version)
        schemaD.processClosure(cl)

        log.debug "generated xsd:\n" + schemaD.xsdString

        schema = new Schema(name, version, schemaD.xsdString)
        String errors = schema.validate()
        schema.namespace = module

        if (errors) {
            log.error "generateSchema() - xsd:\n{}", schemaD.xsdString
            throw new InvalidDataException(errors)
        }

        if (schemaD.expressionScripts) {
            expressionScipts = schemaD.expressionScripts.values()
        }

        return this
    }

    public SchemaBuilder generateSchema(TabularGroovyParser parser) {
        def schemaD = new SchemaDelegate(name: name, version: version)
        schemaD.processTabularData(parser)

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
    public static Schema build(String name, int version, Closure cl) {
        return build('', name, version, cl).schema
    }

    /**
     *
     * @param cl
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, Closure cl) {
        log.info("build(closure) - module:{} name:{} version:{}", module, name, version)

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
    public static Schema build(String name, int version, File file) {
        return build('', name, version, file).schema
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
        log.info("build(file) - module:{} name:{} version:{} file:{}", module, name, version, file.name)

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
