import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * TestItem Item
 */

Schema('TestItem', 0) {
    struct(name:' TestItem', documentation: 'TestItem aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

Schema('TestItem_Details', 0) {
    struct(name: 'TestItem_Details') {

        field(name: 'Name', type: 'string')

        field(name: 'Description', type: 'string')
    }
}


Activity('TestItem_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema($testItem_Details_Schema)
    //Script('CrudEntity_ChangeName', 0)
}

Script('TestItem_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItem_Aggregate.groovy')
}

Script('TestItem_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItem_QueryList.groovy')
}

Activity('TestItem_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema($testItem_Schema)
    Script($testItem_Aggregate_Script)
}

Workflow('TestItem_Workflow', 0) {
    ElemActDef($testItem_Update_ActivityDef)
    CompActDef('CrudState_Manage', 0)
}

PropertyDescriptionList('TestItem', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItem')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestItemFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/TestItems')





    InmutableProperty('UpdateSchema': 'TestItem_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItem_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $testItem_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $testItem_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $testItem_Aggregate_Script) {
            Property('Version': 0)
        }
    }
}
