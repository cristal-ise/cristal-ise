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

import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.RolePath
import org.cristalise.kernel.property.Property
import org.junit.Before
import org.junit.Test

import groovy.transform.CompileStatic


@CompileStatic
class LookupSearchTests extends LookupTestBase {

    UUID uuid0 = new UUID(0,0)
    UUID uuid1 = new UUID(0,1)
    UUID uuid2 = new UUID(0,2)

    @Before
    public void setUp() throws Exception {
        super.setUp()

        lookup.add( new ItemPath(uuid0) )
        lookup.add( new AgentPath(uuid1, "Jim") )
        lookup.add( new AgentPath(uuid2, "John") )
        lookup.add( new DomainPath("empty/nothing") )
        lookup.add( new DomainPath("empty/something/uuid0", lookup.getItemPath(uuid0.toString())) )
        lookup.add( new RolePath(new RolePath(), "User") )
        lookup.add( new RolePath(new RolePath(), "User/SubUser") )
        lookup.add( new RolePath(new RolePath(), "User/SubUser/DummyUser") )
        lookup.add( new RolePath(new RolePath(), "User/LowerUser") )
    }

    @Test
    public void search() {
        def expected = [new DomainPath("empty"), 
                        new DomainPath("empty/nothing"), 
                        new DomainPath("empty/something"), 
                        new DomainPath("empty/something/uuid0", new ItemPath(uuid0))]

        CompareUtils.comparePathLists(expected, lookup.search(new DomainPath("empty"), ""))

        expected = [new DomainPath("empty/something/uuid0", new ItemPath(uuid0))]

        CompareUtils.comparePathLists(expected, lookup.search(new DomainPath("empty"), "uuid0"))
    }

    @Test
    public void searchAliases() {
        lookup.add( new DomainPath("empty/something/uuid0prime", lookup.getItemPath(uuid0.toString())) )

        CompareUtils.comparePathLists(
            [new DomainPath("empty/something/uuid0prime"),  new DomainPath("empty/something/uuid0")], 
            lookup.searchAliases(new ItemPath(uuid0)))
    }

    @Test
    public void getChildren_DomainPath() {
        CompareUtils.comparePathLists(
            [new DomainPath("empty/nothing"),  new DomainPath("empty/something")], 
            lookup.getChildren(new DomainPath("empty")))
    }

    @Test
    public void getChildren_RolePath() {
        CompareUtils.comparePathLists(
            [new RolePath(new RolePath(), "User/SubUser"),  new RolePath(new RolePath(), "User/LowerUser")],
            lookup.getChildren(new RolePath(new RolePath(), "User")))
    }

    @Test
    public void resolvePath() {
        def ip = lookup.resolvePath(new DomainPath("empty/something/uuid0", new ItemPath(uuid0)))
        assert ip
        assert ip.UUID == uuid0
    }

    @Test
    public void getAgentName() {
        assert lookup.getAgentName(new AgentPath(uuid1)) == "Jim"
        assert lookup.getAgentName(new AgentPath(uuid2)) == "John"
    }
}
