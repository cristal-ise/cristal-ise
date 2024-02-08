package org.cristalise.dev.test.scenario

import static org.cristalise.dev.dsl.DevXMLUtility.recordToXML
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 *
 *
 */
@CompileStatic @Slf4j
@TestInstance(Lifecycle.PER_CLASS)
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
            Layout {
                Act(elemActName,  ea)
                actDefList.each { name, actDef ->
                    Act(name,  actDef)
                }
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
        def patient = createItemFromDescription(factory, "$itemType-$timeStamp", folder)

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
        def patient = createItemFromDescription(factory, "$itemType-$timeStamp", folder)

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
}
