import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * TestAgent Item
 */

def TestAgent = Schema('TestAgent', 0) {
    struct(name:' TestAgent', documentation: 'TestAgent aggregated data') {
        field(name: 'Name',        type: 'string')
        field(name: 'State',       type: 'string', values: states)
        field(name: 'Description', type: 'string')
    }
}

def TestAgentDetails = Schema('TestAgent_Details', 0) {
    struct(name: 'TestAgent_Details') {

        field(name: 'Name', type: 'string')

        field(name: 'Description', type: 'string')
    }
}


def TestAgentUpdateAct = Activity('TestAgent_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema(TestAgentDetails)
    //Script('CrudEntity_ChangeName', 0)
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
    CompActDef('CrudState_Manage', 0)
}

def TestAgentPropDesc = PropertyDescriptionList('TestAgent', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgent')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestAgentFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': 'devtest/TestAgents')



    InmutableProperty('CreateAgent': 'true')
    Property('DefaultRoles': 'Admin')



    InmutableProperty('UpdateSchema': 'TestAgent_Details:0')


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestAgent_0.xml')

    Dependency('workflow') {
        Member(itemPath: '/desc/ActivityDesc/devtest/TestAgent_Workflow') {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/devtest/TestAgent') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/devtest/TestAgent_Aggregate') {
            Property('Version': 0)
        }
    }
}
