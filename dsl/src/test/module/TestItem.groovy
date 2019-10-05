import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

/**
 * TestItem Item
 */
def TestItem = Schema('TestItem', 0) {
    struct(name:' TestItem', documentation: 'TestItem aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'ID',          type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestItemDetails = Schema('TestItem_Details', 0) {
    struct(name: 'TestItem_Details') {
        field(name: 'Name',        type: 'string')
        field(name: 'Description', type: 'string')
    }
}

def TestItemUpdateAct = Activity('TestItem_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestItemDetails)
    //Script('Entity_ChangeName', 0)
}

def TestItemAggregateScript = Script('TestItem_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItem_Aggregate.groovy')
}

def TestItemQueryListScript = Script('TestItem_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItem_QueryList.groovy')
}

Activity('TestItem_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestItem)
    Script(TestItemAggregateScript)
}

def TestItemWf = Workflow('TestItem_Workflow', 0) {
    ElemActDef(TestItemUpdateAct)
    CompActDef('State_Manage', 0)
}

def TestItemPropDesc = PropertyDescriptionList('TestItem', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true, defaultValue: 'TestItem')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: true, defaultValue: 'ACTIVE')
}

Item(name: 'TestItemFactory', folder: '/', workflow: 'Factory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': 'testns/TestItems')
    InmutableProperty('IDPrefix': 'ID')
    InmutableProperty('GeneratedName': 'false')
    Property('LeftPadSize': '6')




    InmutableProperty('UpdateSchema': 'TestItem_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItem.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/testns/TestItem_Workflow') {
            Property('Version': 0)
        }
    }



    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/testns/TestItem') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/testns/TestItem_Aggregate') {
            Property('Version': 0)
        }
    }
}
