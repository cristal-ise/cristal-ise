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
package org.cristalise.dsl.test.entity.item

import org.cristalise.dsl.entity.item.ItemBuilder
import org.cristalise.test.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class ItemBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemorySetup()    }
    def cleanup() { cristalCleanup() }

    def 'ItemBuilder with empty Workflow adds Name to Properties'() {
        when:
        def itemB = ItemBuilder.build(name: "myFisrtItem", folder: "testing") {}

        then:
        assert itemB.props.list.size == 1
        assert itemB.props.list[0].name == "Name"
        assert itemB.props.list[0].value == "myFisrtItem"
    }

    def 'ItemBuilder builds domain Workflow'() {
        when:
        def itemB = ItemBuilder.build(name: "myFisrtItem", folder: "testing") { Workflow {} }

        then:
        assert itemB.wf
        assert itemB.wf.search("workflow/domain")
    }

}
