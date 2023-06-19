package org.cristalise.devtest.testItemUseConstructor

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

/**
 * TestItemUseConstructor Item
 */

Schema('TestItemUseConstructor', 0) {
    struct(name:' TestItemUseConstructor', documentation: 'TestItemUseConstructor aggregated data') {
        field(name: 'Name', type: 'string')
        field(name: 'State', type: 'string', values: states)

        field(name: 'Description', type: 'string')

    }
}

Schema('TestItemUseConstructor_Details', 0) {
    struct(name: 'TestItemUseConstructor_Details') {

        field(name: 'Name', type: 'string')


        field(name: 'Description', type: 'string')

    }
}


Activity('TestItemUseConstructor_Update', 0) {
    Property((OUTCOME_INIT): 'Empty')

    Schema($testItemUseConstructor_Details_Schema)
    Script('CrudEntity_ChangeName', 0)
}

Script('TestItemUseConstructor_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorXML', 'java.lang.String')
    script('groovy', moduleDir+'/testItemUseConstructor/script/TestItemUseConstructor_Aggregate.groovy')
}

Script('TestItemUseConstructor_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorMap', 'java.util.Map')
    script('groovy', moduleDir+'/testItemUseConstructor/script/TestItemUseConstructor_QueryList.groovy')
}

Activity('TestItemUseConstructor_Aggregate', 0) {
    Property((OUTCOME_INIT): 'Empty')
    Property((AGENT_ROLE): 'UserCode')

    Schema($testItemUseConstructor_Schema)
    Script($testItemUseConstructor_Aggregate_Script)
}



Workflow('TestItemUseConstructor_Workflow', 0) {
    Layout {
        AndSplit {
            LoopInfinitive { Act('Update', $testItemUseConstructor_Update_ActivityDef)  }
            Block { CompActDef('CrudState_Manage', 0) }

        }
    }
}



Item(name: 'TestItemUseConstructorFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/TestItemUseConstructors')





    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: $testItemUseConstructor_Details_Schema) {
            Property('Version': 0)
        }
    }


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemUseConstructor_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $testItemUseConstructor_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $testItemUseConstructor_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $testItemUseConstructor_Aggregate_Script) {
            Property('Version': 0)
        }
    }

}
