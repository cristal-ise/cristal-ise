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
package org.cristalise.dsl.querying

import static org.cristalise.kernel.process.resource.BuiltInResources.QUERY_RESOURCE

import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.resource.ResourceImportHandler
import org.cristalise.kernel.querying.Query
import org.cristalise.kernel.utils.LocalObjectLoader

import groovy.transform.CompileStatic


/**
 *
 */
@CompileStatic
class QueryBuilder {
    String name = ""
    String module = ""
    int version = -1

    Query query = null

    DomainPath domainPath = null

    public QueryBuilder() {}

    /**
     * 
     * @param module
     * @param name
     * @param version
     */
    public QueryBuilder(String module, String name, int version) {
        this.module  = module
        this.name    = name
        this.version = version
    }


    public static Query build(String module, String name, int version, Closure cl) {
        def qb = new QueryBuilder(module, name, version)

        def queryD = new QueryDelegate(module, name, version)
        queryD.processClosure(cl)

        //delegate's processClosure() can set these members, so copying the latest values
        qb.module   = queryD.module
        qb.name     = queryD.name
        qb.version  = queryD.version
        qb.query    = new Query(name, version, (ItemPath)null, queryD.writer.toString())

        qb.query.namespace = module

        return qb.query
    }

    /**
     * Bootstrap method to create the ResourceItem from a fully configured ScriptBuilder
     *  
     * @return the DomainPath of the newly created resource Item
     */
    public DomainPath create() {
        Schema querySchema = LocalObjectLoader.getSchema("Query", 0)
        ResourceImportHandler importHandler = Gateway.getResourceImportHandler(QUERY_RESOURCE);
        return domainPath = importHandler.createResource(module, name, version, new Outcome(-1, (String)null, querySchema), false, null)
    }
}
