package org.cristalise.dev.test.scenario;

import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Ignore
import org.junit.Test


/**
 * 
 *
 */
class TutorialsDevIT extends KernelScenarioTestBase {

    @Test
    public void basicTutorial() {
        String schemaName  = "PatientDetails-$timeStamp"
        String elemActName = "SetPatientDetails-$timeStamp"
        String compActName = "PatientLifecycle-$timeStamp"
        String factoryName = "PatientFactory-$timeStamp"

        def schema = Schema(schemaName, folder) {
            struct(name: 'PatientDetails', documentation: 'This is the Schema for Basic Tutorial') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth', type: 'date')
                field(name: 'Gender',      type: 'string', values: ['male', 'female'])
                field(name: 'Weight',      type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
            }
        }

        def ea = ElementaryActivityDef(elemActName, folder) {
            Property(OutcomeInit: "Empty")
            Schema(schema)
        }

        def wf = CompositeActivityDef(compActName, folder) {
            ElemActDef('Set Patient Details',  ea)
        }

        def factory = DescriptionItem(factoryName, folder) {
            PropertyDesc(name: "Type", defaultValue: "Patient", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }

        createNewItemByFactory(factory, "CreateNewInstance", "Patient-$timeStamp", folder)

        def patient = agent.getItem("$folder/Patient-$timeStamp")

        executeDoneJob(patient, 'Set Patient Details')
    }

    @Test @Ignore
    public void extendedTutorialWithQuery() {
        def actNameSchemaNameMap = 
           ['Set Patient Details':           'PatientDetails',
            'Blood Biochemical Analysis':    'BloodSample',
            'Urinalysis':                    'UrineSample',
            'ECG':                           'ECGSample',
            'Comprehensive Eye Examination': 'SightExam']

           actNameSchemaNameMap.each { actName, schemaName ->
               def schema = Schema(schemaName+"-$timeStamp", folder) {
                   struct(name: schemaName) {
                       field(name: "details")
                   }
               }
   
               actDefs[index] = ElementaryActivityDef(actName+"-$timeStamp", folder) {
                   Role('admin')
                   Property(OutcomeInit: "XPath")
                   Schema(schema)
               }
           }

    }
}
