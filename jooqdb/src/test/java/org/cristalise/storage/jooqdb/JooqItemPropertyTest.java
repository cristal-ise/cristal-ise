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

import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.property.Property;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqItemPropertyTest extends StorageTestBase {

    Property property;
    JooqItemPropertyHandler jooq;

    private void compareProperties(Property actual, Property expected) {
        Assert.assertNotNull(actual);

        Assert.assertEquals(expected.getName(),   actual.getName());
        Assert.assertEquals(expected.isMutable(), actual.isMutable());
        Assert.assertEquals(expected.getValue(),  actual.getValue());
    }

    @Before
    public void before() throws Exception {
        context = initJooqContext();

        jooq = new JooqItemPropertyHandler();
        jooq.createTables(context);

        property = new Property("toto", "value", true);
        assert jooq.put(context, uuid, property) == 1;
    }

    @After
    public void after() throws Exception {
        jooq.delete(context, uuid);
    }

    @Test
    public void fetchProperty() throws Exception {
        compareProperties((Property)jooq.fetch(context, uuid, "toto"), property);
    }

    @Test
    public void fetchProperty_NullValue() throws Exception {
        Property propertyNullValue = new Property("zaza", null, true);
        assert jooq.put(context, uuid, propertyNullValue) == 1;
        compareProperties((Property)jooq.fetch(context, uuid, "zaza"), propertyNullValue);
    }

    @Test
    public void updateProperty() throws Exception {
        Property propertyPrime = new Property("toto", "valueNew", true);
        assert jooq.put(context, uuid, propertyPrime) == 1;

        compareProperties((Property)jooq.fetch(context, uuid, "toto"), propertyPrime);
    }

    @Test
    public void twoProperties() throws Exception {
        Property property2 = new Property("zaza", "value", false);
        assert jooq.put(context, uuid, property2) == 1;

        compareProperties((Property)jooq.fetch(context, uuid, "toto"), property);
        compareProperties((Property)jooq.fetch(context, uuid, "zaza"), property2);
    }

    @Test
    public void getPropertyNames() throws Exception {
        assert jooq.put(context, uuid,              new Property("zaza", "value", false)) == 1;
        assert jooq.put(context, UUID.randomUUID(), new Property("mimi", "value", false)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);

        Assert.assertEquals(2, keys.length);
        Assert.assertEquals("toto", keys[0]);
        Assert.assertEquals("zaza", keys[1]);
    }

    @Test
    public void checkName() throws Exception {
        assert jooq.put(context, uuid,              new Property("zaza", "value", false)) == 1;
        assert jooq.put(context, UUID.randomUUID(), new Property("mimi", "value", false)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid, "zaza");

        Assert.assertEquals(0, keys.length);
    }

    @Test
    public void checkName_empty() throws Exception {
        assert jooq.put(context, uuid,              new Property("zaza", "value", false)) == 1;
        assert jooq.put(context, UUID.randomUUID(), new Property("mimi", "value", false)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid, "mimi");

        Assert.assertEquals(0, keys.length);
    }

    @Test
    public void delete() throws Exception {
        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid,  new Property("zaza", "value", false)) == 1;
        assert jooq.put(context, uuid2, new Property("mimi", "value", false)) == 1;

        assert jooq.delete(context, uuid) == 2;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid2);

        Assert.assertEquals(1, keys.length);
        Assert.assertEquals("mimi", keys[0]);
    }

    @Test
    public void findItemByName() throws Exception {
        Property property2 = new Property("Name", "prop2", false);
        assert jooq.put(context, uuid, property2) == 1;

        List<UUID> items = jooq.findItemsByName(context, "prop2");

        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals(uuid, items.get(0));
    }

    @Test
    public void findItems() throws Exception {
        Property property2 = new Property("zaza", "value", false);
        assert jooq.put(context, uuid, property2) == 1;

        List<UUID> items = jooq.findItems(context, property, property2);
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size());
        Assert.assertEquals(uuid, items.get(0));
    }
}
