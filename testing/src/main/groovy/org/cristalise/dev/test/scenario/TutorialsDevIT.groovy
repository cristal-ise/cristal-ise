package org.cristalise.dev.test.scenario

import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test

import spock.util.concurrent.PollingConditions


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

        agent.execute(patient, "Erase", new String[0])
    }

    @Test
    public void extendedTutorialWitAggregate() {
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

        //def aggregateQuery =  Query("AggregatePatientData-$timeStamp", folder) {
        //    parameter(name: 'root',       type: 'java.lang.String')
        //    parameter(name: 'uuid',       type: 'java.lang.String')
        //    parameter(name: 'schemaName', type: 'java.lang.String')
        //    parameter(name: 'postFix',    type: 'java.lang.String')
        //    query(language: "existdb:xquery") {
        //        new File('src/main/data/AggregatePatientData.xql').text
        //    }
        //}

        def aggregateScript =  Script("AggregatePatientData-$timeStamp", folder) {
            output("error", "org.cristalise.kernel.scripting.ErrorInfo")
            script(language: 'groovy') {
                new File('src/main/data/AggregatePatientData.groovy').text
            }
        }

        actMap["SetAggregated-$timeStamp"] = ElementaryActivityDef("SetAggregated-$timeStamp", folder) {
            Property(postFix: timeStamp)
            Role("UserCode")
            Schema(aggregatedSchema)
            Script(aggregateScript)
        }

        def ucPath = Gateway.getLookup().getAgents(new RolePath("UserCode", false))[0]
        def userCode = new AgentTestBuilder(ucPath)

        setupPatient(actMap)

        executeDoneJob(patient, elemActName)
        executeDoneJob(patient, 'SetUrinSample')

        PollingConditions pollingWait = new PollingConditions(timeout: 3, initialDelay: 0.2, factor: 1)
        pollingWait.eventually { userCode.jobListContains([stepName: "SetAggregated-$timeStamp", agentRole: "UserCode", transitionName: "Proceed"]) }
    }
}
