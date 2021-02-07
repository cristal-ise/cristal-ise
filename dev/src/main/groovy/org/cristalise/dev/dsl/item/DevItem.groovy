/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dev.dsl.item

import static org.cristalise.dev.dsl.item.DevDependency.Cardinality.*

import org.cristalise.dsl.persistency.outcome.Field
import org.cristalise.dsl.persistency.outcome.Struct

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class DevItem extends Struct {

    List<DevDependency> dependencies = []

    //required for ObjectGraphBuilder
    public DevItem() {
        log.debug('constructor()')
    }

    public DevItem(String n) {
        log.debug('constructor() - name:{}', n)
        name = n
    }
    
    public void addDependency(DevDependency d) {
        d.from = name
        dependencies.add(d)
    }

    public String getPlantUml() {
        def model = new StringBuffer("class $name {\n")
        fields.each { String name, Field field ->
            model.append("  ${field.name} : ${field.type}\n")
        }
        model.append('}\n')

        dependencies.each { dependency ->
            model.append(dependency.getPlantUml())
        }

        return model.toString()
    }
}
