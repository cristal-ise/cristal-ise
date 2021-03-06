import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * TestItemGeneratedName Item
 */

Schema('TestItemGeneratedName', 0) {
    struct(name:' TestItemGeneratedName', documentation: 'TestItemGeneratedName aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

Schema('TestItemGeneratedName_Details', 0) {
    struct(name: 'TestItemGeneratedName_Details') {

        field(name: 'Name', type: 'string') { dynamicForms (disabled: true, label: 'ID') }

        field(name: 'Description', type: 'string')
    }
}


Activity('TestItemGeneratedName_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema($testItemGeneratedName_Details_Schema)
    //Script('CrudEntity_ChangeName', 0)
}

Script('TestItemGeneratedName_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemGeneratedNameXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItemGeneratedName_Aggregate.groovy')
}

Script('TestItemGeneratedName_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemGeneratedNameMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItemGeneratedName_QueryList.groovy')
}

Activity('TestItemGeneratedName_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema($testItemGeneratedName_Schema)
    Script($testItemGeneratedName_Aggregate_Script)
}

Workflow('TestItemGeneratedName_Workflow', 0) {
    ElemActDef($testItemGeneratedName_Update_ActivityDef)
    CompActDef('CrudState_Manage', 0)
}

PropertyDescriptionList('TestItemGeneratedName', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemGeneratedName')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestItemGeneratedNameFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/TestItemGeneratedNames')

    InmutableProperty('IDPrefix': 'ID')
    Property('LeftPadSize': '6')





    InmutableProperty('UpdateSchema': 'TestItemGeneratedName_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemGeneratedName_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $testItemGeneratedName_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $testItemGeneratedName_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $testItemGeneratedName_Aggregate_Script) {
            Property('Version': 0)
        }
    }
}
