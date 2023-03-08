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
package org.cristalise.dsl.test.module

import org.cristalise.dsl.module.ModuleBuilder
import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.entity.imports.ImportAgent
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.process.module.ModuleDomainContext
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.utils.LocalObjectLoader

import spock.lang.Specification

class ModuleBuilderSpecs extends Specification implements CristalTestSetup {

    def setupSpec() {
        def props = new Properties()
        props.put('DSL.Module.generateModuleXml', false)
        props.put('DSL.Module.generateResourceXml', false)
        props.put('DSL.Module.generateAllResourceItems', true)

        inMemoryServer(props, true) //skips bootstrap
    }
    def cleanupSpec() { cristalCleanup() }

    def 'Module can define Info, Url and Configs'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Info(description: 'Test Cristal module', version: '1.0') {
                dependencies: ['CristaliseDev', 'CristalTrigger']
            }
            Url('cristal/resources/')
            Config(name: 'Module.debug', value: false)
            Config(name: 'OverrideScriptLang.javascript', value: 'rhino')
        }

        then:
        module != null
        module.getImports().list.size() == 0
        module.getConfig().size() == 2
        module.getInfo() != null
        module.resURL != null

        module.config[0].name == 'Module.debug'
        module.config[0].value == 'false'
        module.config[1].name == 'OverrideScriptLang.javascript'
        module.config[1].value == 'rhino'
        module.info.desc == 'Test Cristal module'
        module.resURL == 'cristal/resources/'
        module.dependencies.size() == 2
        module.dependencies[0] == 'CristaliseDev'
        module.dependencies[1] == 'CristalTrigger'
    }

    def 'Module can reference existing resources'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Script('ServerNewEntity', 0)
            Schema('Item', 0)
            StateMachine('Default', 0)
            Activity('EditDefinition', 0)
            Workflow('ManageScript', 0)
        }

        then:
        module != null
        module.getImports().list.size() == 5

        module.getImports().findImport('ServerNewEntity', 'SC')
        module.getImports().findImport('Item', 'OD')
        module.getImports().findImport('Default', 'SM')
        module.getImports().findImport('EditDefinition', 'EA')
        module.getImports().findImport('ManageScript', 'CA')

        //the order is important
        module.getImports().list[0].name == 'ServerNewEntity'
        module.getImports().list[1].name == 'Item'
        module.getImports().list[2].name == 'Default'
        module.getImports().list[3].name == 'EditDefinition'
        module.getImports().list[4].name == 'ManageScript'
    }

    def 'Module can create new Item, Agent and Role keeping the order of creation'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Item(name: "ScriptFactory", folder: "/", workflow: "ScriptFactoryWorkflow", workflowVer: 0) {
                Property("Type": "Factory")
                Outcome(schema: "PropertyDescription", version: "0", viewname: "last", path: "boot/property/SCProp.xml")
                Dependency("workflow'") {
                    Member(itemPath: "/desc/ActivityDesc/kernel/ManageSchema") {
                        Property("Version": 0)
                    }
                }
                DependencyDescription('MasterOutcome') {
                    Member(itemPath: '/desc/ActivityDesc/kernel/ManageSchema') {
                        Property("Version": 0)
                    }
                }
            }

            Agent(folder: '/agents', name: 'Test', password: 'Test') {
                Roles {
                    Role(name: 'Admin')
                    Role(name: 'Abort')
                }
            }

            Roles {
                Role(name: 'Abort', jobList: false)
            }
        }

        then:
        module != null
        module.getImports().list.size() == 3

        module.getImports().findImport('Test', 'agent')
        module.getImports().findImport('Abort', 'role')
        module.getImports().findImport('ScriptFactory', 'item')

        //the order is important
        module.getImports().list[0].namespace == 'ttt'
        module.getImports().list[0].name == 'ScriptFactory'
        module.getImports().list[1].namespace == 'ttt'
        module.getImports().list[1].name == 'Test'
        module.getImports().list[2].namespace == 'ttt'
        module.getImports().list[2].name == 'Abort'
    }

    def 'Module can create new PropertyDescriptionList'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            PropertyDescriptionList('DummyProps', 0) {}
        }

        then:
        module != null
        module.getImports().list.size() == 1
        module.getImports().list[0].namespace == 'ttt'
        module.getImports().list[0].name == 'DummyProps'
    }

    def 'Module can create new Query'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Query('DummyQuery', 0) {
                parameter(name: 'root', type: 'java.lang.String')
                query(language: "dummy") {
                    new File('src/test/data/TestData.xsd').text // NOTE: content is not checked
                }
            }
        }

        then:
        module != null
        module.getImports().list.size() == 1
        module.getImports().list[0].name == 'DummyQuery'
        module.getImports().list[0].namespace == 'ttt'
    }

    def 'Module can create new Schema'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Schema("DummySchema", 0) {
                struct(name: 'DummyData') {}
            }
        }

        then:
        module != null
        module.getImports().list.size() == 1
        module.getImports().list[0].name == 'DummySchema'
        module.getImports().list[0].namespace == 'ttt'
    }

    def 'Module can create new Script'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Script("CounterScript", 0) {
                input("counter", "java.lang.String")
                output('java.lang.Integer')
                javascript { "new java.lang.Integer(counter % 2);" }
            }
        }

        then:
        module != null
        module.getImports().list.size() == 1
        module.getImports().list[0].name == 'CounterScript'
        module.getImports().list[0].namespace == 'ttt'
    }

    def 'Module can create new StateMachine'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            StateMachine("DummySM", 0) {
                state("Idle")
            }
        }

        then:
        module != null
        module.getImports().list.size() == 1
        module.getImports().list[0].name == 'DummySM'
        module.getImports().list[0].namespace == 'ttt'
    }

    def 'Module can create new DomainContext'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Contexts {
                DomainContext("/integtest", 0)
                DomainContext("/integtest/Doctor", 2)
            }
        }

        then:
        module != null
        module.getImports().list.size() == 2

        module.getImports().list[0] instanceof ModuleDomainContext
        def mdc = (ModuleDomainContext) module.getImports().list[0]
        mdc.name == 'IntegtestContext'
        mdc.namespace == 'ttt'
        mdc.version == 0

        module.getImports().list[1] instanceof ModuleDomainContext
        def mdc1 = (ModuleDomainContext) module.getImports().list[1]
        mdc1.name == 'IntegtestDoctorContext'
        mdc1.namespace == 'ttt'
        mdc1.version == 2
    }
}
