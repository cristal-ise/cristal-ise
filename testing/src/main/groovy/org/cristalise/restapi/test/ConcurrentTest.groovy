package org.cristalise.restapi.test

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.process.Gateway
import org.junit.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.restassured.http.ContentType


/**
 *
 *
 */
@Slf4j
class ConcurrentTest extends RestapiTestBase {

    /**
     * 
     * @return
     */
    private ItemProxy setupPatientDesc() {
        def detailsSchema = Schema("PatientDetails-$timeStamp", folder) {
            struct(name: 'PatientDetails') {
                attribute(name: 'InsuranceNumber', type: 'string', default: '123456789ABC')
                field(name: 'DateOfBirth', type: 'date')
                field(name: 'Gender',      type: 'string', values: ['male', 'female'])
                field(name: 'Weight',      type: 'decimal') { unit(values: ['g', 'kg'], default: 'kg') }
            }
        }
    
        def setDetailsEA = ElementaryActivityDef("SetPatientDetails-$timeStamp", folder) {
            Property(OutcomeInit: "Empty")
            Schema(detailsSchema)
        }
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
    
        def aggregateScript =  Script("AggregatePatientData-$timeStamp", folder) {
            input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
            input('postFix', 'java.lang.String')
            output('PatientXML', 'java.lang.String')
            script(language: 'groovy') {
                new File('src/main/data/AggregatePatientData.groovy').text
            }
        }
    
        def wf = CompositeActivityDef("PatientLifecycle-$timeStamp", folder) {
            ElemActDef('SetPatientDetails', setDetailsEA)
            ElemActDef('SetUrinSample', urinalysisEA)
        }

        return DescriptionItem("PatientFactory-$timeStamp", folder) {
            PropertyDesc(name: "Type", defaultValue: "Patient", isMutable: false, isClassIdentifier: true)
            Workflow(wf)
        }
    }

    /**
     * 
     * @param count
     * @return
     */
    @CompileStatic
    private List<String> setupPatients(int count) {
        def factory = setupPatientDesc()

        List<String> uuids = []

        count.times { int idx ->
            def name = "Patient${idx+1}-$timeStamp"
            createNewItemByFactory(factory, "CreateNewInstance", name, folder)
            def p = agent.getItem("$folder/$name")

            executeDoneJob(p, 'SetPatientDetails')
            executeDoneJob(p, 'SetUrinSample')

            uuids << p.getPath().getUUID().toString()
        }

        return uuids
    }

    /**
     * 
     * @param count
     * @return
     */
    private List<String> getPatientUuids(int count) {
        List<String> uuids = []

        count.times { int idx ->
            def name = "Patient${idx+1}-$timeStamp"
            def p = agent.getItem("$folder/$name")
            uuids << p.getPath().getUUID().toString()
        }

        return uuids
    }

    @Test
    public void createPatients_RunAggrageScripts_EditScript_Concurrently() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')

        def patientCount = 10
        def uuids = setupPatients(patientCount)

        // use these 2 lines instead of the one above when all items are setup and only the concurrent test needs to be run
        //timeStamp = '2020-11-12_15-49-38_970'
        //def uuids = getPatientUuids(patientCount)

        login()

        def pool = Executors.newFixedThreadPool(patientCount+1)

        pool.submit {
            // Simulates editing the Script
            50.times {
                Script("AggregatePatientData-$timeStamp", folder) {
                    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
                    input('postFix', 'java.lang.String')
                    output('PatientXML', 'java.lang.String')
                    script(language: 'groovy') {
                        new File('src/main/data/AggregatePatientData.groovy').text
                    }
                }
                Thread.sleep(new Random().nextInt(10)*100)
                log.info "finished editing Script: AggregatePatientData-$timeStamp"
            }
        }

        patientCount.times { int idx ->
            pool.submit {
                100.times {
                    def result = executeScript(uuids[idx], "AggregatePatientData-$timeStamp", "{'postFix':'$timeStamp'}")
                    Thread.sleep(new Random().nextInt(10)*10)
                    log.info "${uuids[idx]} - $result"
                }
            }
        }

        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.MINUTES)

        logout('')
        Gateway.close()
    }

    @Test
    public void createPatients_Concurrently() {
        //init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')
        //setupPatientDesc()
        //Gateway.close()

        login()

        // use this line when descriptions are setup and only the concurrent test needs to be run
        timeStamp = '2020-11-16_16-11-55_741'
        def factoryUuuid = resolveDomainPath("/$folder/PatientFactory-$timeStamp")

        def patientCount = 1000
        def pool = Executors.newFixedThreadPool(10)

        log.info('createPatients_Concurrently() - Starts creating {} patients', patientCount)

        patientCount.times { int idx ->
            pool.submit {
                def name = "Patient${idx+1}-$timeStamp"
                log.info('createPatients_Concurrently() - Start:  {}', name)
                def json = "{'NewDevObjectDef': {'ObjectName': $name, 'SubFolder': $folder}}"
                executeActivity(factoryUuuid, 'CreateNewInstance', json)
                resolveDomainPath("$folder/$name") // tests if the patient was created
                log.info('createPatients_Concurrently() - Created:{}', name)
                //Thread.sleep(new Random().nextInt(10)*10)
            }
        }

        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.MINUTES)

        log.info('createPatients_Concurrently() - Finished creating {} patients', patientCount)

        logout('')
    }
}
