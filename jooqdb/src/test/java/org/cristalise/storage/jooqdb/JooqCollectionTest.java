package org.cristalise.storage.jooqdb;

import org.cristalise.kernel.collection.Dependency;
import org.junit.Before;
import org.junit.Test;

public class JooqCollectionTest extends JooqTestBase {

    JooqCollectionHadler jooq;

    @Before
    public void before() throws Exception {
        super.before();

        jooq = new JooqCollectionHadler();
        jooq.createTables(context);
    }

    @Test
    public void fetchDependency() throws Exception {
        Dependency d = new Dependency("TestDependency");
        d.setVersion(0);
        
        assert jooq.put(context, uuid, d) == 1;
        
        Dependency d1 = (Dependency) jooq.fetch(context, uuid, "", "0");
        assert d1 != null;
    }

    @Test
    public void delete() throws Exception {
    }
}
