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
package org.cristalise.devtest.clubMember

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

/**
 * ClubMember Item
 */

Schema('ClubMember', 0) {
  struct(name:' ClubMember', documentation: 'ClubMember aggregated data', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'Email', 
      type: 'string',
      pattern: '[^@]+@[^\\.]+\\..+'
    )
    field(
      name: 'FavoriteCar', 
      type: 'string',
      multiplicity: '0..1'
    )
    field(
      name: 'FavoriteMotorcycle', 
      type: 'string',
      multiplicity: '0..1'
    )


    field(name: 'State', type: 'string', values: states)
  }
}

Schema('ClubMember_Details', 0) {
  struct(name: 'ClubMember_Details', useSequence: true) {
    field(
      name: 'Name', 
      type: 'string'
    )
    field(
      name: 'Email', 
      type: 'string',
      pattern: '[^@]+@[^\\.]+\\..+'
    )
    field(
      name: 'FavoriteCar', 
      type: 'string',
      multiplicity: '0..1'
    ) {
      reference(itemType: 'Car')
    }
    field(
      name: 'FavoriteMotorcycle', 
      type: 'string',
      multiplicity: '0..1'
    ) {
      reference(itemType: 'Motorcycle')
    }


  }
}


Activity('ClubMember_Update', 0) {
  Property((OUTCOME_INIT): 'Empty')

  Schema($clubMember_Details_Schema)
  Script('CrudEntity_ChangeName', 0)
}
Script('ClubMember_Aggregate', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('ClubMemberXML', 'java.lang.String')
  script('groovy', moduleDir+'/clubMember/script/ClubMember_Aggregate.groovy')
}

Script('ClubMember_QueryList', 0) {
  input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
  output('ClubMemberMap', 'java.util.Map')
  script('groovy', moduleDir+'/clubMember/script/ClubMember_QueryList.groovy')
}

Activity('ClubMember_Aggregate', 0) {
  Property((OUTCOME_INIT): 'Empty')
  Property((AGENT_ROLE): 'UserCode')

  Schema($clubMember_Schema)
  Script($clubMember_Aggregate_Script)
}


Workflow(name: 'ClubMember_ManageCars', version: 0) {
  Layout {
    AndSplit {
      LoopInfinitive {
        ElemActDef('AddToCars', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'AddMembersToCollection')
          Property((DEPENDENCY_NAME): 'Cars')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'devtest')
        }
      }
      LoopInfinitive {
        ElemActDef('RemoveFromCars', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'RemoveMembersFromCollection')
          Property((DEPENDENCY_NAME): 'Cars')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'devtest')
        }
      }
    }
  }
}


Workflow(name: 'ClubMember_ManageMotorcycles', version: 0) {
  Layout {
    AndSplit {
      LoopInfinitive {
        ElemActDef('AddToMotorcycles', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'AddMembersToCollection')
          Property((DEPENDENCY_NAME): 'Motorcycles')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'devtest')
        }
      }
      LoopInfinitive {
        ElemActDef('RemoveFromMotorcycles', 'CrudEntity_ChangeDependecy', 0) {
          Property((PREDEFINED_STEP): 'RemoveMembersFromCollection')
          Property((DEPENDENCY_NAME): 'Motorcycles')
          Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy')
          Property(ModuleNameSpace: 'devtest')
        }
      }
    }
  }
}



Workflow('ClubMember_Workflow', 0) {
  Layout {
    AndSplit {
      LoopInfinitive { Act('Update', $clubMember_Update_ActivityDef)  }
      Block { CompActDef('CrudState_Manage', 0) }

      Block { Act($clubMember_ManageCars_CompositeActivityDef) }

      Block { Act($clubMember_ManageMotorcycles_CompositeActivityDef) }

    }
  }
}



Item(name: 'ClubMemberFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
  InmutableProperty((TYPE): 'Factory')
  InmutableProperty((ROOT): '/devtest/ClubMembers')





  InmutableProperty((UPDATE_SCHEMA_URN): 'ClubMember_Details:0')


  Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/ClubMember_0.xml')

  Dependency(WORKFLOW) {
    Member(itemPath: $clubMember_Workflow_CompositeActivityDef) {
      Property('Version': 0)
    }
  }

  Dependency(MASTER_SCHEMA) {
    Member(itemPath: $clubMember_Schema) {
      Property('Version': 0)
    }
  }

  Dependency(AGGREGATE_SCRIPT) {
    Member(itemPath: $clubMember_Aggregate_Script) {
      Property('Version': 0)
    }
  }

  
  DependencyDescription('Cars') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): OneToMany.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'ClubMember')
    }
    
    Member($car_PropertyDescriptionList)
  }
  
  DependencyDescription('Motorcycles') {
    Properties {
      Property((DEPENDENCY_CARDINALITY): OneToMany.toString())
      Property((DEPENDENCY_TYPE): Bidirectional.toString())
      Property((DEPENDENCY_TO): 'ClubMember')
    }
    
    Member($motorcycle_PropertyDescriptionList)
  }
  

}
