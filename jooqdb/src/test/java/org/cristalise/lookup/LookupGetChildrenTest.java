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
import static org.junit.Assume.assumeTrue;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.unitils.reflectionassert.ReflectionComparatorMode.LENIENT_ORDER;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.JooqTestConfigurationBase;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.junit.Before;
import org.junit.Test;

public class LookupGetChildrenTest extends LookupTestBase {

    UUID uuid0 = new UUID(0,0);
    UUID uuid1 = new UUID(0,1);
    UUID uuid2 = new UUID(0,2);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        lookup.add( new ItemPath(uuid0) );
        lookup.add( new AgentPath(uuid1, "Jim") );
        lookup.add( new AgentPath(uuid2, "John") );
        lookup.add( new DomainPath("empty/nothing") );
        lookup.add( new DomainPath("empty/something/uuid0", lookup.getItemPath(uuid0.toString())) );
        lookup.add( new DomainPath("special/something/special||Chars[escaped] *%._\\\\") );
        //lookup.add( new DomainPath("empty.old/something/uuid1", lookup.getItemPath(uuid1.toString())) );
        lookup.add( new RolePath(new RolePath(),               "User") );
        lookup.add( new RolePath(new RolePath("User"),         "SubUser") );
        lookup.add( new RolePath(new RolePath("User/SubUser"), "DummyUser") );
        lookup.add( new RolePath(new RolePath("User"),         "LowerUser") );
    }

    @Test
    public void newDomainPath() throws Exception {
        lookup.add( new DomainPath("dummy") );

        compare(Arrays.asList(new DomainPath("empty"), new DomainPath("dummy"), new DomainPath("special")), lookup.getChildren(new DomainPath()) );

        compare(Arrays.asList(new DomainPath("empty/nothing"),  new DomainPath("empty/something")),
                lookup.getChildren(new DomainPath("empty")));
    }

    @Test
    public void newDomainPathUseSpecialChars() throws Exception {
        assumeTrue("This case only works with psql", JooqTestConfigurationBase.dbType == DBModes.PostgreSQL);

        DomainPath dp       = new DomainPath("special/something/special||Chars[escaped] *%._\\\\");
        DomainPath dp_child = new DomainPath("special/something/special||Chars[escaped] *%._\\\\/dummy");

        compare(new ArrayList<Path>(), lookup.getChildren(dp));

        lookup.add(dp_child);
        compare(Arrays.asList(dp_child), lookup.getChildren(dp));
    }

    @Test
    public void newRolePath() throws Exception {
        compare(Arrays.asList(new RolePath(new RolePath("User", false), "SubUser"),  new RolePath(new RolePath("User", false), "LowerUser")),
                lookup.getChildren(new RolePath(new RolePath(), "User")));
    }

    @Test
    public void newDomainPathUseWithDots() throws Exception {
        lookup.add( new DomainPath("empty/nothing.old") );
        lookup.add( new DomainPath("empty/nothing.new/toto") );

        compare(Arrays.asList(new DomainPath("empty/nothing"),
                new DomainPath("empty/something"),
                new DomainPath("empty/nothing.old"),
                new DomainPath("empty/nothing.new")),
                lookup.getChildren(new DomainPath("empty")));
    }

    @Test
    public void domainPathUsePagedResult() throws Exception {
        List<Path> expecteds = new ArrayList<>();

        for (int i = 0; i < 35; i++) {
            DomainPath dp = new DomainPath("paged/child" + StringUtils.leftPad(""+i, 2, "0"));
            lookup.add(dp);
            expecteds.add(dp);
        }

        PagedResult actuals = null;

        for (int i = 0; i < 3; i++) {
            actuals = lookup.getChildren(new DomainPath("paged"), i*10, 10);

            assertReflectionEquals(expecteds.subList(i*10, i*10+10), actuals.rows, LENIENT_ORDER);
            assertEquals(35, actuals.maxRows);
        }

        actuals = lookup.getChildren(new DomainPath("paged"), 30, 10);

        assertReflectionEquals(expecteds.subList(30, 35), actuals.rows, LENIENT_ORDER);
        assertEquals(35, actuals.maxRows);
    }

    @Test
    public void rolePathUsePagedResult() throws Exception {
        List<Path> expecteds = new ArrayList<>();
        RolePath paged = new RolePath(new RolePath(), "paged");
        lookup.add(paged);

        for (int i = 0; i < 35; i++) {
            RolePath rp = new RolePath(paged, "child" + StringUtils.leftPad(""+i, 2, "0"));
            lookup.add(rp);
            expecteds.add(rp);
        }

        PagedResult actuals = null;

        for (int i = 0; i < 3; i++) {
            actuals = lookup.getChildren(paged, i*10, 10);

            assertReflectionEquals(expecteds.subList(i*10, i*10+10), actuals.rows, LENIENT_ORDER);
            assertEquals(35, actuals.maxRows);
        }

        actuals = lookup.getChildren(paged, 30, 10);

        assertReflectionEquals(expecteds.subList(30, 35), actuals.rows, LENIENT_ORDER);
        assertEquals(35, actuals.maxRows);
    }
}
