package org.cristalise.restapi.test

import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class EraseTest extends RestapiTestBase {

    @Test
    public void 'Erase item posting XML'() throws Exception {
        login('user', 'test')
        def uuid = createNewItem "TestItem-$timeStamp", ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.XML)
        logout(null)
    }

    @Test
    public void 'Erase item posting JSON'() throws Exception {
        login('user', 'test')
        def uuid = createNewItem "TestItem-$timeStamp", ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.JSON)
        logout(null)
    }

    @Test
    public void 'Erase agent posting XML'() throws Exception {
        login('user', 'test')
        def uuid = createNewAgent "TestAgent-$timeStamp", 'test', ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.XML)
        logout(null)
    }

    @Test
    public void 'Erase agent posting JSON'() throws Exception {
        login('user', 'test')
        def uuid = createNewAgent "TestAgent-$timeStamp", 'test', ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.JSON)
        logout(null)
    }
}
