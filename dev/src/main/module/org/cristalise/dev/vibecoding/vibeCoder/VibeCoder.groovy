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
package org.cristalise.dev.vibecoding.vibeCoder

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
 * VibeCoder Item
 */

Schema('VibeCoder', 0) {
  struct(name:' VibeCoder', documentation: 'VibeCoder aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'FistName', 
      type: 'string'
    )
    field(
      name: 'LastName', 
      type: 'string'
    )
    field(
      name: 'Email', 
      type: 'string'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('VibeCoder_Details', 0) {
  struct(name: 'VibeCoder_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'FistName', 
      type: 'string'
    )
    field(
      name: 'LastName', 
      type: 'string'
    )
    field(
      name: 'Email', 
      type: 'string'
    )


  }
}


Activity('VibeCoder_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($vibeCoder_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('VibeCoder_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('VibeCoderXML', 'java.lang.String')
  script('groovy', moduleDir+'/vibecoding/vibeCoder/script/VibeCoder_Aggregate.groovy')
}

Script('VibeCoder_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('VibeCoderMap', 'java.util.Map')
  script('groovy', moduleDir+'/vibecoding/vibeCoder/script/VibeCoder_QueryList.groovy')
}

Activity('VibeCoder_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($vibeCoder_Schema)
  Script($vibeCoder_Aggregate_Script)
}


Workflow(name: 'VibeCoder_ManageVibeConversations', version: 0) {
  Layout {
    AndSplit {
      LoopInfinitive {
        ElemActDef('AddToVibeConversations', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'AddMembersToCollection')
          Property((DEPENDENCY_NAME): 'VibeConversations')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'dev')
        }
      }
      LoopInfinitive {
        ElemActDef('RemoveFromVibeConversations', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'RemoveMembersFromCollection')
          Property((DEPENDENCY_NAME): 'VibeConversations')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'dev')
        }
      }
    }
  }
}



Workflow('VibeCoder_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $vibeCoder_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

      Block { Act($vibeCoder_ManageVibeConversations_CompositeActivityDef) }

    }
  }
}

Item(name: 'VibeCoderFactory', version: 0, folder: '/vibecoding', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/vibecoding/VibeCoders')



  InmutableProperty('CreateAgent': 'true')
  Property('DefaultRoles': 'Admin')



  InmutableProperty((UPDATE_SCHEMA): 'VibeCoder_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/VibeCoder_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $vibeCoder_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $vibeCoder_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $vibeCoder_Aggregate_Script) {
      Property('Version': 0)
    }
  }

  
  DependencyDescription('VibeConversations') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): ManyToMany.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'VibeCoders')
    }
    
    Member($vibeConversation_PropertyDescriptionList)
  }
  

}
