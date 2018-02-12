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
package org.cristalise.lookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.junit.Before;
import org.junit.Test;

public class LookupRoleTests extends LookupTestBase {

    RolePath user = new RolePath( new RolePath(), "User");
    AgentPath jim = new AgentPath(new ItemPath(), "Jim");
    AgentPath tom = new AgentPath(new ItemPath(), "Tom");

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        lookup.add(user);
        lookup.add(jim);
        lookup.add(tom);
    }

    public void checkRolePath(RolePath parent, RolePath role, String name, boolean hasJobList) throws Exception {
        assert lookup.exists(role);
        assertEquals(name, role.getName());
        assertEquals(parent.getStringPath()+"/"+name, role.getStringPath());

        RolePath r = lookup.getRolePath(name);
        assertReflectionEquals(role, r);
        assert r.hasJobList() == hasJobList;
    }

    public RolePath createUserRole(String name, boolean hasJobList) throws Exception {
        RolePath role = lookup.createRole(new RolePath(user, name, hasJobList));
        checkRolePath(user, role, name, hasJobList);
        return role;
    }

    @Test
    public void createRole() throws Exception {
        createUserRole("Internist", false);
        createUserRole("Cardiologist", true);
    }

    @Test
    public void createRole_ObjectAlreadyExists() throws Exception {
        createUserRole("Internist", false);
        try {
            createUserRole("Internist", false);
            fail("Should throw ObjectAlreadyExistsException");
        }
        catch (ObjectAlreadyExistsException e) {}
    }

    @Test
    public void getParent_ObjectNotFoundException() {
        RolePath rp = new RolePath("Clerk/Secretary".split("/"), false);
        try {
            rp.getParent();
            fail("Should throw ObjectNotFoundException");
        }
        catch (ObjectNotFoundException e) {}
    }

    @Test
    public void addRole_ObjectCannotBeUpdated() throws Exception {
        RolePath internist = createUserRole("Internist", false);
        lookup.addRole(jim, internist);
        try {
            lookup.addRole(jim, internist);
            fail("Should throw ObjectCannotBeUpdated exception");
        }
        catch (ObjectCannotBeUpdated e) {}
    }

    @Test
    public void removeRole_ObjectCannotBeUpdated() throws Exception {
        RolePath internist = createUserRole("Internist", false);
        lookup.addRole(jim, internist);
        lookup.removeRole(jim, internist);
        try {
            lookup.removeRole(jim, internist);
            fail("Should throw ObjectCannotBeUpdated exception");
        }
        catch (ObjectCannotBeUpdated e) {}
    }

    @Test
    public void addTwoRolesToAgent() throws Exception {
        RolePath internist    = createUserRole("Internist", false);
        RolePath cardiologist = createUserRole("Cardiologist", false);

        lookup.addRole(jim, internist);
        lookup.addRole(jim, cardiologist);

        assert lookup.hasRole(jim, internist);
        assert lookup.hasRole(jim, cardiologist);

        compare(Arrays.asList(internist, cardiologist), lookup.getRoles(jim));
        compare(Arrays.asList(jim),                     lookup.getAgents(internist));
        compare(Arrays.asList(jim),                     lookup.getAgents(cardiologist));
    }

    @Test
    public void addTwoAgentsToRootRole() throws Exception {
        lookup.addRole(jim, user);
        lookup.addRole(tom, user);

        assert lookup.hasRole(jim, user);
        assert lookup.hasRole(tom, user);

        compare(Arrays.asList(jim, tom),  lookup.getAgents(user));
        compare(Arrays.asList(user), lookup.getRoles(jim));
        compare(Arrays.asList(user), lookup.getRoles(tom));

        assertReflectionEquals(user, lookup.getRolePath("User"));
    }

    @Test
    public void addTwoAgentsToSubRole() throws Exception {
        RolePath internist = createUserRole("Internist", false);

        lookup.addRole(jim, internist);
        lookup.addRole(tom, internist);

        assert lookup.hasRole(jim, internist);
        assert lookup.hasRole(tom, internist);

        compare(Arrays.asList(jim, tom),  lookup.getAgents(internist));
        compare(Arrays.asList(internist), lookup.getRoles(jim));
        compare(Arrays.asList(internist), lookup.getRoles(tom));

        assertReflectionEquals(internist, lookup.getRolePath("Internist"));
    }

    @Test
    public void addRemoveRole() throws Exception {
        RolePath internist    = createUserRole("Internist", false);
        RolePath cardiologist = createUserRole("Cardiologist", false);

        lookup.addRole(jim, internist);
        lookup.addRole(jim, cardiologist);
        lookup.addRole(tom, cardiologist);

        assert lookup.hasRole(jim, internist);
        assert lookup.hasRole(jim, cardiologist);

        compare(Arrays.asList(internist, cardiologist), lookup.getRoles(jim));
        compare(Arrays.asList(cardiologist),            lookup.getRoles(tom));
        compare(Arrays.asList(jim),                     lookup.getAgents(internist));
        compare(Arrays.asList(jim, tom),                lookup.getAgents(cardiologist));

        lookup.removeRole(jim, cardiologist);

        assert   lookup.hasRole(jim, internist);
        assert ! lookup.hasRole(jim, cardiologist);

        compare(Arrays.asList(internist),    lookup.getRoles(jim));
        compare(Arrays.asList(cardiologist), lookup.getRoles(tom));
        compare(Arrays.asList(jim),          lookup.getAgents(internist));
        compare(Arrays.asList(tom),          lookup.getAgents(cardiologist));
    }

    @Test
    public void deleteRole() throws Exception {
        RolePath internist = createUserRole("Internist", false);
        lookup.addRole(jim, internist);
        compare(Arrays.asList(internist), lookup.getRoles(jim));

        lookup.delete(internist);
        assert ! lookup.exists(internist);
        compare(Arrays.asList(), lookup.getRoles(jim));
    }

    @Test
    public void checkRoleHierarchy() throws Exception {
        AgentPath sam = new AgentPath(new ItemPath(), "Sam");
        lookup.add(sam);

        RolePath internist = createUserRole("Internist", false);
        lookup.addRole(sam, user);
        lookup.addRole(jim, internist);
        lookup.addRole(tom, internist);

        compare(Arrays.asList(sam),       lookup.getAgents(user));
        compare(Arrays.asList(jim,tom),   lookup.getAgents(internist));

        compare(Arrays.asList(internist), lookup.getChildren(user));
    }

    @Test
    public void getAgentsPaged() throws Exception {
        List<Path> expecteds = new ArrayList<>();
        RolePath paged = new RolePath(new RolePath(), "paged");
        lookup.add(paged);

        for (int i = 0; i < 35; i++) {
            AgentPath ap = new AgentPath(UUID.randomUUID(), "agent" + StringUtils.leftPad(""+i, 2, "0"));
            lookup.add(ap);
            lookup.addRole(ap, paged);
            expecteds.add(ap);
        }

        PagedResult actuals = null;

        for (int i = 0; i < 3; i++) {
            actuals = lookup.getAgents(paged, i*10, 10);

            assertReflectionEquals(expecteds.subList(i*10, i*10+10), actuals.rows);
            assertEquals(expecteds.size(), actuals.maxRows);
        }

        actuals = lookup.getAgents(paged, 30, 10);

        assertReflectionEquals(expecteds.subList(30, 35), actuals.rows);
        assertEquals(expecteds.size(), actuals.maxRows);
    }

    @Test
    public void addRolesOfAgentPaged() throws Exception {
        AgentPath paged = new AgentPath(new ItemPath(), "paged");
        lookup.add(paged);

        List<Path> expecteds = new ArrayList<>();

        for (int i = 0; i < 35; i++) {
            RolePath rp = new RolePath(new RolePath(), "paged" + StringUtils.leftPad(""+i, 2, "0"));
            lookup.add(rp);
            lookup.addRole(paged, rp);
            expecteds.add(rp);
        }

        PagedResult actuals = null;

        for (int i = 0; i < 3; i++) {
            actuals = lookup.getRoles(paged, i*10, 10);

            assertReflectionEquals(expecteds.subList(i*10, i*10+10), actuals.rows);
            assertEquals(expecteds.size(), actuals.maxRows);
        }

        actuals = lookup.getRoles(paged, 30, 10);

        assertReflectionEquals(expecteds.subList(30, 35), actuals.rows);
        assertEquals(expecteds.size(), actuals.maxRows);
    }
}
