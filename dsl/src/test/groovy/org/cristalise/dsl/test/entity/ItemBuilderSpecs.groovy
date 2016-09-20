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
package org.cristalise.dsl.test.entity

import org.cristalise.dsl.entity.ItemBuilder;
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class ItemBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemorySetup()  }
    def cleanup() { cristalCleanup() }

    def 'Cannot Build Item without folder specified'() {
        when:
        def item = ItemBuilder.build(name: "myFisrtItem") {}

        then:
        thrown InvalidDataException
    }

    def 'ItemBuilder with empty Workflow adds Name to Properties'() {
        when:
        def item = ItemBuilder.build(name: "dummy", folder: "testing") {}

        then:
        item.properties.size == 1
        item.properties[0].name == "Name"
        item.properties[0].value == "dummy"
    }

    def 'ItemBuilder with empty Workflow can have user defined Properties'() {
        when:
        def item = ItemBuilder.build(name: "userDefinedProps", folder: "testing") {
            InmutableProperty("Brain": "kovax")
            Property("Pinky": "kovax")
        }

        then:
        item.properties.size == 3
        item.properties[0].name    == "Name"
        item.properties[0].value   == "userDefinedProps"
        item.properties[0].mutable == true
        item.properties[1].name    == "Brain"
        item.properties[1].value   == "kovax"
        item.properties[1].mutable == false
        item.properties[2].name    == "Pinky"
        item.properties[2].value   == "kovax"
        item.properties[2].mutable == true
    }

    def 'ItemBuilder builds domain Workflow'() {
        when:
        def item = ItemBuilder.build(name: "myFisrtItem", folder: "testing") { Workflow {} }

        then:
        item.wf
        item.wf.search("workflow/domain")

        item.properties.size == 1
    }
}
