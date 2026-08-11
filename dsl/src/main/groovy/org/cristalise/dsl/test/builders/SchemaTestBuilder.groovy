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
package org.cristalise.dsl.test.builders

import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.dsl.persistency.outcome.SchemaDelegate
import org.cristalise.kernel.test.utils.KernelXMLUtility

import groovy.transform.CompileStatic


/**
 *
 */
@CompileStatic
class SchemaTestBuilder extends SchemaBuilder {

    public SchemaTestBuilder(SchemaBuilder sb) {
        name = sb.name
        module = sb.module
        version = sb.version

        schema = sb.schema
    }

    public static SchemaTestBuilder build(String module, String name, int version, String fileName) {
        def sb = SchemaBuilder.build(module, name, version, fileName)
        sb.schema.validate()

        return new SchemaTestBuilder(sb)
    }

    public static SchemaTestBuilder build(String module, String name, int version, @DelegatesTo(SchemaDelegate) Closure cl) {
        def sb = SchemaBuilder.build(module, name, version, cl)
        sb.schema.validate()

        return new SchemaTestBuilder(sb)
    }

    public boolean compareXML(String xml) {
        return KernelXMLUtility.compareXML(xml, schema.schemaData);
    }
}
