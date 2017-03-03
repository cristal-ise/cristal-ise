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
package org.cristalise.lookup.test

import static org.junit.Assert.*
import groovy.transform.CompileStatic

import org.cristalise.kernel.common.ObjectAlreadyExistsException
import org.cristalise.kernel.common.ObjectCannotBeUpdated
import org.cristalise.kernel.common.SystemKey
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.lookup.LookupManager
import org.cristalise.kernel.lookup.Path
import org.cristalise.kernel.lookup.RolePath
import org.junit.Test


@CompileStatic
class LookupAddPathTests extends LookupTestBase {

    UUID uuid0 = new UUID(0,0)
    SystemKey sysKey0 = new SystemKey(0,0)

    @Test
    public void addDeleteItemPath() {
        Path p = new ItemPath(uuid0.toString())
        assert p.stringPath == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getItemPath(uuid0.toString()).getUUID() == uuid0
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void addDeleteAgentPath() {
        Path p = new AgentPath(new ItemPath(uuid0.toString()), "toto")
        assert p.stringPath == "/entity/${uuid0.toString()}"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getAgentPath("toto")
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void addDeleteDomainPath() {
        Path p = new DomainPath("empty/toto")
        assert p.stringPath == "/domain/empty/toto"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.exists(new DomainPath("empty"))
        assert ! lookup.exists(new DomainPath("toto"))
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void addDeleteRolePath() {
        Path p = new RolePath(new RolePath(), "User")
        assert p.stringPath == "/role/User"
        lookup.add(p)
        assert lookup.exists(p)
        assert lookup.getRolePath("User")
        lookup.delete(p)
        assert ! lookup.exists(p)
    }

    @Test
    public void ObjectAlreadyExistsException() {
        Path p = new DomainPath("emtpy")
        lookup.add(p)
        assert lookup.exists(p)

        try {
            lookup.add(p)
            fail("second add() shall throw ObjectAlreadyExistsException")
        }
        catch (ObjectAlreadyExistsException e) {}
    }

    @Test(expected=ObjectCannotBeUpdated)
    public void deleteDomainPath_ObjectIsNotALeafException() {
        lookup.add(new DomainPath("empty/toto"))
        lookup.delete(new DomainPath("empty"))
    }
}
