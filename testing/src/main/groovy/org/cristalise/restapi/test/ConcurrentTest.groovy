package org.cristalise.restapi.test

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import org.cristalise.kernel.process.Gateway
import org.junit.jupiter.api.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 *
 *
 */
@Slf4j @CompileStatic
class ConcurrentTest extends RestapiTestBase {

    /**
     * 
     * @param count
     * @return
     */
    private List<String> setupPatients(int count) {
        def factory = agent.getItem("/$folder/PatientFactory")
        def createItemJob = factory.getJobByName('InstantiateItem', agent)
        def o = createItemJob.getOutcome()
        // Empty OotcomeInitiator will create this optional node
        o.removeNodeByXPath('//PropertyList')

        List<String> uuids = []

        count.times { int idx ->
            def name = "Patient${idx+1}"

            o.setField('Name', name)
            o.setField('SubFolder', timeStamp)
            agent.execute(createItemJob)

            def p = agent.getItem("$folder/Patients/$timeStamp/$name")

            executeDoneJob(p, 'SetDetails')
            executeDoneJob(p, 'SetUrinSample')

            uuids << p.uuid
        }

        return uuids
    }

    /**
     * issue #447: Deadlock caused by editing a Script
     */
    @Test @CompileDynamic
    public void createPatients_RunAggrageScripts_EditScript_Concurrently() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')

        def patientCount = 10
        def uuids = setupPatients(patientCount)

        login()

        def pool = Executors.newFixedThreadPool(patientCount+1)

        pool.submit {
            // Simulates editing the Script - check issue #447
            5.times {
                Script("Patient_Aggregate", folder) {
                    input('item', 'org.cristalise.kernel.entity.proxy.ItemProxy')
                    output('PatientXML', 'java.lang.String')
                    script(language: 'groovy') {
                        new File('src/main/data/AggregatePatientData.groovy').text
                    }
                }
                Thread.sleep(new Random().nextInt(10)*100)
                log.info "finished editing Script: Patient_Aggregate"
            }
        }

        patientCount.times { int idx ->
            pool.submit {
                10.times {
                    def result = executeScript(uuids[idx], 'Patient_Aggregate', '{}')
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

    /**
     * issue #502/#509: Make the initial parsing of Script XML thread-safe
     */
    @Test @CompileDynamic
    public void createPatients_RunAggrageScripts_ParseScript_Concurrently() {
        init('src/main/bin/client.conf', 'src/main/bin/integTest.clc')
        def clearCacheScriptName = 'ClearCache'+timeStamp

        def patientCount = 10
        def uuids = setupPatients(patientCount)

        login()

        def pool = Executors.newFixedThreadPool(patientCount+1)

        Script(clearCacheScriptName, folder) {
            script(language: 'groovy') {
                'org.cristalise.kernel.process.Gateway.getStorage().clearCache(); System.gc();'
            }
        }
        log.info 'finished creating Script: ' + clearCacheScriptName

        patientCount.times { int idx ->
            pool.submit {
                2.times {
                    def result = executeScript(uuids[idx], 'Patient_Aggregate', '{}')
                    log.info "${uuids[idx]} - $result"
                }
            }
            executeScript(uuids[idx], clearCacheScriptName, '{}')
            Thread.sleep(2000)
        }

        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.MINUTES)

        logout('')
        Gateway.close()
    }
}
