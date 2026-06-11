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
package org.cristalise.devtest.testAgentUseConstructor

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
 * TestAgentUseConstructor Item
 */

Schema('TestAgentUseConstructor', 0) {
  struct(name:' TestAgentUseConstructor', documentation: 'TestAgentUseConstructor aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('TestAgentUseConstructor_Details', 0) {
  struct(name: 'TestAgentUseConstructor_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )


  }
}


Activity('TestAgentUseConstructor_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($testAgentUseConstructor_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('TestAgentUseConstructor_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestAgentUseConstructorXML', 'java.lang.String')
  script('groovy', moduleDir+'/testAgentUseConstructor/script/TestAgentUseConstructor_Aggregate.groovy')
}

Script('TestAgentUseConstructor_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('TestAgentUseConstructorMap', 'java.util.Map')
  script('groovy', moduleDir+'/testAgentUseConstructor/script/TestAgentUseConstructor_QueryList.groovy')
}

Activity('TestAgentUseConstructor_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($testAgentUseConstructor_Schema)
  Script($testAgentUseConstructor_Aggregate_Script)
}



Workflow('TestAgentUseConstructor_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $testAgentUseConstructor_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

    }
  }
}



Item(name: 'TestAgentUseConstructorFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/TestAgentUseConstructors')



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
