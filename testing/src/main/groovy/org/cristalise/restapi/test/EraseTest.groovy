package org.cristalise.restapi.test

import static org.junit.jupiter.api.Assertions.fail
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
        try {
            resolveDomainPath("/restapiTests/TestItem-$timeStamp")
            fail("Item:/restapiTests/TestItem-$timeStamp shall be deleted")
        }
        catch (Throwable e) {}
        logout(null)
    }

    @Test
    public void 'Erase item posting JSON'() throws Exception {
        login('user', 'test')
        def uuid = createNewItem "TestItem-$timeStamp", ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.JSON)
        try {
            resolveDomainPath("/restapiTests/TestItem-$timeStamp")
            fail("Item:/restapiTests/TestItem-$timeStamp shall be deleted")
        }
        catch (Throwable e) {}
        logout(null)
    }

    @Test
    public void 'Erase agent posting XML'() throws Exception {
        login('user', 'test')
        def uuid = createNewAgent "TestAgent-$timeStamp", 'test', ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.XML)
        try {
            resolveDomainPath("/restapiTests/TestItem-$timeStamp")
            fail("Item:/restapiTests/TestItem-$timeStamp shall be deleted")
        }
        catch (Throwable e) {}
        logout(null)
    }

    @Test
    public void 'Erase agent posting JSON'() throws Exception {
        login('user', 'test')
        def uuid = createNewAgent "TestAgent-$timeStamp", 'test', ContentType.XML
        executePredefStep(uuid, Erase.class, ContentType.JSON)
        try {
            resolveDomainPath("/restapiTests/TestItem-$timeStamp")
            fail("Item:/restapiTests/TestItem-$timeStamp shall be deleted")
        }
        catch (Throwable e) {}
        logout(null)
    }
}
