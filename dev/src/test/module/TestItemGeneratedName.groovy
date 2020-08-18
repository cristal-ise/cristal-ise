import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * TestItemGeneratedName Item
 */

def TestItemGeneratedName = Schema('TestItemGeneratedName', 0) {
    struct(name:' TestItemGeneratedName', documentation: 'TestItemGeneratedName aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestItemGeneratedNameDetails = Schema('TestItemGeneratedName_Details', 0) {
    struct(name: 'TestItemGeneratedName_Details') {

        field(name: 'Name', type: 'string') { dynamicForms (disabled: true, label: 'ID') }

        field(name: 'Description', type: 'string')
    }
}


def TestItemGeneratedNameUpdateAct = Activity('TestItemGeneratedName_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestItemGeneratedNameDetails)
    //Script('CrudEntity_ChangeName', 0)
}

def TestItemGeneratedNameAggregateScript = Script('TestItemGeneratedName_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemGeneratedNameXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItemGeneratedName_Aggregate.groovy')
}

def TestItemGeneratedNameQueryListScript = Script('TestItemGeneratedName_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemGeneratedNameMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItemGeneratedName_QueryList.groovy')
}

Activity('TestItemGeneratedName_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestItemGeneratedName)
    Script(TestItemGeneratedNameAggregateScript)
}

def TestItemGeneratedNameWf = Workflow('TestItemGeneratedName_Workflow', 0) {
    ElemActDef(TestItemGeneratedNameUpdateAct)
    CompActDef('CrudState_Manage', 0)
}

def TestItemGeneratedNamePropDesc = PropertyDescriptionList('TestItemGeneratedName', 0) {
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

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/devtest/TestItemGeneratedName_Workflow') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/devtest/TestItemGeneratedName') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/devtest/TestItemGeneratedName_Aggregate') {
            Property('Version': 0)
        }
    }
}
