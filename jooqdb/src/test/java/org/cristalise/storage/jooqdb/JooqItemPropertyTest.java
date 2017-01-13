package org.cristalise.storage.jooqdb;

import org.cristalise.kernel.property.Property;
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
}
