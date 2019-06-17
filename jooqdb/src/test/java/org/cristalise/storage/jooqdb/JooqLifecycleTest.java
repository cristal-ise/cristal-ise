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

import static org.cristalise.JooqTestConfigurationBase.DBModes.MYSQL;
import static org.cristalise.JooqTestConfigurationBase.DBModes.PostgreSQL;

import java.util.UUID;

import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.storage.jooqdb.clusterStore.JooqLifecycleHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JooqLifecycleTest extends StorageTestBase {

    Workflow wf;
    JooqLifecycleHandler jooq;

    @Before
    public void before() throws Exception {
        context = initJooqContext();

        jooq = new JooqLifecycleHandler();
        jooq.createTables(context);

        wf = new Workflow(new CompositeActivity(), new ServerPredefinedStepContainer());
        wf.initialise(new ItemPath(), new AgentPath(UUID.randomUUID(), "dummy"), null);
        assert jooq.put(context, uuid, wf) == 1;
    }

    @After
    public void after() throws Exception {
        jooq.delete(context, uuid);

        if (dbType == MYSQL || dbType == PostgreSQL) jooq.dropTables(context);
    }

    @Test @Ignore
    public void fetchWorkflow() throws Exception {
        Workflow wfPrime = (Workflow)jooq.fetch(context, uuid);
        assert wfPrime != null;
        //assert "<xml/>".equals(outcomePrime.getData());
    }
}
