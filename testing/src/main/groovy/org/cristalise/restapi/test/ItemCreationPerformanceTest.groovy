package org.cristalise.restapi.test

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

    int count = 100

    
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
    }
}
