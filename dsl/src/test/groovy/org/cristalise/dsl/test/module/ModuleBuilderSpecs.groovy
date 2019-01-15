package org.cristalise.dsl.test.module

import org.cristalise.dsl.module.ModuleBuilder
import org.cristalise.kernel.entity.imports.ImportItem
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification

class ModuleBuilderSpecs extends Specification implements CristalTestSetup {

    def setupSpec() {
        def props = new Properties()
        props.put('DSL.GenerateModuleXml', false)
        inMemoryServer(8, props, false)
    }
    def cleanupSpec() { cristalCleanup() }

    def 'Module can define Info, Url and Configs'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Info(description: 'Test Cristal module', version: '1.0') {}
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

        module.getImports().findImport('ServerNewEntity')
        module.getImports().findImport('Item')
        module.getImports().findImport('Default')
        module.getImports().findImport('EditDefinition')
        module.getImports().findImport('ManageScript')

        //the order is important
        module.getImports().list[0].name == 'ServerNewEntity'
        module.getImports().list[1].name == 'Item'
        module.getImports().list[2].name == 'Default'
        module.getImports().list[3].name == 'EditDefinition'
        module.getImports().list[4].name == 'ManageScript'
    }

    def 'Module can create new Item, Agent and Role'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Item(name: "ScriptFactory", folder: "/", workflow: "ScriptFactoryWorkflow") {
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

            Agent(name: 'Test', password: 'Test') {
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
        
        module.getImports().findImport('Test')
        module.getImports().findImport('Abort')
        module.getImports().findImport('ScriptFactory')

        //the order is important
        module.getImports().list[0].name == 'ScriptFactory'
        module.getImports().list[1].name == 'Test'
        module.getImports().list[2].name == 'Abort'

        def item = (ImportItem)module.getImports().list[0]
        item.dependencyList.size() == 2
        item.dependencyList[0].name == 'workflow\''
        item.dependencyList[0].isDescription == false
        item.dependencyList[0].dependencyMemberList[0].itemPath == '/desc/ActivityDesc/kernel/ManageSchema'

        item.dependencyList[1].name == 'MasterOutcome'
        item.dependencyList[1].isDescription == true
        item.dependencyList[1].dependencyMemberList[0].itemPath == '/desc/ActivityDesc/kernel/ManageSchema'
    }
}
