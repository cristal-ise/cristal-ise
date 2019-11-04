package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static org.hamcrest.Matchers.*

import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.utils.Logger
import org.json.JSONArray
import org.junit.BeforeClass
import org.junit.Test

import groovy.transform.CompileStatic
import io.restassured.http.ContentType
import io.restassured.http.Cookie
import io.restassured.response.Response

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
}
