import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * ClubMember Item
 */

Schema('ClubMember', 0) {
    struct(name:' ClubMember', documentation: 'ClubMember aggregated data') {
        field(name: 'Name',  type: 'string')
        field(name: 'State', type: 'string', values: states)
      
        field(name: 'Email',  type: 'string')
      
    }
}

Schema('ClubMember_Details', 0) {
    struct(name: 'ClubMember_Details') {

        field(name: 'Name', type: 'string')

      
        field(name: 'Email',  type: 'string')
      
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
    script('groovy', moduleDir+'/script/ClubMember_Aggregate.groovy')
}

Script('ClubMember_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('ClubMemberMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/ClubMember_QueryList.groovy')
}

Activity('ClubMember_Aggregate', 0) {
    Property((OUTCOME_INIT): 'Empty')
    Property((AGENT_ROLE): 'UserCode')

    Schema($clubMember_Schema)
    Script($clubMember_Aggregate_Script)
}


Schema('ClubMember_Car', 0) {
    struct(name: 'ClubMember_Car', useSequence: true) {
        field(name: 'MemberName', type: 'string') {
            dynamicForms (label: 'Car')
        }
        struct(name: 'AddMembersToCollection', useSequence: true) {
            dynamicForms (hidden: true)
            anyField()
        }
    }
}

Activity('ClubMember_AddCar', 0) {
    Property((PREDEFINED_STEP): 'AddMembersToCollection')
    Property((DEPENDENCY_NAME): 'Cars')
    Property((DEPENDENCY_TO): 'Car')
    Property((DEPENDENCY_TYPE): 'Bidirectional')
    Property((OUTCOME_INIT): 'Empty')

    Schema($clubMember_Car_Schema)
}

Activity('ClubMember_RemoveCar', 0) {
    Property((PREDEFINED_STEP): 'RemoveSlotFromCollection')
    Property((DEPENDENCY_NAME): 'Cars')
    Property((DEPENDENCY_TO): 'Car')
    Property((DEPENDENCY_TYPE): 'Bidirectional')
    Property((OUTCOME_INIT): 'Empty')

    Schema($clubMember_Car_Schema)
}

Workflow(name: 'ClubMember_ManageCars', version: 0) {
    Layout {
        AndSplit {
            LoopInfinitive { Act('AddCar', $clubMember_AddCar_ActivityDef) }
            LoopInfinitive { Act('RemoveCar', $clubMember_RemoveCar_ActivityDef) }
        }
    }
}



Workflow('ClubMember_Workflow', 0) {
    Layout {
        AndSplit {
            LoopInfinitive { Act('Update', $clubMember_Update_ActivityDef)  }
            Block { CompActDef('CrudState_Manage', 0) }

            Block { Act($clubMember_ManageCars_CompositeActivityDef) }

        }
    }
}



Item(name: 'ClubMemberFactory', version: 0, folder: '/devtest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/ClubMembers')





    InmutableProperty('UpdateSchema': 'ClubMember_Details:0')


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
            Property((DEPENDENCY_CARDINALITY): OneToMany)
            Property((DEPENDENCY_TYPE): Bidirectional)
            Property((DEPENDENCY_TO): 'Car')
        }
        
        Member($car_PropertyDescriptionList)
    }
  

}
