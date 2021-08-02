import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * TestAgentUseConstructor Item
 */

Schema('TestAgentUseConstructor', 0) {
    struct(name:' TestAgentUseConstructor', documentation: 'TestAgentUseConstructor aggregated data') {
        field(name: 'Name',  type: 'string')
        field(name: 'State', type: 'string', values: states)
      
        field(name: 'Description', type: 'string')
      
    }
}

Schema('TestAgentUseConstructor_Details', 0) {
    struct(name: 'TestAgentUseConstructor_Details') {

        field(name: 'Name', type: 'string')

      
        field(name: 'Description', type: 'string')
      
    }
}


Activity('TestAgentUseConstructor_Update', 0) {
    Property('OutcomeInit': 'Empty')
    Schema($testAgentUseConstructor_Details_Schema)
    Script('CrudEntity_ChangeName', 0)
}

Script('TestAgentUseConstructor_Aggregate', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestAgentUseConstructorXML', 'java.lang.String')
    script('groovy', moduleDir+'/script/TestAgentUseConstructor_Aggregate.groovy')
}

Script('TestAgentUseConstructor_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('TestAgentUseConstructorMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/TestAgentUseConstructor_QueryList.groovy')
}

Activity('TestAgentUseConstructor_Aggregate', 0) {
    Property('OutcomeInit': 'Empty')
    Property('Agent Role': 'UserCode')

    Schema($testAgentUseConstructor_Schema)
    Script($testAgentUseConstructor_Aggregate_Script)
}

Workflow('TestAgentUseConstructor_Workflow', 0) {
    ElemActDef($testAgentUseConstructor_Update_ActivityDef)
    CompActDef('CrudState_Manage', 0)
}

PropertyDescriptionList('TestAgentUseConstructor', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'TestAgentUseConstructor')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

Item(name: 'TestAgentUseConstructorFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/TestAgentUseConstructors')



    InmutableProperty('CreateAgent': 'true')
    Property('DefaultRoles': 'Admin')



    Dependency(SCHEMA_INITIALISE) {
        Member(itemPath: $testAgentUseConstructor_Details_Schema) {
            Property('Version': 0)
        }
    }


    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestAgentUseConstructor_0.xml')

    Dependency(WORKFLOW) {
        Member(itemPath: $testAgentUseConstructor_Workflow_CompositeActivityDef) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: $testAgentUseConstructor_Schema) {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: $testAgentUseConstructor_Aggregate_Script) {
            Property('Version': 0)
        }
    }
}
