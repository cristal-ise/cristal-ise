package org.cristalise.storage.jooqdb;

import java.util.UUID;

import org.cristalise.kernel.property.Property;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqItemPropertyTest extends JooqTestBase {

    Property property;
    JooqItemPropertyHandler jooq;

    private void compareProperties(Property actual, Property expected) {
        assert actual.getName().equals(expected.getName());
        assert actual.getValue().equals(expected.getValue());
        assert actual.isMutable() == expected.isMutable();
    }

    @Before
    public void before() throws Exception {
        super.before();

        jooq = new JooqItemPropertyHandler();
        jooq.createTables(context);

        property = new Property("toto", "value", true);
        assert jooq.put(context, uuid, property) == 1;
    }

    @Test
    public void fetchProperty() throws Exception {
        compareProperties((Property)jooq.fetch(context, uuid, "toto"), property);
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

        Assert.assertEquals(1, keys.length);
        Assert.assertEquals("zaza", keys[0]);
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
}
