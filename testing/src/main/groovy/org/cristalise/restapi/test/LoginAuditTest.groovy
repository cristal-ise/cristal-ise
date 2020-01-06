package org.cristalise.restapi.test

import org.junit.Test

import groovy.transform.CompileStatic

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
