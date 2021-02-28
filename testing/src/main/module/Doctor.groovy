import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.DEPENDENCY_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.OUTCOME_INIT
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PREDEFINED_STEP

PropertyDescriptionList('Doctor', 0) {
    PropertyDesc(name: 'Name',  isMutable: true,  isClassIdentifier: false)
    PropertyDesc(name: 'Type',  isMutable: false, isClassIdentifier: true,  defaultValue: 'Doctor')
    PropertyDesc(name: 'State', isMutable: true,  isClassIdentifier: false, defaultValue: 'ACTIVE')
}

def doctorPatientSchema = Schema("Doctor_Patient", 0) {
    struct(name: 'Doctor_Patient', useSequence: true) {
        field(name: 'MemberName', type: 'string') {
            dynamicForms (label: 'Patient')
        }
        struct(name: 'AddMembersToCollection', useSequence: true) {
            dynamicForms (hidden: true)
            anyField()
        }
    }
}

def doctorPatientScript = Script('Doctor_AddPatient', 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    script('groovy', 'src/main/module/script/Doctor_AddPatient.groovy')
}

def doctorAddPatient = Activity("Doctor_AddPatient", 0) {
    Property(OutcomeInit: "Empty")
    Property((PREDEFINED_STEP): "AddMembersToCollection")
    Property((DEPENDENCY_NAME): "Patients")
    Property((OUTCOME_INIT): "Empty")
    Schema(doctorPatientSchema)
    Script(doctorPatientScript)
}

def doctorWf = Workflow(name: "Doctor_Workflow", version: 0, generate: true) {
    ElemActDef('AddPatient', doctorAddPatient)
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
      Member($patient_PropertyDescriptionList) {
        Property("MemberUpdateSchema": "Doctor_Patient:0")
      }
    }
}

