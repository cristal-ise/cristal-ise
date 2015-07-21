package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.RolePath
import org.junit.Before
import org.junit.Test


@CompileStatic
class LookupRoleTests extends LookupTestBase {

    UUID uuid0 = new UUID(0,0)
    UUID uuid1 = new UUID(0,1)

    RolePath user = new RolePath(new RolePath(), "User")
    AgentPath jim = new AgentPath(new ItemPath(), "Jim")

    @Before
    public void setUp() throws Exception {
        super.setUp()
        
        lookup.add(user)
        lookup.add(jim)
    }

    public void checkRolePath(RolePath parent, RolePath role, String name) {
        assert role.getName() == name
        assert role.string == "$parent.string/$name"
        assert lookup.exists(role)
        assert lookup.getRolePath(name) == role
    }

    public RolePath createUserRole(String name) {
        RolePath role = lookup.createRole(new RolePath(user, name))
        checkRolePath(user, role, name)
        return role
    }

    @Test
    public void createRole() {
        createUserRole("Internist")
    }

    @Test
    public void addRole() {
        RolePath internist    = createUserRole("Internist")
        RolePath cardiologist = createUserRole("Cardiologist")

        def expected = [internist, cardiologist]

        lookup.addRole(jim, internist)
        lookup.addRole(jim, cardiologist)

        for(RolePath r: lookup.getRoles(jim) ) { assert expected.contains(r) }
    }
}
