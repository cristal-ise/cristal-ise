import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

/**
 * TestItemUseConstructorGeneratedName Item
 */
def TestItemUseConstructorGeneratedName = Schema('TestItemUseConstructorGeneratedName', 0) {
    struct(name:' TestItemUseConstructorGeneratedName', documentation: 'TestItemUseConstructorGeneratedName aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestItemUseConstructorGeneratedNameDetails = Schema('TestItemUseConstructorGeneratedName_Details', 0) {
    struct(name: 'TestItemUseConstructorGeneratedName_Details') {

        field(name: 'Name', type: 'string') { dynamicForms (disabled: true, label: 'ID') }

        field(name: 'Description', type: 'string')
    }
}

def TestItemUseConstructorGeneratedNameUpdateAct = Activity('TestItemUseConstructorGeneratedName_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestItemUseConstructorGeneratedNameDetails)
    //Script('Entity_ChangeName', 0)
}

def TestItemUseConstructorGeneratedNameAggregateScript = Script('TestItemUseConstructorGeneratedName_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorGeneratedNameXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItemUseConstructorGeneratedName_Aggregate.groovy')
}

def TestItemUseConstructorGeneratedNameQueryListScript = Script('TestItemUseConstructorGeneratedName_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorGeneratedNameMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItemUseConstructorGeneratedName_QueryList.groovy')
}

Activity('TestItemUseConstructorGeneratedName_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestItemUseConstructorGeneratedName)
    Script(TestItemUseConstructorGeneratedNameAggregateScript)
}

def TestItemUseConstructorGeneratedNameWf = Workflow('TestItemUseConstructorGeneratedName_Workflow', 0) {
    ElemActDef(TestItemUseConstructorGeneratedNameUpdateAct)
    CompActDef('State_Manage', 0)
}

def TestItemUseConstructorGeneratedNamePropDesc = PropertyDescriptionList('TestItemUseConstructorGeneratedName', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemUseConstructorGeneratedName')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestItemUseConstructorGeneratedNameFactory', folder: '/testns', workflow: 'Factory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': 'testns/TestItemUseConstructorGeneratedNames')

    InmutableProperty('IDPrefix': 'ID')
    Property('LeftPadSize': '6')





    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: '/desc/Schema/testns/TestItemUseConstructorGeneratedName_Details') {
            Property('Version': 0)
        }
    }


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemUseConstructorGeneratedName.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/testns/TestItemUseConstructorGeneratedName_Workflow') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/testns/TestItemUseConstructorGeneratedName') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/testns/TestItemUseConstructorGeneratedName_Aggregate') {
            Property('Version': 0)
        }
    }
}
