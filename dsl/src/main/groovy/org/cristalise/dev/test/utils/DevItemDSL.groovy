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

package org.cristalise.dev.test.utils

import groovy.transform.CompileStatic

import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.ElemActDefBuilder
import org.cristalise.dsl.persistency.outcome.SchemaBuilder
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.persistency.outcome.Schema


/**
 * 
 */
@CompileStatic
class DevItemDSL extends DevItemUtility {

    public Schema Schema(String name, String folder, Closure cl) {
        createNewSchema(name, folder)
        def schema = SchemaBuilder.build(name, 0, cl);
        editSchema(name, folder, schema.XSD)
        return schema
    }

    public ActivityDef ElementaryActivityDef(String actName, String folder, Closure cl) {
        createNewElemActDesc(actName, folder)
        def eaDef = ElemActDefBuilder.build(name: (Object)actName, version: 0, cl)
        editElemActDesc(actName, folder, eaDef)
        return eaDef
    }

    public CompositeActivityDef CompositeActivityDef(String actName, String folder, Closure cl) {
        createNewCompActDesc(actName, folder)
        def caDef = CompActDefBuilder.build(name: (Object)actName, version: 0, cl)
        editCompActDesc(actName, folder, caDef)
        return caDef
    }
    
    def DescriptionItem(String itemName, String folder, Closure cl) {
        createNewDescriptionItem(itemName, folder)
    }
}
