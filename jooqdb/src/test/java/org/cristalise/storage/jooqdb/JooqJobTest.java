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
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.storage.jooqdb.clusterStore.JooqJobHandler;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqJobTest extends StorageTestBase {
    Job job;
    JooqJobHandler jooq;
    CastorHashMap actProps;

    @Before
    public void before() throws Exception {
        context = initJooqContext();

        actProps = new CastorHashMap();
        actProps.setBuiltInProperty(BuiltInVertexProperties.STATE_MACHINE_NAME, "Default");
        actProps.setBuiltInProperty(BuiltInVertexProperties.STATE_MACHINE_VERSION, "0");

        jooq = new JooqJobHandler();
        jooq.createTables(context);

        job = createJob(uuid, 0);
        assert jooq.put(context, uuid, job) == 1;
    }

    @After
    public void after() throws Exception {
        jooq.delete(context, uuid);

        if (dbType == MYSQL || dbType == PostgreSQL) jooq.dropTables(context);
    }

    private void compareJobs(Job actual, Job expected) throws Exception {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.getItemPath(),        actual.getItemPath());
        Assert.assertEquals(expected.getName(),            actual.getName());
        Assert.assertEquals(expected.getSchemaName(),      actual.getSchemaName());
        Assert.assertEquals(expected.getSchemaVersion(),   actual.getSchemaVersion());
        Assert.assertEquals(expected.getStepName(),        actual.getStepName());
        Assert.assertEquals(expected.getStepPath(),        actual.getStepPath());
        Assert.assertEquals(expected.getStepType(),        actual.getStepType());

        compareTimestramps(actual.getCreationDate(), expected.getCreationDate());
    }

    private Job createJob(UUID itemUUID, int idx) throws InvalidItemPathException {
        return createJob(itemUUID, idx, "Done");
    }

    private Job createJob(UUID itemUUID, int idx, String transition) throws InvalidItemPathException {
        return new Job(
                new ItemPath(itemUUID),
                "stepName"+idx, 
                "stepaPath"+idx,
                "stepType"+idx,
                transition,
                "admin",
                actProps, 
                DateUtility.getNow());
    }

    @Test
    public void fetchJob() throws Exception {
        compareJobs((Job)jooq.fetch(context, uuid, "stepName0", "Done"), job);
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
    public void getStepNames() throws Exception {
        assert jooq.put(context, uuid, createJob(uuid, 1, "Done")) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 1, "Start")) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 2)) == 1;
        assert jooq.put(context, uuid, createJob(uuid, 3)) == 1;

        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid2, createJob(uuid2, 0)) == 1;
        assert jooq.put(context, uuid2, createJob(uuid2, 1)) == 1;
        assert jooq.put(context, uuid2, createJob(uuid2, 2)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);
        assertThat(Arrays.asList(keys), IsIterableContainingInAnyOrder.containsInAnyOrder("stepName0", "stepName1", "stepName2", "stepName3"));

        keys = jooq.getNextPrimaryKeys(context, uuid, "stepName0");
        assertThat(Arrays.asList(keys), IsIterableContainingInAnyOrder.containsInAnyOrder("Done"));

        keys = jooq.getNextPrimaryKeys(context, uuid, "stepName1");
        assertThat(Arrays.asList(keys), IsIterableContainingInAnyOrder.containsInAnyOrder("Done", "Start"));
    }
}
