package org.cristalise.dev.test.scenario

import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Ignore
import org.junit.Test


/**
 * 
 *
 */
class TutorialsDevIT extends KernelScenarioTestBase {
    String schemaName  = "PatientDetails"
    String elemActName = "SetPatientDetails"
    String compActName = "PatientLifecycle"
    String factoryName = "PatientFactory"
    String itemName    = "Patient"

    ItemProxy patient

	/**
	 * 
	 * @param actDefList
	 */
    private void setupPatient(Map<String, ActivityDef> actDefList) {
        def schema = Schema("$schemaName-$timeStamp", folder) {
            struct(name: schemaName, documentation: 'This is the Schema for Basic Tutorial') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth', type: 'date')
                field(name: 'Gender',      type: 'string', values: ['male', 'female'])
                field(name: 'Weight',      type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
            }
        }

        def ea = ElementaryActivityDef("$elemActName-$timeStamp", folder) {
            Property(OutcomeInit: "Empty")
            Schema(schema)
        }

        def wf = CompositeActivityDef("$compActName-$timeStamp", folder) {
            ElemActDef(elemActName,  ea)
            actDefList.each { name, actDef ->
                ElemActDef(name,  actDef)
            }
        }

        def factory = DescriptionItem("$factoryName-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "Patient", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }

        createNewItemByFactory(factory, "CreateNewInstance", "$itemName-$timeStamp", folder)

        patient = agent.getItem("$folder/$itemName-$timeStamp")
    }

    @Test
    public void basicTutorial() {
        setupPatient()

        executeDoneJob(patient, elemActName)
    }

    @Test @Ignore("Query does not work with jooqdb backend")
    public void extendedTutorialWithQuery() {
        Map<String, ActivityDef> actMap = [:]

        def urinalysisSchema =  Schema("UrinSample-$timeStamp", folder) {
            struct(name: 'UrinSample') {
                field(name: 'Transparency', type: 'string', values: ['clear', 'clouded'])
                field(name: 'Color',        type: 'string')
            }
        }

        def urinalysisEA = ElementaryActivityDef("SetUrinSample-$timeStamp", folder) {
            Property(OutcomeInit: "Empty")
            Schema(urinalysisSchema)
        }

        actMap['SetUrinSample'] = urinalysisEA

        def aggregatedSchema =  Schema("AggregatedPatientData-$timeStamp", folder) {
            struct(name: 'AggregatedPatientData') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth',  type: 'date')
                field(name: 'Gender',       type: 'string', values: ['male', 'female'])
                field(name: 'Weight',       type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
                field(name: 'Transparency', type: 'string', values: ['clear', 'clouded'])
                field(name: 'Color',        type: 'string')
            }
        }

        def aggregateQuery =  Query("AggregatePatientData-$timeStamp", folder) {
            parameter(name: 'root',       type: 'java.lang.String')
            parameter(name: 'uuid',       type: 'java.lang.String')
            parameter(name: 'schemaName', type: 'java.lang.String')
            parameter(name: 'postFix',    type: 'java.lang.String')
            query(language: "existdb:xquery") {
                new File('src/main/data/AggregatePatientData.xql').text
            }
        }

        actMap['SetAggregated'] = ElementaryActivityDef("SetAggregated-$timeStamp", folder) {
            Property(postFix: timeStamp)
            Role("UserCode")
            Schema(aggregatedSchema)
            Query(aggregateQuery)
        }

        setupPatient(actMap)

        executeDoneJob(patient, elemActName)
        executeDoneJob(patient, 'SetUrinSample')

        //TODO: Wait until notification is received abotu the execution of 'SetAggregated' Activity, and check the Outcome
    }
}
