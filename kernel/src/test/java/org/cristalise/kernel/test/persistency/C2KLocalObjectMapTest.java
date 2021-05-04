/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.test.persistency;

import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Viewpoint;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class C2KLocalObjectMapTest {
    static ItemPath itemPath;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mStorage", new ClusterStorageManager(null), true);
        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    @Test
    public void checkPropertyMap() throws Exception {
        C2KLocalObjectMap<Property> propertyMap = new C2KLocalObjectMap<>(itemPath, PROPERTY);

        Set<String> keys = propertyMap.keySet();

        assertEquals(19, keys.size());
        assertNotNull(propertyMap.get("Name"));
        assertTrue(keys.contains("Name"));
        assertTrue(propertyMap.containsKey("Name"));
    }

    @Test
    public void checkViewpointMap() throws Exception {
        C2KLocalObjectMap<Viewpoint> viewpointMap = new C2KLocalObjectMap<>(itemPath, VIEWPOINT);

        Set<String> keys = viewpointMap.keySet();

        assertEquals(14, keys.size());
        assertNotNull(viewpointMap.get("NextStepData/last"));
        assertTrue(keys.contains("NextStepData/last"));
        assertTrue(viewpointMap.containsKey("NextStepData/last"));
    }

    @Test
    public void checkOutcomeMap() throws Exception {
        C2KLocalObjectMap<Outcome> outcomeMap = new C2KLocalObjectMap<>(itemPath, OUTCOME);

        Set<String> keys = outcomeMap.keySet();

        assertEquals(27, keys.size());
        //assertNotNull(outcomeMap.get("DispensingData/0/18")); //mock LocalObjectLoader to return the Schema
        assertTrue(keys.contains("DispensingData/0/18"));
        assertTrue(outcomeMap.containsKey("DispensingData/0/18"));
    }

    @Test
    public void checkHistory() throws Exception {
        History history = new History(itemPath);

        Set<String> keys = history.keySet();

        assertEquals(30, keys.size());
        assertTrue(keys.contains("0"));
        assertTrue(history.containsKey("0"));

        try {
            history.values();
            fail("Shall throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e) {}
    }

    @Test
    public void checkEventMap() throws Exception {
        C2KLocalObjectMap<Event> eventMap = new C2KLocalObjectMap<>(itemPath, HISTORY);

        Set<String> keys = eventMap.keySet();

        assertEquals(30, keys.size());
        assertEquals(30, eventMap.values().size());
        assertNotNull(eventMap.get("0"));
        assertTrue(keys.contains("0"));
        assertTrue(eventMap.containsKey("0"));
    }

    @Test
    public void checLifecycleMap() throws Exception {
        C2KLocalObjectMap<Workflow> workflowMap = new C2KLocalObjectMap<>(itemPath, LIFECYCLE);

        Set<String> keys = workflowMap.keySet();

        assertEquals(1, keys.size());
        assertNotNull(workflowMap.get("workflow"));
        assertTrue(keys.contains("workflow"));
        assertTrue(workflowMap.containsKey("workflow"));
    }
}
