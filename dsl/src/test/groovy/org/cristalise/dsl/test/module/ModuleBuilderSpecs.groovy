package org.cristalise.dsl.test.module

import org.cristalise.dsl.module.ModuleBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.process.module.Module
import org.cristalise.kernel.test.utils.CristalTestSetup
import org.cristalise.kernel.utils.Logger

import spock.lang.Specification

class ModuleBuilderSpecs  extends Specification implements CristalTestSetup {

    def setup()   { inMemorySetup() }
    def cleanup() { cristalCleanup() }

    def 'Module can reference existing resources'() {
        when:
        def module = ModuleBuilder.build('ttt', 'integtest', 0) {
            Script('ServerNewEntity', 0)
            Schema('Item', 0)
            StateMachine('Default', 0)
            Activity('EditDefinition', 0)
            Workflow('ManageScript', 0)
            Agent(name: 'Test', password: 'test'){
                Roles{
                    Role(name: 'Admin')
                    Role(name: 'Abort')
                }
            }
            Roles {
                Role(name: 'Abort', jobList: false)
            }
            Config(name: 'Module.debug', value: false)
            Info(description: 'Test Cristal module', version: '1.0'){}
            Url('cristal/resources/')
        }

        then:
        module != null
        module.getImports().list.size() == 7
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

        module.getConfig().find{
            it.name == 'Module.debug'
        }

        //the order is important
        module.getImports().list[0].name == 'ServerNewEntity'
        module.getImports().list[1].name == 'Item'
        module.getImports().list[2].name == 'Default'
        module.getImports().list[3].name == 'EditDefinition'
        module.getImports().list[4].name == 'ManageScript'
        module.getImports().list[5].name == 'Test'
        module.getImports().list[6].name == 'Abort'

        module.getConfig().get(0).name == 'Module.debug'
        module.info.desc == 'Test Cristal module'
        module.resURL == 'cristal/resources/'
    }
}
