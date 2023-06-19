package org.cristalise.devtest.testItemUseConstructorGeneratedName

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
 * TestItemUseConstructorGeneratedName Item
 */

Schema('TestItemUseConstructorGeneratedName', 0) {
    struct(name:' TestItemUseConstructorGeneratedName', documentation: 'TestItemUseConstructorGeneratedName aggregated data') {
        field(name: 'Name', type: 'string')
        field(name: 'State', type: 'string', values: states)

        field(name: 'Description', type: 'string')

    }
}

Schema('TestItemUseConstructorGeneratedName_Details', 0) {
    struct(name: 'TestItemUseConstructorGeneratedName_Details') {

        field(name: 'Name', type: 'string') { dynamicForms (disabled: true, label: 'ID') }


        field(name: 'Description', type: 'string')

    }
}


Activity('TestItemUseConstructorGeneratedName_Update', 0) {
    Property((OUTCOME_INIT): 'Empty')

    Schema($testItemUseConstructorGeneratedName_Details_Schema)
    Script('CrudEntity_ChangeName', 0)
}

Script('TestItemUseConstructorGeneratedName_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorGeneratedNameXML', 'java.lang.String')
    script('groovy', moduleDir+'/testItemUseConstructorGeneratedName/script/TestItemUseConstructorGeneratedName_Aggregate.groovy')
}

Script('TestItemUseConstructorGeneratedName_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorGeneratedNameMap', 'java.util.Map')
    script('groovy', moduleDir+'/testItemUseConstructorGeneratedName/script/TestItemUseConstructorGeneratedName_QueryList.groovy')
}

Activity('TestItemUseConstructorGeneratedName_Aggregate', 0) {
    Property((OUTCOME_INIT): 'Empty')
    Property((AGENT_ROLE): 'UserCode')

    Schema($testItemUseConstructorGeneratedName_Schema)
    Script($testItemUseConstructorGeneratedName_Aggregate_Script)
}



Workflow('TestItemUseConstructorGeneratedName_Workflow', 0) {
    Layout {
        AndSplit {
            LoopInfinitive { Act('Update', $testItemUseConstructorGeneratedName_Update_ActivityDef)  }
            Block { CompActDef('CrudState_Manage', 0) }

        }
    }
}



Item(name: 'TestItemUseConstructorGeneratedNameFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/TestItemUseConstructorGeneratedNames')

    InmutableProperty((ID_PREFIX): 'ID')
    InmutableProperty((LEFT_PAD_SIZE): '6')
    Property((LAST_COUNT): '0')





    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: $testItemUseConstructorGeneratedName_Details_Schema) {
            Property('Version': 0)
        }
    }


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemUseConstructorGeneratedName_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $testItemUseConstructorGeneratedName_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $testItemUseConstructorGeneratedName_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $testItemUseConstructorGeneratedName_Aggregate_Script) {
            Property('Version': 0)
        }
    }

}
