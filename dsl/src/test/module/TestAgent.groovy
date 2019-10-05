import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

/**
 * TestAgent Item
 */
def TestAgent = Schema('TestAgent', 0) {
    struct(name:' TestAgent', documentation: 'TestAgent aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'ID',          type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestAgentDetails = Schema('TestAgent_Details', 0) {
    struct(name: 'TestAgent_Details') {
        field(name: 'Name',        type: 'string')
        field(name: 'Description', type: 'string')
    }
}

def TestAgentUpdateAct = Activity('TestAgent_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestAgentDetails)
    //Script('Entity_ChangeName', 0)
}

def TestAgentAggregateScript = Script('TestAgent_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestAgentXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestAgent_Aggregate.groovy')
}

def TestAgentQueryListScript = Script('TestAgent_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestAgentMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestAgent_QueryList.groovy')
}

Activity('TestAgent_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema(TestAgent)
    Script(TestAgentAggregateScript)
}

def TestAgentWf = Workflow('TestAgent_Workflow', 0) {
    ElemActDef(TestAgentUpdateAct)
    CompActDef('State_Manage', 0)
}

def TestAgentPropDesc = PropertyDescriptionList('TestAgent', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true, defaultValue: 'TestAgent')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: true, defaultValue: 'ACTIVE')
}

Item(name: 'TestAgentFactory', folder: '/', workflow: 'Factory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': 'testns/TestAgents')
    InmutableProperty('IDPrefix': 'ID')
    InmutableProperty('GeneratedName': 'false')
    Property('LeftPadSize': '6')


    InmutableProperty('CreateAgent': 'true')
    Property('DefaultRoles': 'Admin')




    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestAgent.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/testns/TestAgent_Workflow') {
            Property('Version': 0)
        }
    }


    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: '/desc/Schema/testns/TestAgent_Details') {
            Property('Version': 0)
        }
    }


    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/testns/TestAgent') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/testns/TestAgent_Aggregate') {
            Property('Version': 0)
        }
    }
}
