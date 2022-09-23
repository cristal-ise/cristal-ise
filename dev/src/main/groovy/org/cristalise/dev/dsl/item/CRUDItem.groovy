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

import static org.cristalise.kernel.collection.Collection.Type.*

import org.apache.commons.lang3.StringUtils
import org.atteo.evo.inflector.English
import org.cristalise.dsl.persistency.outcome.Field
import org.cristalise.dsl.persistency.outcome.Struct

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class CRUDItem extends Struct {

    List<String> orderOfElements = []
    Map<String, CRUDDependency> dependencies = [:]

    public void addBidirectionalDependency(CRUDDependency otherDep) {
        log.debug('addBidirectionalDependency(item:{}) - processing other dependency:{}', name, otherDep.name)

        def newDep = new CRUDDependency(
            from: otherDep.to,
            to: otherDep.from,
            type: Bidirectional,
            cardinality: otherDep.cardinality.reverse(),
            originator: false
        )

        if (!dependencies.containsKey(newDep.name)) {
            log.debug('addBiderectionalDependency(item:{}) - adding:{}', name, newDep.name)

            dependencies.put(newDep.name, newDep)
            orderOfElements << newDep.name
        }

        dependencies.get(newDep.name).otherDependencyNames << otherDep.name
    }

    public String getPlantUml() {
        def model = new StringBuffer("class $name {\n")
        model.append("  Name : xs:string\n")
        model.append("  State : xs:string\n")
        fields.each { String name, Field field ->
            model.append("  ${field.name} : ${field.type}\n")
        }
        model.append('}\n')

        dependencies.values().each { dependency ->
            if (dependency.originator) model.append(dependency.getPlantUml())
        }

        return model.toString()
    }

    @Override
    public String toString() {
        return name
    }

    public String getLowPlur() {
        return StringUtils.uncapitalize(English.plural(name));
    }

    public String getUpPlur() {
        return English.plural(name)
    }

    public String getLowSing() {
        return StringUtils.uncapitalize(name)
    }

    public String getUpSing() {
        return name
    }
}
