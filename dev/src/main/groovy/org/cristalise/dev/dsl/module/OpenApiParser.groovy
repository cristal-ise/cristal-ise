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
import static org.cristalise.kernel.collection.Collection.Cardinality.*

import java.util.Map.Entry

import org.apache.commons.lang3.StringUtils
import org.cristalise.dev.dsl.item.CRUDDependency
import org.cristalise.dev.dsl.item.CRUDItem
import org.cristalise.dsl.persistency.outcome.Field

import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.yaml.YamlSlurper

@Slf4j @CompileStatic
class OpenApiParser {

    CRUDModule module

    public OpenApiParser(Map args) {
        log.debug 'constructor() - args:{}', args

        module = new CRUDModule(name: args.name)
    }
    
    private CRUDItem addItemToModule(Entry<String, Object> itemYaml) {
        def itemName = StringUtils.capitalize(itemYaml.key)
        def item = new CRUDItem(name: itemName)
        module.items[itemName] = item

        log.info('{}', item)

        return item
    }
    
    private Field addFieldToItem(CRUDItem item, String fieldType, Entry<String, Object> fieldYaml) {
        String fieldName = StringUtils.capitalize(fieldYaml.key)

        def field = new Field(name: fieldName, type: fieldType)
        log.info('  {} {}', field.name, field.type)
        item.fields[field.name] = field

        return field
    }
    
    @CompileDynamic
    private CRUDDependency preprocessDependency(CRUDItem item, Object dependencyYaml) {
        def dependencyName = StringUtils.capitalize(dependencyYaml.key)
        def cardinality = dependencyYaml.value.type == 'array' ? OneToMany : OneToOne
        def $ref = cardinality == OneToOne ? dependencyYaml.value.$ref as String : dependencyYaml.value.items.$ref as String
        def dependencyToName = $ref.substring($ref.lastIndexOf('/')+1)

        def dependency = new CRUDDependency(
            name: dependencyName,
            from: item.name,
            to: dependencyToName,
            type: Unidirectional, // can be Bidirectional if the other end also has a declaration
            cardinality: cardinality,
            originator: cardinality == OneToMany
        )

        log.info('  {} {}', dependencyName, dependency.plantUml)

        item.dependencies[dependencyName] = dependency

        return dependency
    }

    private void consolidateDependencies() {
        module.items.each { String itemName, CRUDItem item ->
            item.dependencies.each { dependencyName, dependency ->
                def otherItem = module.items[dependency.to]

                CRUDDependency otherDependency = otherItem.dependencies.find { otherItemName, otherDep -> otherDep.to == itemName }?.value

                if (otherDependency) {
                    log.info('consolidateDependencies() - ORIG current:({}) ORIG other:({})', dependency.plantUml, otherDependency.plantUml)
                    dependency.type = Bidirectional
                    otherDependency.type = Bidirectional
                    log.info('consolidateDependencies() - NEW  current:({}) NEW other:({})', dependency.plantUml, otherDependency.plantUml)
                }
            }
        }
    }

    @CompileDynamic
    public CRUDModule parse(String oasText) {
        def oas = oasText.startsWith('{') ? new JsonSlurper().parseText(oasText) : new YamlSlurper().parseText(oasText)

        oas.components.schemas.each { itemYaml ->
            CRUDItem item = addItemToModule(itemYaml as Entry<String, Object>)

            itemYaml.value.properties.each { propYaml ->
                String propType = propYaml.value.type

                assert propType : "Property 'type' cannot be null for field ${propYaml.key} in item ${item.name}"

                if (['array','object'].contains(propType)) {
                    preprocessDependency(item, propYaml)
                }
                else {
                    addFieldToItem(item, propType, propYaml as Entry<String, Object>)
                }
            }
        }

        consolidateDependencies()

        return module
    }
}
