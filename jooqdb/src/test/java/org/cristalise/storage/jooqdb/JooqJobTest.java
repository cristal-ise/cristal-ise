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

import java.util.Arrays;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.storage.jooqdb.clusterStore.JooqJobHandler;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqJobTest extends StorageTestBase {
    Job job;
    JooqJobHandler jooq;
    CastorHashMap actProps;

    private void compareJobs(Job actual, Job expected) throws Exception {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getId(),              actual.getId());
        Assert.assertEquals(expected.getItemPath(),        actual.getItemPath());
        Assert.assertEquals(expected.getAgentPath(),       actual.getAgentPath());
        Assert.assertEquals(expected.getName(),            actual.getName());
        Assert.assertEquals(expected.getSchemaName(),      actual.getSchemaName());
        Assert.assertEquals(expected.getSchemaVersion(),   actual.getSchemaVersion());
        Assert.assertEquals(expected.getStepName(),        actual.getStepName());
        Assert.assertEquals(expected.getStepPath(),        actual.getStepPath());
        Assert.assertEquals(expected.getStepType(),        actual.getStepType());
        Assert.assertEquals(expected.getOriginStateName(), actual.getOriginStateName());
        Assert.assertEquals(expected.getTargetStateName(), actual.getTargetStateName());

        if (expected.getDelegatePath() != null)
            Assert.assertEquals(expected.getDelegatePath(), actual.getDelegatePath());

        compareTimestramps(expected.getCreationDate(), actual.getCreationDate());
    }

    private Job createJob(UUID itemUUID, int id) throws InvalidItemPathException {
        return new Job(
                id,
                new ItemPath(UUID.randomUUID()),
                "stepName"+id, 
                "stepaPath"+id,
                "stepType"+id,
                new Transition(0, "Done"),
                "originStateName"+id, 
                "targetStateName"+id,
                "admin",
                new AgentPath(itemUUID),
                null,
                actProps, 
                DateUtility.getNow());
    }

    @Before
    public void before() throws Exception {
        context = initH2Context();

        actProps = new CastorHashMap();
        actProps.setBuiltInProperty(BuiltInVertexProperties.STATE_MACHINE_NAME, "Default");
        actProps.setBuiltInProperty(BuiltInVertexProperties.STATE_MACHINE_VERSION, "0");

        jooq = new JooqJobHandler();
        jooq.createTables(context);

        job = createJob(uuid, 0);
        assert jooq.put(context, uuid, job) == 1;
    }

    @Test
    public void fetchJob() throws Exception {
        compareJobs((Job)jooq.fetch(context, uuid, "0"), job);
    }

    @Test(expected=PersistencyException.class)
    public void updateJob() throws Exception {
        jooq.put(context, uuid, job);
    }

    @Test
    public void delete() throws Exception {
        assert jooq.put(context, uuid, createJob(uuid, 1)) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 2)) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 3)) == 1;

        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid2, createJob(uuid2, 0)) == 1;
        assert jooq.put(context, uuid2, createJob(uuid2, 1)) == 1;
        assert jooq.put(context, uuid2, createJob(uuid2, 2)) == 1;

        Assert.assertEquals(4, jooq.delete(context, uuid));

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);
        Assert.assertEquals(0, keys.length);

        keys = jooq.getNextPrimaryKeys(context, uuid2);

        Assert.assertEquals(3, keys.length);
    }

    @Test
    public void getEventIDs() throws Exception {
        assert jooq.put(context, uuid, createJob(uuid, 1)) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 2)) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 3)) == 1;

        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid2, createJob(uuid2, 0)) == 1;
        assert jooq.put(context, uuid2, createJob(uuid2, 1)) == 1;
        assert jooq.put(context, uuid2, createJob(uuid2, 2)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);

        Assert.assertThat(Arrays.asList(keys), IsIterableContainingInAnyOrder.containsInAnyOrder("0", "1", "2", "3"));
    }
}
