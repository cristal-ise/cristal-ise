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
        thrown AssertionError
    }

    def 'Item is built without workflow adds Name to Properties'() {
        when:
        def item = ItemBuilder.build(name: "dummy", folder: "testing") {}

        then:
        item.properties.size == 1
        item.properties[0].name == "Name"
        item.properties[0].value == "dummy"
    }

    def 'Item is built without workflow can have user defined Properties'() {
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

        !item.wf
    }

    def 'Item is built with empty domain Workflow'() {
        when:
        def item = ItemBuilder.build(name: "myFisrtItem", folder: "testing") { Workflow {} }

        then:
        item.wf
        item.wf.search("workflow/domain")

        item.properties.size == 1
    }

    def 'Item is built with specifing workfflow name and version'() {
        when:
        def item = ItemBuilder.build(name: "myFisrtItem", folder: "testing", workflow: "TestWorkflow", workflowVer: 1) {
            InmutableProperty("Brain": "kovax")
            Property("Pinky": "kovax")
        }

        then:
        item.workflow == "TestWorkflow"
        item.workflowVer == 1
        
        item.properties.size == 3
    }

    def 'Item is built without workflow and a single Outcome'() {
        when:
        def item = ItemBuilder.build(name: "testItem", folder: "test") {
            Outcome(schema: "PropertyDescription", version: "0", viewname: "last", path: "boot/property/Test.xml")
        }

        then:
        item.outcomes.size == 1
        item.outcomes.get(0).schema == "PropertyDescription"
        item.outcomes.get(0).version == 0
        item.outcomes.get(0).viewname == "last"
        item.outcomes.get(0).path == "boot/property/Test.xml"
    }

    def 'Item is built without workflow and a Dependency to another Item'() {
        when:
        def item = ItemBuilder.build(name: "testItem", folder: "test") {
            Dependency('workflow') {
                Member(itemPath: "/desc/ActivityDesc/domain/TestWorkflow") {
                    Property("Version": 0)
                }
            }
        }

        then:
        item.dependencyList.size == 1
        item.dependencyList.get(0).name == "workflow"
        item.dependencyList.get(0).isDescription == false
        item.dependencyList.get(0).dependencyMemberList.size == 1
        item.dependencyList.get(0).dependencyMemberList.get(0).itemPath == "/desc/ActivityDesc/domain/TestWorkflow"
        item.dependencyList.get(0).dependencyMemberList.get(0).props.size() == 1
        item.dependencyList.get(0).dependencyMemberList.get(0).props.get("Version") == 0
    }

    def 'Item is built without workflow and a DependencyDescription to another Item'() {
        when:
        def item = ItemBuilder.build(name: "testItem", folder: "test") {
            DependencyDescription('workflow') {
                Member(itemPath: "/desc/ActivityDesc/domain/TestWorkflow") {
                    Property("Version": 0)
                }
            }
        }

        then:
        item.dependencyList.size == 1
        item.dependencyList.get(0).name == "workflow"
        item.dependencyList.get(0).isDescription == true
        item.dependencyList.get(0).dependencyMemberList.size == 1
        item.dependencyList.get(0).dependencyMemberList.get(0).itemPath == "/desc/ActivityDesc/domain/TestWorkflow"
        item.dependencyList.get(0).dependencyMemberList.get(0).props.size() == 1
        item.dependencyList.get(0).dependencyMemberList.get(0).props.get("Version") == 0
    }
}
