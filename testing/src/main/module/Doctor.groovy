import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*


def doctorWf = Workflow(name: "Doctor_Workflow", version: 0, generate: true) {
    Layout {
        LoopInfinitive {
            ElemActDef('AddPatient', 'CrudEntity_ChangeDependecy', 0) {
                Property((PREDEFINED_STEP): 'AddMembersToCollection')
                Property((DEPENDENCY_NAME): 'Patients')
                Property((ACTIVITY_DEF_NAME): 'CrudEntity_ChangeDependecy') //
                Property(ModuleNameSpace: 'integTest')
            }
        }
    }
}

Item(name: 'DoctorFactory', version: 0, folder: '/integTest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/integTest/Doctors')

    Outcome($doctor_PropertyDescriptionList)

    Dependency('workflow') {
        Member(doctorWf) {
            Property('Version': 0)
        }
    }

    DependencyDescription('Patients') {
        Properties {
            Property((DEPENDENCY_CARDINALITY): OneToMany.toString())
            Property((DEPENDENCY_TYPE): Bidirectional.toString())
            Property((DEPENDENCY_TO): 'Doctor')
        }
        Member($patient_PropertyDescriptionList)
    }
}

