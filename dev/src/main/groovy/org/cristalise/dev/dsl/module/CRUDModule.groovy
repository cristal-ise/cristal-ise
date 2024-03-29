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
package org.cristalise.dev.dsl.module

import static org.cristalise.kernel.collection.Collection.Type.*

import org.cristalise.dev.dsl.item.CRUDItem

import groovy.transform.CompileStatic

@CompileStatic
class CRUDModule {

    String name
    String namespace
    String rootPackage
    
    /**
     * Set this to true in the DSL file to trigger the generation of the Module.groovy
     */
    boolean generateModule = false
    /**
     * Set this to true in the DSL file to trigger the generation of JSON files to configure WwebUI
     */
    boolean webuiConfigs = false

    List<String> orderOfElements = []
    Map<String, CRUDItem> items = [:]

    /**
     * Create Dependency in the CRUDItem of the other end of the relationship
     */
    public void createBidirectionalDependencies() {
        orderOfElements.each { itemType ->
            def item = items[itemType]

            item.dependencies.values().each { dependency ->
                if (dependency.originator && dependency.type == Bidirectional) {
                    def itemTo = items[dependency.to]
                    itemTo.addBidirectionalDependency(dependency)
                }
            }
        }
    }

    public String getPlantUml() {
        def model = new StringBuffer('@startuml\n')
        items.values().each { CRUDItem item ->
            model.append(item.getPlantUml())
        }
        return model.append('@enduml\n').toString()
    }
}
