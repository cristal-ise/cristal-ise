package org.cristalise.devtest.testAgent

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

/**
 * TestAgent Item
 */

Schema('TestAgent', 0) {
  struct(name:' TestAgent', documentation: 'TestAgent aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('TestAgent_Details', 0) {
  struct(name: 'TestAgent_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


  }
}


Activity('TestAgent_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($testAgent_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('TestAgent_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestAgentXML', 'java.lang.String')
  script('groovy', moduleDir+'/testAgent/script/TestAgent_Aggregate.groovy')
}

Script('TestAgent_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestAgentMap', 'java.util.Map')
  script('groovy', moduleDir+'/testAgent/script/TestAgent_QueryList.groovy')
}

Activity('TestAgent_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($testAgent_Schema)
  Script($testAgent_Aggregate_Script)
}



Workflow('TestAgent_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $testAgent_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

    }
  }
}



Item(name: 'TestAgentFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/TestAgents')



  InmutableProperty('CreateAgent': 'true')
  Property('DefaultRoles': 'Admin')



  InmutableProperty((UPDATE_SCHEMA): 'TestAgent_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestAgent_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $testAgent_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $testAgent_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $testAgent_Aggregate_Script) {
      Property('Version': 0)
    }
  }

}
