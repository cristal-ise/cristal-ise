package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
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
    public void createRole_ObjectAlreadyExists() {
        createUserRole("Internist")
        try {
            createUserRole("Internist")
            fail("Should throw ObjectAlreadyExistsException")
        }
        catch (ObjectAlreadyExistsException e) {}
    }
    
    @Test
    public void addRole_ObjectCannotBeUpdated() {
        RolePath internist = createUserRole("Internist")
        lookup.addRole(jim, internist)
        try {
            lookup.addRole(jim, internist)
            fail("Should throw ObjectCannotBeUpdated exception")
        } catch (ObjectCannotBeUpdated e) {}
    }

    @Test
    public void removeRole_ObjectCannotBeUpdated() {
        RolePath internist = createUserRole("Internist")
        lookup.addRole(jim, internist)
        lookup.removeRole(jim, internist)
        try {
            lookup.removeRole(jim, internist)
            fail("Should throw ObjectCannotBeUpdated exception")
        } catch (ObjectCannotBeUpdated e) {}
    }

    @Test
    public void addRemoveRole() {
        RolePath internist    = createUserRole("Internist")
        RolePath cardiologist = createUserRole("Cardiologist")

        lookup.addRole(jim, internist)
        lookup.addRole(jim, cardiologist)

        CompareUtils.comparePathLists([internist, cardiologist], lookup.getRoles(jim))

        lookup.removeRole(jim, cardiologist)

        CompareUtils.comparePathLists([internist], lookup.getRoles(jim))
    }

    @Test
    public void deleteRole() {
        RolePath internist = createUserRole("Internist")
        lookup.addRole(jim, internist)
        CompareUtils.comparePathLists([internist], lookup.getRoles(jim))

        lookup.delete(internist)
        assert ! lookup.exists(internist)
        CompareUtils.comparePathLists([], lookup.getRoles(jim))
    }
}
