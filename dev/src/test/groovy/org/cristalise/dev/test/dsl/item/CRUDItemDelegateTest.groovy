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
package org.cristalise.dev.test.dsl.item

import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*

import org.cristalise.dev.dsl.item.CRUDAgent
import org.cristalise.dev.dsl.item.CRUDItemDelegate
import org.junit.Test

class CRUDItemDelegateTest {

    @Test
    public void devItem_ListOfFields() {
        def itemD = new CRUDItemDelegate()
        def devItem = itemD.processClosure() {
            Item(name: 'Car') {
                field(name: 'RegistrationPlate', type: 'string')
                field(name: 'ProductionYear', type: 'date')
            }
        }

        assert devItem
        assert devItem.name == 'Car'
        assert devItem.fields['RegistrationPlate'].name == 'RegistrationPlate'
        assert devItem.fields['RegistrationPlate'].type == 'xs:string'
        assert devItem.fields['ProductionYear'].name == 'ProductionYear'
        assert devItem.fields['ProductionYear'].type == 'xs:date'
    }

    @Test
    public void devAgent_ListOfFields() {
        def itemD = new CRUDItemDelegate()
        def devAgent = (CRUDAgent) itemD.processClosure() {
            Agent(name: 'ClubMember') {
                field(name: 'Email', type: 'string')
                field(name: 'DateOfBirth', type: 'date')
            }
        }

        assert devAgent
        assert devAgent.name == 'ClubMember'
        assert devAgent.fields['Email'].name == 'Email'
        assert devAgent.fields['Email'].type == 'xs:string'
        assert devAgent.fields['DateOfBirth'].name == 'DateOfBirth'
        assert devAgent.fields['DateOfBirth'].type == 'xs:date'
    }

    @Test
    public void devItem_Dependency() {
        def itemD = new CRUDItemDelegate()
        def devItem = itemD.processClosure() {
            Item(name: 'ClubMember') {
                dependency(to: 'Car', type: 'Bidirectional', cardinality: OneToMany)
            }
        }

        assert devItem
        assert devItem.name == 'ClubMember'
        assert devItem.dependencies[0]
        assert devItem.dependencies[0].name == 'Cars'
        assert devItem.dependencies[0].type == Bidirectional
        assert devItem.dependencies[0].cardinality == OneToMany
    }
}
