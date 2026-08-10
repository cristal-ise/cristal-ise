/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.devtest.testItem

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

/**
 * TestItem Item
 */

Schema('TestItem', 0) {
  struct(name:' TestItem', documentation: 'TestItem aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('TestItem_Details', 0) {
  struct(name: 'TestItem_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


  }
}


Activity('TestItem_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($testItem_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('TestItem_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestItemXML', 'java.lang.String')
  script('groovy', moduleDir+'/testItem/script/TestItem_Aggregate.groovy')
}

Script('TestItem_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestItemMap', 'java.util.Map')
  script('groovy', moduleDir+'/testItem/script/TestItem_QueryList.groovy')
}

Activity('TestItem_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($testItem_Schema)
  Script($testItem_Aggregate_Script)
}



Workflow('TestItem_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $testItem_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

    }
  }
}



Item(name: 'TestItemFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/TestItems')





  InmutableProperty((UPDATE_SCHEMA_URN): 'TestItem_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/TestItem_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $testItem_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $testItem_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $testItem_Aggregate_Script) {
      Property('Version': 0)
    }
  }

}
