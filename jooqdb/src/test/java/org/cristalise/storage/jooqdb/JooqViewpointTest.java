package org.cristalise.storage.jooqdb;

import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.Before;
import org.junit.Test;

public class JooqViewpointTest {
    DSLContext context;
    UUID       uuid = UUID.randomUUID();
    Viewpoint  viewpoint;

    JooqViewpoint jooq;

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
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);

        jooq = new JooqViewpoint();
        jooq.createTables(context);

        viewpoint = new Viewpoint(new ItemPath(uuid), "SchemaName", "Name", 0, 1);
        assert jooq.put(context, uuid, viewpoint) == 1;
    }

    @Test
    public void fetchViewpoint() throws Exception {
        compareViewpoints(jooq.fetch(context, uuid, "SchemaName", "Name"), viewpoint);
    }

    @Test
    public void updateViewpoint() throws Exception {
        Viewpoint updatedViewpoint = new Viewpoint(new ItemPath(uuid), "SchemaName", "Name", 0, 10);
        assert jooq.put(context, uuid, updatedViewpoint) == 1;
        compareViewpoints(jooq.fetch(context, uuid, "SchemaName", "Name"), updatedViewpoint);
    }
}
