package org.cristalise.restapi.test

import org.cristalise.kernel.process.Gateway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic

@CompileStatic
class NgDynamiFormApiTest extends RestapiTestBase {
    
    @BeforeAll
    public void beforeAll() {
        super.init()
        login('user', 'test')
    }

    @AfterAll
    public void afterAll() {
        logout()
        Gateway.close()
    }

    @Test
    public void getJobFormTemplate() throws Exception {
        def itemUuid = resolveDomainPath('integTest/PatientFactory')

        def result = getJobFormTemplate(itemUuid, 'InstantiateItem', 'Done')
        assert result
//        println result
    }

    @Test
    public void getJobFormModel() throws Exception {
        def itemUuid = resolveDomainPath('integTest/PatientFactory')

        def result = getJobFormModel(itemUuid, 'InstantiateItem', 'Done')
        assert result
//        println result
    }

    @Test
    public void getJobFormLayout() throws Exception {
        def itemUuid = resolveDomainPath('integTest/PatientFactory')

        def result = getJobFormLayout(itemUuid, 'InstantiateItem', 'Done')
        assert result
//        println result
    }
}
