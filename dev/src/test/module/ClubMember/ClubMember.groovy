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
 * ClubMember Item
 */

Schema('ClubMember', 0) {
    struct(name:' ClubMember', documentation: 'ClubMember aggregated data') {
        field(name: 'Name', type: 'string')
        field(name: 'State', type: 'string', values: states)

        field(name: 'Email', type: 'string')

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
    script('groovy', moduleDir+'/ClubMember/script/Aggregate.groovy')
}

Script('ClubMember_QueryList', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('ClubMemberMap', 'java.util.Map')
    script('groovy', moduleDir+'/ClubMember/script/QueryList.groovy')
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
            LoopInfinite {
                ElemActDef('AddToCars', 'CrudEntity_ChangeDependecy', 0) {
                    Property((PREDEFINED_STEP): 'AddMembersToCollection')
                    Property((DEPENDENCY_NAME): 'Cars')
                    Property(ActivityDefName: 'CrudEntity_ChangeDependecy')
                    Property(ModuleNameSpace: 'devtest')
                }
            }
            LoopInfinite {
                ElemActDef('RemoveFromCars', 'CrudEntity_ChangeDependecy', 0) {
                    Property((PREDEFINED_STEP): 'RemoveMembersFromCollection')
                    Property((DEPENDENCY_NAME): 'Cars')
                    Property(ModuleNameSpace: 'devtest')
                    Property(ActivityDefName: 'CrudEntity_ChangeDependecy')
                }
            }
        }
    }
}


Workflow(name: 'ClubMember_ManageMotorcycles', version: 0) {
    Layout {
        AndSplit {
            LoopInfinite {
                ElemActDef('AddToMotorcycles', 'CrudEntity_ChangeDependecy', 0) {
                    Property((PREDEFINED_STEP): 'AddMembersToCollection')
                    Property((DEPENDENCY_NAME): 'Motorcycles')
                    Property(ActivityDefName: 'CrudEntity_ChangeDependecy')
                    Property(ModuleNameSpace: 'devtest')
                }
            }
            LoopInfinite {
                ElemActDef('RemoveFromMotorcycles', 'CrudEntity_ChangeDependecy', 0) {
                    Property((PREDEFINED_STEP): 'RemoveMembersFromCollection')
                    Property((DEPENDENCY_NAME): 'Motorcycles')
                    Property(ModuleNameSpace: 'devtest')
                    Property(ActivityDefName: 'CrudEntity_ChangeDependecy')
                }
            }
        }
    }
}



Workflow('ClubMember_Workflow', 0) {
    Layout {
        AndSplit {
            LoopInfinite { Act('Update', $clubMember_Update_ActivityDef)  }
            Block { CompActDef('CrudState_Manage', 0) }

            Block { Act($clubMember_ManageCars_CompositeActivityDef) }

            Block { Act($clubMember_ManageMotorcycles_CompositeActivityDef) }

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
