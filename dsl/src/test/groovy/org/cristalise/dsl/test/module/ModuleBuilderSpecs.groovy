package org.cristalise.dsl.test.module

import org.cristalise.dsl.module.ModuleBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification

class ModuleBuilderSpecs extends Specification implements CristalTestSetup {

    def setup() { inMemoryServer() }

    def cleanup() { cristalCleanup() }

    def 'Module can reference existing resources'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Info(description: 'Test Cristal module', version: '1.0') {}
            Url('cristal/resources/')
            Config(name: 'Module.debug', value: false)

            Script('ServerNewEntity', 0)
            Schema('Item', 0)
            StateMachine('Default', 0)
            Activity('EditDefinition', 0)
            Workflow('ManageScript', 0)

            Item(name: "ScriptFactory", folder: "/", workflow: "ScriptFactoryWorkflow") {
                Property("Type": "Factory")
                Outcome(schema: "PropertyDescription", version: "0", viewname: "last", path: "boot/property/SCProp.xml")
                Dependency("workflow'") {
                    Properties {
                        Property("isDescription": false)
                    }
                    Member(itemPath: "/desc/ActivityDesc/kernel/ManageSchema") {
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
        module.getImports().list.size() == 8
        module.getConfig().size() == 1
        module.getInfo() != null
        module.resURL != null

        module.getImports().findImport('ServerNewEntity')
        module.getImports().findImport('Item')
        module.getImports().findImport('Default')
        module.getImports().findImport('EditDefinition')
        module.getImports().findImport('ManageScript')
        module.getImports().findImport('Test')
        module.getImports().findImport('Abort')
        module.getImports().findImport('ScriptFactory')

        module.config.get(0).name == "Module.debug"
        module.config.get(0).value == "false"

        //the order is important
        module.getImports().list[0].name == 'ServerNewEntity'
        module.getImports().list[1].name == 'Item'
        module.getImports().list[2].name == 'Default'
        module.getImports().list[3].name == 'EditDefinition'
        module.getImports().list[4].name == 'ManageScript'
        module.getImports().list[5].name == 'Test'
        module.getImports().list[6].name == 'ScriptFactory'
        module.getImports().list[7].name == 'Abort'

        module.getConfig().get(0).name == 'Module.debug'
        module.info.desc == 'Test Cristal module'
        module.resURL == 'cristal/resources/'
    }
}
