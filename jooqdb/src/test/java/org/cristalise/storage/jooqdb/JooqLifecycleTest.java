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

import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer;
import org.cristalise.kernel.lookup.ItemPath;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class JooqLifecycleTest extends JooqTestBase {

    Workflow wf;
    JooqLifecycleHandler jooq;

    @Before
    public void before() throws Exception {
        initH2();

        jooq = new JooqLifecycleHandler();
        jooq.createTables(context);

        wf = new Workflow(new CompositeActivity(), new ServerPredefinedStepContainer());
        wf.initialise(new ItemPath(), null, null);
        assert jooq.put(context, uuid, wf) == 1;
    }

    @Test @Ignore
    public void fetchWorkflow() throws Exception {
        Workflow wfPrime = (Workflow)jooq.fetch(context, uuid);
        assert wfPrime != null;
        //assert "<xml/>".equals(outcomePrime.getData());
    }
}
