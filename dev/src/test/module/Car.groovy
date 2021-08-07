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
 * Car Item
 */

Schema('Car', 0) {
    struct(name:' Car', documentation: 'Car aggregated data') {
        field(name: 'Name',  type: 'string')
        field(name: 'State', type: 'string', values: states)
      
        field(name: 'RegistrationPlate',  type: 'string')
      
    }
}

Schema('Car_Details', 0) {
    struct(name: 'Car_Details') {

        field(name: 'Name', type: 'string')

      
        field(name: 'RegistrationPlate',  type: 'string')
      
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
    script('groovy', moduleDir+'/script/Car_Aggregate.groovy')
}

Script('Car_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('CarMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/Car_QueryList.groovy')
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
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/Cars')





    InmutableProperty('UpdateSchema': 'Car_Details:0')


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

  
    DependencyDescription('ClubMembers') {
        Properties {
            Property((DEPENDENCY_CARDINALITY): ManyToOne)
            Property((DEPENDENCY_TYPE): Bidirectional)
            Property((DEPENDENCY_TO): 'ClubMember')
        }
        
        Member($clubMember_PropertyDescriptionList)
    }
  

}
