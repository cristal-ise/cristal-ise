package org.cristalise.restapi.test

import static io.restassured.RestAssured.*
import static org.hamcrest.Matchers.*

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
class LoginAuditTest extends RestapiTestBase {

    @Test
    public void 'Test Login-Logout Login-LoginTimeout and Login-ForcedLogout scenarios'() throws Exception {
        login()
        def eventId = checkEvent(null, 'Login')

        logout(null)
        checkEvent(++eventId, 'Logout')

        login()
        checkEvent(++eventId, 'Login')

        logout('timeout')
        checkEvent(++eventId, 'LoginTimeout')

        login()
        checkEvent(++eventId, 'Login')

        logout('windowClose')
        checkEvent(++eventId, 'ForcedLogout')
    }
}
