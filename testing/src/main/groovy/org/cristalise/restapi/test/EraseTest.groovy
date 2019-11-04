package org.cristalise.restapi.test

import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class EraseTest extends RestapiTestBase {
    
    @Test
    public void 'Erase item using ContentType XML'() throws Exception {
        login('mainUser', 'test')
        def mainUserUuid = userUuid
        logout(null)

        login('user', 'test')
        executePredefStep(mainUserUuid, Erase.class, ContentType.XML)
        logout(null)
    }

    @Test
    public void 'Erase item using ContentType JSON'() throws Exception {
        login('mainUser', 'test')
        def mainUserUuid = userUuid
        logout(null)

        login('user', 'test')
        executePredefStep(mainUserUuid, Erase.class, ContentType.JSON)
        logout(null)
    }
}
