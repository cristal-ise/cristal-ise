package org.cristalise.restapi.test

import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 *
 *
 */
@Slf4j @CompileStatic
class ItemCreationPerformanceTest extends RestapiTestBase {

    int count = 20

    @BeforeEach
    public void before() {
        timeStamp = getNowString()
        login()
    }

    @AfterEach
    public void after() {
        logout()
    }

    /**
     * 
     * @param count
     * @return
     */
    @Test
    public void createPatients() {
        def factoryUuid = resolveDomainPath('integTest/PatientFactory')

        count.times { int idx ->
            def json = "{'CrudFactory_NewInstanceDetails': {'Name': 'Patient${idx+1}', 'SubFolder': '$timeStamp'}}";

            executeActivity(factoryUuid, 'InstantiateItem', json.toString())
        }

        def PatientDetails = org.json.XML.toJSONObject('''
          <PatientDetails InsuranceNumber="string">
            <DateOfBirth>2008-09-29</DateOfBirth>
            <Gender>male</Gender>
            <Weight unit="kg">1000.0</Weight>
          </PatientDetails>
        ''').toString()

        def UrinSample = org.json.XML.toJSONObject('''
          <UrinSample>
            <Transparency>clear</Transparency>
            <Color>yellow</Color>
          </UrinSample>
        ''').toString()

        count.times { int idx ->
            if (idx % 10 == 0) {
                def dp = "integTest/Patients/$timeStamp/Patient${idx+1}"
                def itemUuid = resolveDomainPath(dp)

                log.info('createPatients() - executing acts of patient:{}', dp)

                executeActivity(itemUuid, 'SetDetails',    PatientDetails)
                executeActivity(itemUuid, 'SetUrinSample', UrinSample)
            }
        }
    }
}
