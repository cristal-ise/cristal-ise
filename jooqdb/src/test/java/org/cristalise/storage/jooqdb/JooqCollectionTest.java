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
package org.cristalise.storage.jooqdb;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.storage.jooqdb.clusterStore.JooqCollectionHadler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqCollectionTest extends JooqTestBase {

    JooqCollectionHadler jooq;

    @Before
    public void before() throws Exception {
        initH2();

        jooq = new JooqCollectionHadler();
        jooq.createTables(context);
    }

    private void compareCollections(Collection<?> expected, Collection<?> actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getVersionName(), actual.getVersionName());
    }

    private Dependency addDependency(String name, Integer version) throws Exception {
        Dependency d = new Dependency(name, version);
        assert jooq.put(context, uuid, d) == 1;
        return d;
    }

    @Test
    public void fetch() throws Exception {
        Dependency d = addDependency("TestDependency", null);
        Dependency d1 = (Dependency) jooq.fetch(context, uuid, "TestDependency", "last");
        compareCollections(d, d1);

        d = addDependency("TestDependency", 0);
        d1 = (Dependency) jooq.fetch(context, uuid, "TestDependency", "0");
        compareCollections(d, d1);
    }

    @Test
    public void delete() throws Exception {
        addDependency("Test1", 1);
        addDependency("Test2", null);
        assert jooq.delete(context, uuid) == 2;
    }

    @Test
    public void getNames() throws Exception {
        addDependency("Test1", 1);
        addDependency("Test2", null);

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);

        Assert.assertEquals(2, keys.length);
        Assert.assertEquals("Test1", keys[0]);
        Assert.assertEquals("Test2", keys[1]);
    }

    @Test
    public void getVersions() throws Exception {
        addDependency("Test", 0);
        addDependency("Test", null);

        String[] keys = jooq.getNextPrimaryKeys(context, uuid, "Test");

        Assert.assertEquals(2, keys.length);
        Assert.assertEquals("last", keys[0]);
        Assert.assertEquals("0", keys[1]);
    }

    @Test
    public void getVersion() throws Exception {
        addDependency("Test", 0);
        addDependency("Test", null);

        String[] keys = jooq.getNextPrimaryKeys(context, uuid, "Test", "0");

        Assert.assertEquals(1, keys.length);
        Assert.assertEquals("0", keys[0]);
    }
}
