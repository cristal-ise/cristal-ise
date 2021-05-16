import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.collection.BuiltInCollections.AGGREGATE_SCRIPT
import static org.cristalise.kernel.collection.BuiltInCollections.MASTER_SCHEMA
import static org.cristalise.kernel.collection.BuiltInCollections.SCHEMA_INITIALISE
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*

def detailsSchema = Schema("Patient_Details", 0) {
    struct(name: 'PatientDetails') {
        attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
        field(name: 'DateOfBirth', type: 'date')
        field(name: 'Gender',      type: 'string', values: ['male', 'female'])
        field(name: 'Weight',      type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
    }
}

def setDetailsEA = Activity("Patient_SetDetails", 0) {
    Property(OutcomeInit: "Empty")
    Schema(detailsSchema)
}

def urinalysisSchema =  Schema("Patient_UrinSample", 0) {
    struct(name: 'UrinSample') {
        field(name: 'Transparency', type: 'string', values: ['clear', 'clouded'])
        field(name: 'Color',        type: 'string')
    }
}

def urinalysisEA = Activity("Patient_SetUrinSample", 0) {
    Property(OutcomeInit: "Empty")
    Schema(urinalysisSchema)
}

def aggregatedSchema =  Schema("Patient", 0) {
    struct(name: 'Patient') {
        attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
        field(name: 'DateOfBirth',  type: 'date')
        field(name: 'Gender',       type: 'string', values: ['male', 'female'])
        field(name: 'Weight',       type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
        field(name: 'Transparency', type: 'string', values: ['clear', 'clouded'])
        field(name: 'Color',        type: 'string')
    }
}

def aggregateScript =  Script("Patient_Aggregate", 0) {
    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
    output('PatientXML', 'java.lang.String')
    script('groovy', 'src/main/module/script/Patient_Aggregate.groovy')
}

def aggregateEA = Activity("Patient_Aggregate", 0) {
    Property(OutcomeInit: "Empty")
    Schema(aggregatedSchema)
    Script(aggregateScript)
}

def patientWf = Workflow(name: "Patient_Workflow", version: 0, generate: true) {
    Layout {
        Act('SetDetails', setDetailsEA)
        Act('SetUrinSample', urinalysisEA)
        Loop {
            Act('Aggregate', aggregateEA) //by default the DSL creates infinitive Loop
        }
    }
}

Item(name: 'PatientFactory', version: 0, folder: '/integTest', workflow: 'CrudFactory_Workflow', workflowVer: 0) {
    InmutableProperty('Type': 'Factory')
    InmutableProperty('Root': '/integTest/Patients')
    InmutableProperty('UpdateSchema': 'Equipment_Details:0')

    Outcome(schema: 'PropertyDescription', version: '0', viewname: 'last', path: 'boot/property/Patient_0.xml')

    DependencyDescription('Doctor') {
        Properties {
            Property((DEPENDENCY_CARDINALITY): ManyToOne.toString())
            Property((DEPENDENCY_TYPE): Bidirectional.toString())
            Property((DEPENDENCY_TO): 'Patients')
        }
        Member($doctor_PropertyDescriptionList)
    }

    Dependency('workflow') {
        Member(patientWf) {
            Property('Version': 0)
        }
    }

    Dependency(MASTER_SCHEMA) {
        Member(itemPath: '/desc/Schema/integTest/Patient') {
            Property('Version': 0)
        }
    }

    Dependency(AGGREGATE_SCRIPT) {
        Member(itemPath: '/desc/Script/integTest/Patient_Aggregate') {
            Property('Version': 0)
        }
    }
}
