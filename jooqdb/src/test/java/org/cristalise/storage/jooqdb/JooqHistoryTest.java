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
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.storage.jooqdb.clusterStore.JooqHistoryHandler;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqHistoryTest extends StorageTestBase {
    Event event;
    JooqHistoryHandler jooq;

    @Before
    public void before() throws Exception {
        context = initJooqContext();

        jooq = new JooqHistoryHandler();
        jooq.createTables(context);

        event = createEvent(uuid);
        assert jooq.put(context, uuid, event) == 1;
    }

    @After
    public void after() throws Exception {
        context.close();
        
        if (dbType == MYSQL || dbType == PostgreSQL) jooq.dropTables(context);
    }

    private void compareEvents(Event actual, Event expected) {
        Assert.assertNotNull(actual);

        Assert.assertEquals(expected.getID(),                   actual.getID());
        Assert.assertEquals(expected.getItemPath(),             actual.getItemPath());
        Assert.assertEquals(expected.getAgentPath(),            actual.getAgentPath());
        Assert.assertEquals(expected.getAgentRole(),            actual.getAgentRole());
        Assert.assertEquals(expected.getName(),                 actual.getName());
        Assert.assertEquals(expected.getSchemaName(),           actual.getSchemaName());
        Assert.assertEquals(expected.getSchemaVersion(),        actual.getSchemaVersion());
        Assert.assertEquals(expected.getStateMachineName(),     actual.getStateMachineName());
        Assert.assertEquals(expected.getStateMachineVersion(),  actual.getStateMachineVersion());
        Assert.assertEquals(expected.getStepName(),             actual.getStepName());
        Assert.assertEquals(expected.getStepPath(),             actual.getStepPath());
        Assert.assertEquals(expected.getStepType(),             actual.getStepType());
        Assert.assertEquals(expected.getOriginState(),          actual.getOriginState());
        Assert.assertEquals(expected.getTargetState(),          actual.getTargetState());
        Assert.assertEquals(expected.getTransition(),           actual.getTransition());
        Assert.assertEquals(expected.getViewName(),             actual.getViewName());

        compareTimestramps(actual.getTimeStamp(), expected.getTimeStamp());
    }

    private Event createEvent(UUID itemUUID) throws InvalidItemPathException {
        return createEvent(itemUUID, jooq.getLastEventId(context, uuid)+1);
    }

    private Event createEvent(UUID itemUUID, int id) throws InvalidItemPathException {
        return new Event(
                id,
                new ItemPath(itemUUID), 
                new AgentPath(new ItemPath(), "agent"),
                "role", 
                "stepName"+id, 
                "stepaPath"+id,
                "stepType"+id,
                "Default",
                id+1, //smVersion
                id+2, //transitionId
                id+3, //originState
                id+4, //targetState
                "schemaname"+id,
                id+5, //schemaVersion
                "last", //viewName
                DateUtility.getNow());
    }

    private Event createEventNullable(UUID itemUUID, int id) throws InvalidItemPathException {
        return new Event(
                id,
                new ItemPath(itemUUID), 
                new AgentPath(new ItemPath(), "agent"),
                null, 
                "stepName"+id, 
                "stepaPath"+id,
                null,
                "Default",
                0, //smVersion
                0, //transitionId
                1, //originState
                0, //targetState
                null,
                0, //schemaVersion
                null, //viewName
                DateUtility.getNow());
    }

    @Test
    public void fetchEvent() throws Exception {
        compareEvents((Event)jooq.fetch(context, uuid, "0"), event);
    }

    @Test
    public void fetchEventWithNulls() throws Exception {
        Event eventNulls = createEventNullable(uuid, 1);
        assert jooq.put(context, uuid, eventNulls) == 1;
        compareEvents((Event)jooq.fetch(context, uuid, "1"), eventNulls);
    }

    @Test(expected=PersistencyException.class)
    public void updateEvent_ThrowsException() throws Exception {
        jooq.put(context, uuid, event);
    }

    @Test
    public void delete() throws Exception {
        assert jooq.put(context, uuid, createEvent(uuid, 1)) == 1;
        assert jooq.put(context, uuid, createEvent(uuid, 2)) == 1;
        assert jooq.put(context, uuid, createEvent(uuid, 3)) == 1;

        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid2, createEvent(uuid2, 0)) == 1;
        assert jooq.put(context, uuid2, createEvent(uuid2, 1)) == 1;
        assert jooq.put(context, uuid2, createEvent(uuid2, 2)) == 1;

        Assert.assertEquals(4, jooq.delete(context, uuid));

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);
        Assert.assertEquals(0, keys.length);

        keys = jooq.getNextPrimaryKeys(context, uuid2);

        Assert.assertEquals(3, keys.length);
    }

    @Test
    public void getEventIDs() throws Exception {
        assertEquals(0, jooq.getLastEventId(context, uuid)); //event 0 is created in before()
        assert jooq.put(context, uuid, createEvent(uuid)) == 1;
        assert jooq.put(context, uuid, createEvent(uuid)) == 1;
        assert jooq.put(context, uuid, createEvent(uuid)) == 1;
        assertEquals(3, jooq.getLastEventId(context, uuid));

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);
        assertThat(Arrays.asList(keys), IsIterableContainingInAnyOrder.containsInAnyOrder("0", "1", "2", "3"));

        UUID uuid2 = UUID.randomUUID();
        assertEquals(-1, jooq.getLastEventId(context, uuid2));
        assert jooq.put(context, uuid2, createEvent(uuid2, 0)) == 1;
        assert jooq.put(context, uuid2, createEvent(uuid2, 1)) == 1;
        assert jooq.put(context, uuid2, createEvent(uuid2, 2)) == 1;
        assertEquals(2, jooq.getLastEventId(context, uuid2));
    }
}
