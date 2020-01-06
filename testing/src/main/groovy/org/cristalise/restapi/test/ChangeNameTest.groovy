package org.cristalise.restapi.test

import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType

@CompileStatic
class ChangeNameTest extends RestapiTestBase {
    
    @Test
    public void 'Login after changing the Name of a logged in Agent'() throws Exception {
        login('mainUser', 'test')
        def mainUserUuid = userUuid
        logout(null)

        login('user', 'test')
        executePredefStep(mainUserUuid, ChangeName.class, 'mainUser', 'rootUser')
        logout(null)

        login('rootUser', 'test')
        assert mainUserUuid == userUuid
        logout(null)

        login('user', 'test')
        executePredefStep(mainUserUuid, ChangeName.class, 'rootUser', 'mainUser')
        logout(null)

        login('mainUser', 'test')
        logout(null)
    }

    @Test
    public void 'Change name of an Item from camel case to lower case'() throws Exception {
        login('user', 'test')
        def name = "TestItem-$timeStamp"
        def itemUuid = createNewItem(name, ContentType.XML)
        executePredefStep(itemUuid, ChangeName.class, name, name.toLowerCase())
        logout(null)
    }
}
