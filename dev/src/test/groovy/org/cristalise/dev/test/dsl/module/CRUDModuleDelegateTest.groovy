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
package org.cristalise.dev.test.dsl.module

import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*

import org.cristalise.dev.dsl.module.CRUDModuleDelegate
import org.junit.Test

class CRUDModuleDelegateTest {

    @Test
    public void devModule_Dependency() {
        def moduleD = new CRUDModuleDelegate()
        def crudModule = moduleD.processClosure() {
            //System.setProperty('line.separator', '\n')
    
            Module(namespace: 'test') {
                Item(name: 'Car') {
                    field(name: 'RegistrationPlate')
                }

                Item(name: 'ClubMember') {
                    field(name: 'Email')

                    dependency(to: 'Car', type: Bidirectional, cardinality: OneToMany)
                }
            }
        }

        assert crudModule
        assert crudModule.namespace == 'test'
        assert crudModule.items.size() == 2
        assert crudModule.getPlantUml() ==
"""@startuml
class Car {
  RegistrationPlate : xs:string
}
class ClubMember {
  Email : xs:string
}
ClubMember -- "*" Car
@enduml
"""
    }
}
