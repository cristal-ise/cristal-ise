package org.cristalise.devtest.testItemGeneratedName

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
 * TestItemGeneratedName Item
 */

Schema('TestItemGeneratedName', 0) {
  struct(name:' TestItemGeneratedName', documentation: 'TestItemGeneratedName aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('TestItemGeneratedName_Details', 0) {
  struct(name: 'TestItemGeneratedName_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    ) {
      dynamicForms (disabled: true, label: 'ID')
    }


  }
}


Activity('TestItemGeneratedName_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($testItemGeneratedName_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('TestItemGeneratedName_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestItemGeneratedNameXML', 'java.lang.String')
  script('groovy', moduleDir+'/testItemGeneratedName/script/TestItemGeneratedName_Aggregate.groovy')
}

Script('TestItemGeneratedName_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestItemGeneratedNameMap', 'java.util.Map')
  script('groovy', moduleDir+'/testItemGeneratedName/script/TestItemGeneratedName_QueryList.groovy')
}

Activity('TestItemGeneratedName_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($testItemGeneratedName_Schema)
  Script($testItemGeneratedName_Aggregate_Script)
}



Workflow('TestItemGeneratedName_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $testItemGeneratedName_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

    }
  }
}



Item(name: 'TestItemGeneratedNameFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/TestItemGeneratedNames')

  InmutableProperty((ID_PREFIX): 'ID')
  InmutableProperty((LEFT_PAD_SIZE): '6')
  Property((LAST_COUNT): '0')





  InmutableProperty((UPDATE_SCHEMA): 'TestItemGeneratedName_Details:0')


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
