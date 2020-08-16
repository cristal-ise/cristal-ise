import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

/**
 * TestAgentUseConstructor Item
 */
def TestAgentUseConstructor = Schema('TestAgentUseConstructor', 0) {
    struct(name:' TestAgentUseConstructor', documentation: 'TestAgentUseConstructor aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestAgentUseConstructorDetails = Schema('TestAgentUseConstructor_Details', 0) {
    struct(name: 'TestAgentUseConstructor_Details') {

        field(name: 'Name', type: 'string')

        field(name: 'Description', type: 'string')
    }
}

def TestAgentUseConstructorUpdateAct = Activity('TestAgentUseConstructor_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestAgentUseConstructorDetails)
    //Script('Entity_ChangeName', 0)
}

def TestAgentUseConstructorAggregateScript = Script('TestAgentUseConstructor_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestAgentUseConstructorXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestAgentUseConstructor_Aggregate.groovy')
}

def TestAgentUseConstructorQueryListScript = Script('TestAgentUseConstructor_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestAgentUseConstructorMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestAgentUseConstructor_QueryList.groovy')
}

Activity('TestAgentUseConstructor_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestAgentUseConstructor)
    Script(TestAgentUseConstructorAggregateScript)
}

def TestAgentUseConstructorWf = Workflow('TestAgentUseConstructor_Workflow', 0) {
    ElemActDef(TestAgentUseConstructorUpdateAct)
    CompActDef('State_Manage', 0)
}

def TestAgentUseConstructorPropDesc = PropertyDescriptionList('TestAgentUseConstructor', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgentUseConstructor')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestAgentUseConstructorFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': 'devtest/TestAgentUseConstructors')



    InmutableProperty('CreateAgent': 'true')
    Property('DefaultRoles': 'Admin')



    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: '/desc/Schema/devtest/TestAgentUseConstructor_Details') {
            Property('Version': 0)
        }
    }


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestAgentUseConstructor_0.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/devtest/TestAgentUseConstructor_Workflow') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/devtest/TestAgentUseConstructor') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/devtest/TestAgentUseConstructor_Aggregate') {
            Property('Version': 0)
        }
    }
}
