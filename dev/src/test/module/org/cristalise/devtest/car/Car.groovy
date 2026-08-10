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
package org.cristalise.devtest.car

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

/**
 * Car Item
 */

Schema('Car', 0) {
  struct(name:' Car', documentation: 'Car aggregated data', useSequence: true) {
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
      values: ['BMW','Audi','Mercedes']
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('Car_Details', 0) {
  struct(name: 'Car_Details', useSequence: true) {
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
      values: ['BMW','Audi','Mercedes']
    )


  }
}


Activity('Car_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($car_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('Car_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('CarXML', 'java.lang.String')
  script('groovy', moduleDir+'/car/script/Car_Aggregate.groovy')
}

Script('Car_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('CarMap', 'java.util.Map')
  script('groovy', moduleDir+'/car/script/Car_QueryList.groovy')
}

Activity('Car_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($car_Schema)
  Script($car_Aggregate_Script)
}





Workflow('Car_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $car_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }



    }
  }
}



Item(name: 'CarFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/Cars')





  InmutableProperty((UPDATE_SCHEMA_URN): 'Car_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/Car_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $car_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $car_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $car_Aggregate_Script) {
      Property('Version': 0)
    }
  }

  
  DependencyDescription('ClubMember') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): ManyToOne.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'Cars')
    }
    
    Member($clubMember_PropertyDescriptionList)
  }
  

}
