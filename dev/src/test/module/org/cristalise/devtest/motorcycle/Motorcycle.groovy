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
package org.cristalise.devtest.motorcycle

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

/**
 * Motorcycle Item
 */

Schema('Motorcycle', 0) {
  struct(name:' Motorcycle', documentation: 'Motorcycle aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'RegistrationPlate', 
      type: 'string'
    )
    field(
      name: 'Make', 
      type: 'string',
      values: ['BMW','Suzuki']
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('Motorcycle_Details', 0) {
  struct(name: 'Motorcycle_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'RegistrationPlate', 
      type: 'string'
    )
    field(
      name: 'Make', 
      type: 'string',
      values: ['BMW','Suzuki']
    )


  }
}


Activity('Motorcycle_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($motorcycle_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('Motorcycle_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('MotorcycleXML', 'java.lang.String')
  script('groovy', moduleDir+'/motorcycle/script/Motorcycle_Aggregate.groovy')
}

Script('Motorcycle_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('MotorcycleMap', 'java.util.Map')
  script('groovy', moduleDir+'/motorcycle/script/Motorcycle_QueryList.groovy')
}

Activity('Motorcycle_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($motorcycle_Schema)
  Script($motorcycle_Aggregate_Script)
}





Workflow('Motorcycle_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $motorcycle_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }



    }
  }
}



Item(name: 'MotorcycleFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/Motorcycles')





  InmutableProperty((UPDATE_SCHEMA_URN): 'Motorcycle_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/Motorcycle_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $motorcycle_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $motorcycle_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $motorcycle_Aggregate_Script) {
      Property('Version': 0)
    }
  }

  
  DependencyDescription('ClubMember') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): ManyToOne.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'Motorcycles')
    }
    
    Member($clubMember_PropertyDescriptionList)
  }
  

}
