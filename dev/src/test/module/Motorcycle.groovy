import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.collection.BuiltInCollections.WORKFLOW
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

// this is defined in CrudState.groovy of the dev module
def states = ['ACTIVE', 'INACTIVE']

/**
 * Motorcycle Item
 */

Schema('Motorcycle', 0) {
    struct(name:' Motorcycle', documentation: 'Motorcycle aggregated data') {
        field(name: 'Name', type: 'string')
        field(name: 'State', type: 'string', values: states)

        field(name: 'RegistrationPlate', type: 'string')

    }
}

Schema('Motorcycle_Details', 0) {
    struct(name: 'Motorcycle_Details') {

        field(name: 'Name', type: 'string')


        field(name: 'RegistrationPlate',  type: 'string')

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
    script('groovy', moduleDir+'/script/Motorcycle_Aggregate.groovy')
}

Script('Motorcycle_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('MotorcycleMap', 'java.util.Map')
    script('groovy', moduleDir+'/script/Motorcycle_QueryList.groovy')
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
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/devtest/Motorcycles')





    InmutableProperty('UpdateSchema': 'Motorcycle_Details:0')


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
