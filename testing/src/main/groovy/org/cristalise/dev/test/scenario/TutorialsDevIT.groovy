package org.cristalise.dev.test.scenario

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertThat
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals

import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.kernel.entity.imports.ImportRole
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.instance.predefined.server.UpdateRole
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.cristalise.kernel.utils.CastorXMLUtility
import org.hamcrest.collection.IsIterableContainingInAnyOrder
import org.junit.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import spock.util.concurrent.PollingConditions


/**
 *
 *
 */
@CompileStatic
class TutorialsDevIT extends KernelScenarioTestBase {
    String schemaName  = "PatientDetails"
    String elemActName = "SetPatientDetails"
    String compActName = "PatientLifecycle"
    String factoryName = "PatientFactory"
    String itemType    = "Patient"

    /**
     *
     * @param actDefList
     */
    @CompileDynamic
    private ItemProxy setupPatient(Map<String, ActivityDef> actDefList) {
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

        return DescriptionItem("$factoryName-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "Patient", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }
    }

    @Test
    public void 'Basic Tutorial with one Activy'() {
        def factory = setupPatient([:])
        createNewItemByFactory(factory, "CreateNewInstance", "$itemType-$timeStamp", folder)
        def patient = agent.getItem("$folder/$itemType-$timeStamp")

        executeDoneJob(patient, elemActName)
    }
    
    @CompileDynamic
    private ItemProxy setupExtedned() {
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

//        Schema(itemType, folder) {
//            struct(name: itemType) {
//                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
//                field(name: 'DateOfBirth',  type: 'date')
//                field(name: 'Gender',       type: 'string', values: ['male', 'female'])
//                field(name: 'Weight',       type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
//                field(name: 'Transparency', type: 'string', values: ['clear', 'clouded'])
//                field(name: 'Color',        type: 'string')
//            }
//        }

//        Script("${itemType}_Aggregate", folder) {
//            output("error", "org.cristalise.kernel.scripting.ErrorInfo")
//            script(language: 'groovy') {
//                new File('src/main/data/AggregatePatientData.groovy').text
//            }
//        }

        return setupPatient(actMap)
    }

    @Test
    public void 'Extended Tutorial with default Master Schema and Aggregate Script'() {
        def factory = setupExtedned()
        createNewItemByFactory(factory, "CreateNewInstance", "$itemType-$timeStamp", folder)

        def patient = agent.getItem("$folder/$itemType-$timeStamp")
        
        assert patient.getMasterSchema()
        assert patient.getAggregateScript()

        executeDoneJob(patient, elemActName)
        executeDoneJob(patient, 'SetUrinSample')
    }

    @CompileDynamic
    private ItemProxy setupUsercode() {
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

        return setupPatient(actMap)
    }

    @Test
    public void 'Extended Tutorial using Usercode to execute Aggregate Script'() {
        def factory = setupUsercode()
        createNewItemByFactory(factory, "CreateNewInstance", "$itemType-$timeStamp", folder)
        def patient = agent.getItem("$folder/$itemType-$timeStamp")

        RolePath rp = Gateway.getLookup().getRolePath('UserCode')
        def ucPath = Gateway.getLookup().getAgents(rp)[0]
        def userCode = new AgentTestBuilder(ucPath)

        executeDoneJob(patient, elemActName)
        executeDoneJob(patient, 'SetUrinSample')

        PollingConditions pollingWait = new PollingConditions(timeout: 3, initialDelay: 0.2 as double, factor: 1)
        pollingWait.eventually {
            userCode.jobList.isEmpty()
        }

        //checks if the usercode successfully executed aggregate script and the outcome was stored
        def vp = patient.getViewpoint("AggregatedPatientData-$timeStamp", 'last')
        patient.getOutcome(vp)
    }
}
