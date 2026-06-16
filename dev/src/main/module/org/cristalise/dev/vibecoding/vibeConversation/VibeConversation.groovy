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
package org.cristalise.dev.vibecoding.vibeConversation

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
 * VibeConversation Item
 */

Schema('VibeConversation', 0) {
  struct(name:' VibeConversation', documentation: 'VibeConversation aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'Description', 
      type: 'string'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('VibeConversation_Details', 0) {
  struct(name: 'VibeConversation_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'Description', 
      type: 'string'
    )


  }
}


Activity('VibeConversation_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($vibeConversation_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('VibeConversation_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('VibeConversationXML', 'java.lang.String')
  script('groovy', moduleDir+'/vibecoding/vibeConversation/script/VibeConversation_Aggregate.groovy')
}

Script('VibeConversation_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('VibeConversationMap', 'java.util.Map')
  script('groovy', moduleDir+'/vibecoding/vibeConversation/script/VibeConversation_QueryList.groovy')
}

Activity('VibeConversation_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($vibeConversation_Schema)
  Script($vibeConversation_Aggregate_Script)
}


Workflow(name: 'VibeConversation_ManageLlmConnection', version: 0) {
  Layout {
    AndSplit {
      LoopInfinitive {
        ElemActDef('AddToLlmConnection', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'AddMembersToCollection')
          Property((DEPENDENCY_NAME): 'LlmConnection')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'dev')
        }
      }
      LoopInfinitive {
        ElemActDef('RemoveFromLlmConnection', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'RemoveMembersFromCollection')
          Property((DEPENDENCY_NAME): 'LlmConnection')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'dev')
        }
      }
    }
  }
}





Workflow('VibeConversation_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $vibeConversation_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

      Block { Act($vibeConversation_ManageLlmConnection_CompositeActivityDef) }



    }
  }
}

Item(name: 'VibeConversationFactory', version: 0, folder: '/vibecoding', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/vibecoding/VibeConversations')





  InmutableProperty((UPDATE_SCHEMA): 'VibeConversation_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/VibeConversation_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $vibeConversation_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $vibeConversation_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $vibeConversation_Aggregate_Script) {
      Property('Version': 0)
    }
  }

  
  DependencyDescription('LlmConnection') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): ManyToOne.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'VibeConversations')
    }
    
    Member($llmConnection_PropertyDescriptionList)
  }
  
  DependencyDescription('VibeCoders') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): ManyToMany.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'VibeConversations')
    }
    
    Member($vibeCoder_PropertyDescriptionList)
  }
  

}
