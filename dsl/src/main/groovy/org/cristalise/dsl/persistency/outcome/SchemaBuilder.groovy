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

import static org.cristalise.kernel.process.resource.BuiltInResources.SCHEMA_RESOURCE

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.ResourceImportHandler
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 *
 */
@CompileStatic @Slf4j
class SchemaBuilder {
    String module = ""
    String name = ""
    int version = -1

    DomainPath domainPath = null

    Schema schema = null

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
        log.debug "loadXSD() - From file:$xsdFile"

        schema = new Schema(name, version, new File(xsdFile).text)
        schema.validate()

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
     * @param xsdFile
     * @return
     */
    public static SchemaBuilder create(String module, String name, int version, String xsdFile) {
        def sb = build(module, name, version, xsdFile)
        sb.create()
        return sb
    }

    public static Schema build(String name, int version, Closure cl) {
        def sb = new SchemaBuilder(name, version)

        generateSchema(sb, cl)

        return sb.schema
    }

    /**
     *
     * @param cl
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, Closure cl) {
        def sb = new SchemaBuilder(module, name, version)

        generateSchema(sb, cl)

        return sb
    }

    private static void generateSchema(SchemaBuilder sb, Closure cl) {
        def schemaD = new SchemaDelegate()
        schemaD.processClosure(cl)

        log.debug "generated xsd:\n" + schemaD.xsdString

        sb.schema = new Schema(sb.name, sb.version, schemaD.xsdString)
        String errors = sb.schema.validate()

        if (errors) throw new InvalidDataException(errors)
    }

    /**
     * 
     * @param module
     * @param name
     * @param version
     * @param xsdFile
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, String xsdFile) {
        def sb = new SchemaBuilder(module, name, version)
        return sb.loadXSD(xsdFile)
    }

    /**
      * Bootstrap method to create the ResourceItem from a fully configured SchemaBuilder
     *  
     * @return the DomainPath of the newly created resource Item
    */
    public DomainPath create() {
        Schema schemaSchema = LocalObjectLoader.getSchema("Schema", 0)
        ResourceImportHandler importHandler = Gateway.getResourceImportHandler(SCHEMA_RESOURCE);
        return domainPath = importHandler.createResource(module, name, version, new Outcome(-1, schema.schemaData, schemaSchema), false)
    }
}
