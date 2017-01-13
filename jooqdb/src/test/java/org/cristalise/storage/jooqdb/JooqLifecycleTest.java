package org.cristalise.storage.jooqdb;

import java.util.Properties;

import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.junit.Before;
import org.junit.Test;

public class JooqLifecycleTest extends JooqTestBase {

    Workflow wf;
    JooqLifecycleHandler jooq;

    @Before
    public void before() throws Exception {
        Gateway.init(new Properties());

        super.before();

        jooq = new JooqLifecycleHandler();
        jooq.createTables(context);

        wf = new Workflow(new CompositeActivity(), new ServerPredefinedStepContainer());
        wf.initialise(new ItemPath(), null, null);
        assert jooq.put(context, uuid, wf) == 1;
    }

    @Test
    public void fetchWorkflow() throws Exception {
        Workflow wfPrime = (Workflow)jooq.fetch(context, uuid);
        assert wfPrime != null;
        //assert "<xml/>".equals(outcomePrime.getData());
    }
}
