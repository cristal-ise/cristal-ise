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
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.utils.DateUtility;
import org.cristalise.kernel.utils.Logger;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JooqHistoryTest extends JooqTestBase {
    Event event;
    JooqHistoryHandler jooq;

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

        if (expected.getDelegatePath() != null)
            Assert.assertEquals(expected.getDelegatePath(), actual.getDelegatePath());

        compareTimestramps(expected.getTimeStamp(), actual.getTimeStamp());
    }

    private Event createEvent(UUID itemUUID, int id) throws InvalidItemPathException {
        return new Event(
                id,
                new ItemPath(itemUUID), 
                new AgentPath(new ItemPath(), "agent"),
                null, 
                "role", 
                "stepName"+id, 
                "stepaPath"+id,
                "stepType"+id,
                "Default",
                0, //smVersion
                0, //transitionId
                1, //originState
                0, //targetState
                "schemaname"+id,
                0, //schemaVersion
                "last", //viewName
                DateUtility.getNow());
    }

    @Before
    public void before() throws Exception {
        super.before();

        jooq = new JooqHistoryHandler();
        jooq.createTables(context);

        event = createEvent(uuid, 0);
        assert jooq.put(context, uuid, event) == 1;
    }

    @Test
    public void fetchEvent() throws Exception {
        compareEvents((Event)jooq.fetch(context, uuid, "0"), event);
    }

    @Test(expected=PersistencyException.class)
    public void updateEvent() throws Exception {
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
        assert jooq.put(context, uuid, createEvent(uuid, 1)) == 1;
        assert jooq.put(context, uuid, createEvent(uuid, 2)) == 1;
        assert jooq.put(context, uuid, createEvent(uuid, 3)) == 1;

        UUID uuid2 = UUID.randomUUID();
        assert jooq.put(context, uuid2, createEvent(uuid2, 0)) == 1;
        assert jooq.put(context, uuid2, createEvent(uuid2, 1)) == 1;
        assert jooq.put(context, uuid2, createEvent(uuid2, 2)) == 1;

        String[] keys = jooq.getNextPrimaryKeys(context, uuid);
        Logger.msg(Arrays.toString(keys));

        Assert.assertThat(Arrays.asList(keys), IsIterableContainingInAnyOrder.containsInAnyOrder("0", "1", "2", "3"));
    }
}
