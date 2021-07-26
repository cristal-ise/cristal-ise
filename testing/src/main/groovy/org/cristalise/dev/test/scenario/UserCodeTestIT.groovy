package org.cristalise.dev.test.scenario

import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.KernelScenarioTestBase
import org.junit.Test

import groovy.transform.CompileStatic
import spock.util.concurrent.PollingConditions

/**
 *
 *
 */
@CompileStatic
class UserCodeTestIT extends KernelScenarioTestBase {
    @Test
    public void 'Usercode to execute Aggregate Script'() {
        def factory = agent.getItem("$folder/PatientFactory")
        def patientName = 'Patient-'+timeStamp

        executeDoneJob(factory, 'InstantiateItem', "<CrudFactory_NewInstanceDetails><Name>$patientName</Name></CrudFactory_NewInstanceDetails>")
        def patient = agent.getItem("$folder/Patients/$patientName")

        RolePath rp = Gateway.getLookup().getRolePath('UserCode')
        def ucPath = Gateway.getLookup().getAgents(rp)[0]
        def userCode = new AgentTestBuilder(ucPath)

        executeDoneJob(patient, 'SetDetails')
        executeDoneJob(patient, 'SetUrinSample')

        PollingConditions pollingWait = new PollingConditions(timeout: 5, initialDelay: 0.5, delay: 0.5, factor: 1)
        pollingWait.eventually {
            assert patient.checkViewpoint('Patient', 'last')
        }

        //checks if the usercode successfully executed aggregate script and the outcome was stored
        def vp = patient.getViewpoint('Patient', 'last')
        patient.getOutcome(vp)
    }
}
