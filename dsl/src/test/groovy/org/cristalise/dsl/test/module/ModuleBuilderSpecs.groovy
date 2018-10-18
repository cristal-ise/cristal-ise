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
}
