package org.cristalise.restapi.test

import org.junit.jupiter.api.Test

import groovy.transform.CompileStatic

@CompileStatic
class LoginTest extends RestapiTestBase {

    @Test
    public void 'Test Login-Logout Login-LoginTimeout and Login-ForcedLogout scenarios'() throws Exception {

        login()
        def eventId = checkEvent(null, 'Login')

        logout(null)
        checkEvent(++eventId, 'Logout')

        loginPost()
        checkEvent(++eventId, 'Login')

        logout('timeout')
        checkEvent(++eventId, 'LoginTimeout')

        login()
        checkEvent(++eventId, 'Login')

        logout('windowClose')
        checkEvent(++eventId, 'ForcedLogout')
    }
    
    @Test
    public void 'Test Login using passowrd with special characters'() throws Exception {
        loginPost('mainUser', 'test©£')
    }
}
