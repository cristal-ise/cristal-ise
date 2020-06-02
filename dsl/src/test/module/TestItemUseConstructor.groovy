import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

/**
 * TestItemUseConstructor Item
 */
def TestItemUseConstructor = Schema('TestItemUseConstructor', 0) {
    struct(name:' TestItemUseConstructor', documentation: 'TestItemUseConstructor aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestItemUseConstructorDetails = Schema('TestItemUseConstructor_Details', 0) {
    struct(name: 'TestItemUseConstructor_Details') {

        field(name: 'Name', type: 'string')

        field(name: 'Description', type: 'string')
    }
}

def TestItemUseConstructorUpdateAct = Activity('TestItemUseConstructor_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestItemUseConstructorDetails)
    //Script('Entity_ChangeName', 0)
}

def TestItemUseConstructorAggregateScript = Script('TestItemUseConstructor_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestItemUseConstructor_Aggregate.groovy')
}

def TestItemUseConstructorQueryListScript = Script('TestItemUseConstructor_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestItemUseConstructorMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestItemUseConstructor_QueryList.groovy')
}

Activity('TestItemUseConstructor_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestItemUseConstructor)
    Script(TestItemUseConstructorAggregateScript)
}

def TestItemUseConstructorWf = Workflow('TestItemUseConstructor_Workflow', 0) {
    ElemActDef(TestItemUseConstructorUpdateAct)
    CompActDef('State_Manage', 0)
}

def TestItemUseConstructorPropDesc = PropertyDescriptionList('TestItemUseConstructor', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestItemUseConstructor')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestItemUseConstructorFactory', folder: '/dsl', workflow: 'Factory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': 'dsl/TestItemUseConstructors')





    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: '/desc/Schema/dsl/TestItemUseConstructor_Details') {
            Property('Version': 0)
        }
    }


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemUseConstructor_0.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/dsl/TestItemUseConstructor_Workflow') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/dsl/TestItemUseConstructor') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/dsl/TestItemUseConstructor_Aggregate') {
            Property('Version': 0)
        }
    }
}
