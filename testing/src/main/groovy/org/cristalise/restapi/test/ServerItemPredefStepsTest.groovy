package org.cristalise.restapi.test

import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.lifecycle.instance.predefined.Erase
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewAgent
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewItem
import org.cristalise.kernel.lifecycle.instance.predefined.server.CreateNewRole
import org.cristalise.kernel.test.utils.KernelXMLUtility
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class ServerItemPredefStepsTest extends RestapiTestBase {

    @Test
    public void 'CreateNewItem predefined step posting XML'() throws Exception {
        login('user', 'test')
        String itemUuid = createNewItem "TestItem-$timeStamp", ContentType.XML
    }

    @Test @Ignore('OutcomeBuiler cannot convert JSON of Item.xsd to XML')
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

    @Test @Ignore('OutcomeBuiler cannot convert JSON of Agent.xsd to XML')
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
