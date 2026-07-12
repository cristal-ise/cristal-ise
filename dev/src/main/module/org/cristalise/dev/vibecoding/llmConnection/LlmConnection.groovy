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
package org.cristalise.dev.vibecoding.llmConnection


import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.collection.Collection.Cardinality.OneToMany
import static org.cristalise.kernel.collection.Collection.Type.Bidirectional
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.UPDATE_SCHEMA_URN;

/**
 * LlmConnection Item
 */

Schema('LlmConnection', 0) {
  struct(name:' LlmConnection', documentation: 'LlmConnection aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'Description', 
      type: 'string'
    )
    field(
      name: 'ModelFamily', 
      type: 'string',
      values: ['Gemini','GPT','Llama','Mistral','Claude']
    )
    field(
      name: 'ModelName', 
      type: 'string'
    )
    field(
      name: 'ApiKey', 
      type: 'string'
    )
    field(
      name: 'Temperature', 
      type: 'decimal'
    )
    field(
      name: 'MaxOutputTokens', 
      type: 'integer'
    )
    field(
      name: 'Timeout', 
      type: 'integer'
    )
    field(
      name: 'SystemInstruction', 
      type: 'string'
    )
    field(
      name: 'AllowCodeExecution', 
      type: 'boolean'
    )
    field(
      name: 'LogRequests', 
      type: 'boolean'
    )
    field(
      name: 'LogResponses', 
      type: 'boolean'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('LlmConnection_Details', 0) {
  struct(name: 'LlmConnection_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'Description', 
      type: 'string'
    )
    field(
      name: 'ModelFamily', 
      type: 'string',
      values: ['Gemini','GPT','Llama','Mistral','Claude']
    )
    field(
      name: 'ModelName', 
      type: 'string'
    )
    field(
      name: 'ApiKey', 
      type: 'string'
    )
    field(
      name: 'Temperature', 
      type: 'decimal'
    )
    field(
      name: 'MaxOutputTokens', 
      type: 'integer'
    )
    field(
      name: 'Timeout', 
      type: 'integer'
    )
    field(
      name: 'SystemInstruction', 
      type: 'string'
    )
    field(
      name: 'AllowCodeExecution', 
      type: 'boolean'
    )
    field(
      name: 'LogRequests', 
      type: 'boolean'
    )
    field(
      name: 'LogResponses', 
      type: 'boolean'
    )


  }
}


Activity('LlmConnection_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($llmConnection_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('LlmConnection_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('LlmConnectionXML', 'java.lang.String')
  script('groovy', moduleDir+'/vibecoding/llmConnection/script/LlmConnection_Aggregate.groovy')
}

Script('LlmConnection_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('LlmConnectionMap', 'java.util.Map')
  script('groovy', moduleDir+'/vibecoding/llmConnection/script/LlmConnection_QueryList.groovy')
}

Activity('LlmConnection_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($llmConnection_Schema)
  Script($llmConnection_Aggregate_Script)
}





Workflow('LlmConnection_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $llmConnection_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }



    }
  }
}


Item(name: 'LlmConnectionFactory', version: 0, folder: '/vibecoding', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/vibecoding/LlmConnections')





  InmutableProperty((UPDATE_SCHEMA_URN): 'LlmConnection_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/LlmConnection_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $llmConnection_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $llmConnection_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $llmConnection_Aggregate_Script) {
      Property('Version': 0)
    }
  }

  
  DependencyDescription('VibeConversations') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): OneToMany.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'LlmConnection')
    }
    
    Member($vibeConversation_PropertyDescriptionList)
  }
  

}
