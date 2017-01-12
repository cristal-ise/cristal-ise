package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.cristalise.kernel.property.Property;
import org.cristalise.storage.jooqdb.JOOQItemProperty;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.Test;

public class JOOQItemPropertyTest {
    DSLContext context;
    UUID       uuid = UUID.randomUUID();
    Property   property = new Property("toto", "value", true);

    JOOQItemProperty jooq;

    private void compareProperties(Property actual, Property expected) {
        assert actual.getName().equals(expected.getName());
        assert actual.getValue().equals(expected.getValue());
        assert actual.isMutable() == expected.isMutable();
    }

    @Before
    public void before() {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        try {
            Connection conn = DriverManager.getConnection(url, userName, password);
            context = using(conn, SQLDialect.H2);

            jooq = new JOOQItemProperty();
            jooq.createTables(context);
            assert jooq.put(context, uuid, property) == 1;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void fetchProperty() throws Exception {
        compareProperties(jooq.fetch(context, uuid, "toto"), property);
    }

    @Test
    public void updateProperty() throws Exception {
        Property propertyPrime = new Property("toto", "valueNew", true);
        assert jooq.put(context, uuid, propertyPrime) == 1;

        compareProperties(jooq.fetch(context, uuid, "toto"), propertyPrime);
    }

    @Test
    public void twoProperties() throws Exception {
        Property property2 = new Property("zaza", "value", false);
        assert jooq.put(context, uuid, property2) == 1;

        compareProperties(jooq.fetch(context, uuid, "toto"), property);
        compareProperties(jooq.fetch(context, uuid, "zaza"), property2);
    }
}
