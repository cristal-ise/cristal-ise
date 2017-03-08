/**
 * This file is part of the CRISTAL-iSE jOOQ Cluster Storage Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.lookup.test;

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.RolePath
import org.junit.Before
import org.junit.Test

@CompileStatic
class LookupRoleTests extends LookupTestBase {

    RolePath user = new RolePath( new RolePath(), "User")
    AgentPath jim = new AgentPath(new ItemPath(), "Jim")
    AgentPath tom = new AgentPath(new ItemPath(), "Tom")

    @Before
    public void setUp() throws Exception {
        super.setUp()

        lookup.add(user)
        lookup.add(jim)
        lookup.add(tom)
    }

    public void checkRolePath(RolePath parent, RolePath role, String name, boolean hasJobList = false) {
        assert role.getName() == name
        assert role.stringPath == "$parent.stringPath/$name"
        assert lookup.exists(role)
        def r = lookup.getRolePath(name)
        assert r == role
        assert r.hasJobList() == hasJobList
    }

    public RolePath createUserRole(String name, boolean hasJobList = false) {
        RolePath role = lookup.createRole(new RolePath(user, name, hasJobList))
        checkRolePath(user, role, name, hasJobList)
        return role
    }

    @Test
    public void createRole() {
        createUserRole("Internist")
        createUserRole("Cardiologist", true)
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
    public void getParent_ObjectNotFoundException() {
        def rp = new RolePath("Clerk/Secretary".split("/"), false)
        try {
            rp.getParent()
            fail("Should throw ObjectNotFoundException")
        }
        catch (ObjectNotFoundException e) {}
    }

    @Test
    public void addRole_ObjectCannotBeUpdated() {
        RolePath internist = createUserRole("Internist")
        lookup.addRole(jim, internist)
        try {
            lookup.addRole(jim, internist)
            fail("Should throw ObjectCannotBeUpdated exception")
        }
        catch (ObjectCannotBeUpdated e) {}
    }

    @Test
    public void removeRole_ObjectCannotBeUpdated() {
        RolePath internist = createUserRole("Internist")
        lookup.addRole(jim, internist)
        lookup.removeRole(jim, internist)
        try {
            lookup.removeRole(jim, internist)
            fail("Should throw ObjectCannotBeUpdated exception")
        }
        catch (ObjectCannotBeUpdated e) {}
    }

    @Test
    public void addTwoRolesToAgent() {
        RolePath internist    = createUserRole("Internist")
        RolePath cardiologist = createUserRole("Cardiologist")

        lookup.addRole(jim, internist)
        lookup.addRole(jim, cardiologist)
        
        assert lookup.hasRole(jim, internist)
        assert lookup.hasRole(jim, cardiologist)

        CompareUtils.comparePathLists([internist, cardiologist], lookup.getRoles(jim))
        CompareUtils.comparePathLists([jim], lookup.getAgents(internist))
        CompareUtils.comparePathLists([jim], lookup.getAgents(cardiologist))
    }

    @Test
    public void addTwoAgentsToRole() {
        RolePath internist = createUserRole("Internist")

        lookup.addRole(jim, internist)
        lookup.addRole(tom, internist)

        assert lookup.hasRole(jim, internist)
        assert lookup.hasRole(tom, internist)

        CompareUtils.comparePathLists([jim, tom],  lookup.getAgents(internist))
        CompareUtils.comparePathLists([internist], lookup.getRoles(jim))
        CompareUtils.comparePathLists([internist], lookup.getRoles(tom))

        assert lookup.getRolePath("Internist") == internist
    }

    @Test
    public void addRemoveRole() {
        RolePath internist    = createUserRole("Internist")
        RolePath cardiologist = createUserRole("Cardiologist")

        lookup.addRole(jim, internist)
        lookup.addRole(jim, cardiologist)
        lookup.addRole(tom, cardiologist)

        assert lookup.hasRole(jim, internist)
        assert lookup.hasRole(jim, cardiologist)

        CompareUtils.comparePathLists([internist, cardiologist], lookup.getRoles(jim))
        CompareUtils.comparePathLists([cardiologist],            lookup.getRoles(tom))
        CompareUtils.comparePathLists([jim],                     lookup.getAgents(internist))
        CompareUtils.comparePathLists([jim, tom],                lookup.getAgents(cardiologist))

        lookup.removeRole(jim, cardiologist)

        assert   lookup.hasRole(jim, internist)
        assert ! lookup.hasRole(jim, cardiologist)

        CompareUtils.comparePathLists([internist],    lookup.getRoles(jim))
        CompareUtils.comparePathLists([cardiologist], lookup.getRoles(tom))
        CompareUtils.comparePathLists([jim],          lookup.getAgents(internist))
        CompareUtils.comparePathLists([tom],          lookup.getAgents(cardiologist))
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
