package org.cristalise.restapi.test

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class ServerItemPredefStepsTest extends RestapiTestBase {

    @Test
    public void 'CreateNewItem predefined step posting XML'() throws Exception {
        login('user', 'test')
        String itemUuid = createNewItem "TestItem-$timeStamp", ContentType.XML
    }

    @Test @Disabled('OutcomeBuiler cannot convert JSON of Item.xsd to XML')
    public void 'CreateNewItem predefined step posting JSON'() throws Exception {
        login('user', 'test')
        String itemUuid = createNewItem "TestItem-$timeStamp", ContentType.JSON
    }

    @Test
    public void 'CreateNewAgent predefined step posting XML'() throws Exception {
        login('user', 'test')
        createNewAgent "TestAgent-$timeStamp", 'test', ContentType.XML
        logout(null)

        login("TestAgent-$timeStamp", 'test')
        logout(null)
    }

    @Test @Disabled('OutcomeBuiler cannot convert JSON of Agent.xsd to XML')
    public void 'CreateNewAgent predefined step posting JSON'() throws Exception {
        login('user', 'test')
        createNewAgent "TestAgent-$timeStamp", 'test', ContentType.JSON
        logout(null)

        login("TestAgent-$timeStamp", 'test')
        logout(null)
    }

    @Test
    public void 'CreateNewRole predefined step posting XML'() throws Exception {
        login('user', 'test')
        createNewRole "TestRole-$timeStamp", ContentType.XML
        logout(null)
    }

    @Test
    public void 'CreateNewRole predefined step posting JSON'() throws Exception {
        login('user', 'test')
        createNewRole "TestRole-$timeStamp", ContentType.JSON
        logout(null)
    }
}
