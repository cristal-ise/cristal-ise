package org.cristalise.devtest.testItemExcel

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
 * TestItemExcel Item
 */

def xlsxFile = new File(moduleDir+'/TestItemExcel.xlsx')

Schema('TestItemExcel', 0, xlsxFile)
Schema('TestItemExcel_Details', 0, xlsxFile)



Activity('TestItemExcel_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($testItemExcel_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('TestItemExcel_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestItemExcelXML', 'java.lang.String')
  script('groovy', moduleDir+'/testItemExcel/script/TestItemExcel_Aggregate.groovy')
}

Script('TestItemExcel_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestItemExcelMap', 'java.util.Map')
  script('groovy', moduleDir+'/testItemExcel/script/TestItemExcel_QueryList.groovy')
}

Activity('TestItemExcel_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($testItemExcel_Schema)
  Script($testItemExcel_Aggregate_Script)
}



Workflow('TestItemExcel_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $testItemExcel_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

    }
  }
}



Item(name: 'TestItemExcelFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/TestItemExcels')





  InmutableProperty((UPDATE_SCHEMA): 'TestItemExcel_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItemExcel_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $testItemExcel_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $testItemExcel_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $testItemExcel_Aggregate_Script) {
      Property('Version': 0)
    }
  }

}
