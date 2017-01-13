package org.cristalise.storage.jooqdb;

import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.junit.Before;
import org.junit.Test;

public class JooqViewpointTest extends JooqTestBase {
    Viewpoint     viewpoint;
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
        super.before();

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
}
