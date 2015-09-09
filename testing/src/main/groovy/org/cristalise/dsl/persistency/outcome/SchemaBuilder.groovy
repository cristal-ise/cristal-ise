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

import groovy.transform.CompileStatic

import org.cristalise.dsl.process.DSLBoostrapper
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Bootstrap
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class SchemaBuilder implements DSLBoostrapper {
    String module = ""
    String name = ""
    int version = -1

    DomainPath domainPath = null

    Schema schema = null

    public SchemaBuilder() {}

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
        Logger.msg 5, "SchemaBuilder.loadXSD() - From file:$xsdFile"

        schema = new Schema(name, version,  new File(xsdFile).getText())
        schema.parse(null)

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
        sb.createResourceItem()
        return sb
    }

    /**
     * Loads and perses the file to 'build' Schema and creates the ResourceItem
     * 
     * @param module
     * @param name
     * @param version
     * @param xsdFile
     * @return
     */
    public static SchemaBuilder create(String module, String name, int version, String xsdFile) {
        def sb = build(module, name, version, xsdFile)
        sb.createResourceItem()
        return sb
    }

    /**
     * 
     * @param cl
     * @return
     */
    public static SchemaBuilder build(String module, String name, int version, Closure cl) {
        def sb = new SchemaBuilder(module, name, version)

        def schemaD = new SchemaDelegate()
        schemaD.processClosure(cl)
        
        Logger.msg 5, "SchemaBuilder - generated xsd:\n" + schemaD.xsdString

        sb.schema = new Schema(name, version, schemaD.xsdString)
        sb.schema.parse(null)

        return sb
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
    public DomainPath createResourceItem() {
//        return domainPath = Bootstrap.verifyResource(module, name, version, "OD", [new Outcome(-1, schema.schema, "Schema", version)] as Set, false)
        return domainPath = Bootstrap.createResource(module, name, version, "OD", [new Outcome(-1, schema.schema, "Schema", version)] as Set, false)
    }
}
