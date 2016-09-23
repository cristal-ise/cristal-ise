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
package org.cristalise.kernel.test.entity

import org.cristalise.dsl.test.builders.ItemTestBuilder
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class ItemCreateSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemoryServer() }
    def cleanup() { cristalCleanup() }

    def 'Item can be created without workflow'() {
        when:
        def builder = ItemTestBuilder.create(name: "myFisrtItem", folder: "testing") {}

        then:
        assert builder
        builder.checkPathes("myFisrtItem", "testing")
        builder.checkProperties(Name: "myFisrtItem", Creator: "bootstrap")
        //assert workflow only has predefined steps
    }
    
    def 'Item can be created with CompActDef'() {
        when:
        def builder = ItemTestBuilder.create(name: "myFisrtItem", folder: "testing") {
            CompositeActivityDef {
                ElemActDef('TriggerTestAct',  0)
            }
        }

        then:
        assert builder
        builder.checkPathes("myFisrtItem", "testing")
        builder.checkProperties(Name: "myFisrtItem", Creator: "bootstrap")
        //assert workflow only has predefined steps
    }

}
