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

import java.util.UUID;

import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.storage.jooqdb.clusterStore.JooqViewpointHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqViewpointTest extends JooqTestBase {
    Viewpoint            viewpoint;
    JooqViewpointHandler jooq;

    private void compareViewpoints(Viewpoint actual, Viewpoint expected) {
        assert actual != null;
        assert actual.getItemPath().getStringPath().equals(expected.getItemPath().getStringPath());
        assert actual.getName().equals(expected.getName());
        assert actual.getSchemaName().equals(expected.getSchemaName());
        assert actual.getSchemaVersion() == expected.getSchemaVersion();
        assert actual.getEventId() == expected.getEventId();
    }

    @Before
    public void before() throws Exception {
        initH2();

        jooq = new JooqViewpointHandler();
        jooq.createTables(context);

        viewpoint = new Viewpoint(new ItemPath(uuid), "SchemaName", "Name", 0, 1);
        assert jooq.put(context, uuid, viewpoint) == 1;
    }

    @Test
    public void fetchViewpoint() throws Exception {
        compareViewpoints((Viewpoint)jooq.fetch(context, uuid, "SchemaName", "Name"), viewpoint);
    }

    @Test
    public void updateViewpoint() throws Exception {
        Viewpoint updatedViewpoint = new Viewpoint(new ItemPath(uuid), "SchemaName", "Name", 0, 10);
        assert jooq.put(context, uuid, updatedViewpoint) == 1;
        compareViewpoints((Viewpoint)jooq.fetch(context, uuid, "SchemaName", "Name"), updatedViewpoint);
    }

    @Test
    public void getNextSchemaNames() throws Exception {
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName",  "Name2", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name3", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name4", 0, 1)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);

        assert keys.length == 2;
        assert keys[0].equals("SchemaName");
        assert keys[1].equals("SchemaName2");
    }

    @Test
    public void getNextViewpointNames() throws Exception {
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName",  "Name2", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name3", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name4", 0, 1)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid, "SchemaName");

        assert keys.length == 2;
        assert keys[0].equals("Name");
        assert keys[1].equals("Name2");
    }

    @Test
    public void getSingleNextViewpointName() throws Exception {
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName",  "Name2", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name3", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name4", 0, 1)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid, "SchemaName2", "Name4");

        Assert.assertEquals(1, keys.length);
        Assert.assertEquals("Name4", keys[0]);
    }

    @Test
    public void delete() throws Exception {
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName",  "Name2", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name3", 0, 1)) == 1;
        assert jooq.put(context, uuid, new Viewpoint(new ItemPath(uuid), "SchemaName2", "Name4", 0, 1)) == 1;
        
        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid2, new Viewpoint(new ItemPath(uuid2), "SchemaName",  "Name5", 0, 1)) == 1;
        assert jooq.put(context, uuid2, new Viewpoint(new ItemPath(uuid2), "SchemaName2", "Name6", 0, 1)) == 1;

        Assert.assertEquals(4, jooq.delete(context, uuid));

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);
        Assert.assertEquals(0, keys.length);

        keys = jooq.getNextPrimaryKeys(context, uuid2);

        Assert.assertEquals(2, keys.length);
        Assert.assertEquals("SchemaName", keys[0]);
        Assert.assertEquals("SchemaName2", keys[1]);
    }
}
