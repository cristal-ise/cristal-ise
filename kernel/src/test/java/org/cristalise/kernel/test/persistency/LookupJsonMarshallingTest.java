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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.entity.DomainContext;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.lookup.SearchFilter;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.utils.VertxJsonMarshaller;
import org.junit.Test;

import io.vertx.core.json.JsonObject;

@Slf4j
public class LookupJsonMarshallingTest {

    private final VertxJsonMarshaller serializer = new VertxJsonMarshaller();

    @Test
    public void testItemPathMarshalling() throws Exception {
        UUID uuid = UUID.randomUUID();
        ItemPath path = new ItemPath(uuid);
        
        String json = serializer.marshall(path);
        JsonObject jo = new JsonObject(json);
        
        assertEquals("/entity/" + uuid.toString(), jo.getString("path"));
        assertEquals(1, jo.size());
        
        ItemPath roundTrip = serializer.unmarshall(json, ItemPath.class);
        assertEquals(path.getStringPath(), roundTrip.getStringPath());
    }

    @Test
    public void testAgentPathMarshalling() throws Exception {
        UUID uuid = UUID.randomUUID();
        String agentName = "testAgent";
        AgentPath path = new AgentPath(uuid, agentName);
        
        String json = serializer.marshall(path);
        JsonObject jo = new JsonObject(json);
        
        assertEquals("/entity/" + uuid.toString(), jo.getString("path"));
        assertEquals(agentName, jo.getString("agentName"));
        assertEquals(2, jo.size());
        
        AgentPath roundTrip = serializer.unmarshall(json, AgentPath.class);
        assertEquals(path.getStringPath(), roundTrip.getStringPath());
        assertEquals(path.getAgentName(), roundTrip.getAgentName());
    }

    @Test
    public void testDomainPathMarshalling() throws Exception {
        String pathStr = "/domain/a/b/c";
        UUID targetUuid = UUID.randomUUID();
        DomainPath origPath = new DomainPath(pathStr);
        origPath.setTargetUUID(targetUuid.toString());
        
        String jsonString = serializer.marshall(origPath);
        JsonObject json = new JsonObject(jsonString);

        assertEquals(pathStr, json.getString("path"));
        assertEquals(targetUuid.toString(), json.getString("target"));

        DomainPath roundTrip = serializer.unmarshall(jsonString, DomainPath.class);
        assertEquals(origPath.getStringPath(), roundTrip.getStringPath());
        assertEquals(origPath.getTargetUUID(), roundTrip.getTargetUUID());
    }

    @Test
    public void testRolePathMarshalling() throws Exception {
        String pathStr = "/role/admin";
        RolePath path = new RolePath(pathStr);
        path.setHasJobList(true);
        path.setPermissions(Arrays.asList("READ", "WRITE"));
        
        String jsonString = serializer.marshall(path);
        JsonObject json = new JsonObject(jsonString);
        
        assertEquals(pathStr, json.getString("path"));
        assertTrue(json.getBoolean("hasJobList"));
//        assertNotNull(json.getJsonArray("permissions"));
        assertEquals(2, json.getJsonArray("permissions").size());
        
        RolePath roundTrip = serializer.unmarshall(jsonString, RolePath.class);
        assertEquals(path.getStringPath(), roundTrip.getStringPath());
        assertEquals(path.hasJobList(), roundTrip.hasJobList());
        Set<String> perms = roundTrip.getPermissions();
        assertTrue(perms.contains("READ"));
        assertTrue(perms.contains("WRITE"));
    }

    @Test
    public void testSearchFilterMarshalling() throws Exception {
        SearchFilter filter = new SearchFilter();
        filter.setSearchRoot("/entity");
        filter.setRecordsFound(10);
        filter.getProperties().add(new Property("p1", "v1"));
        
        String json = serializer.marshall(filter);
        JsonObject jo = new JsonObject(json);
        
        assertEquals("/entity", jo.getString("searchRoot"));
        assertEquals(10, jo.getInteger("recordsFound").intValue());
        assertNotNull(jo.getJsonArray("properties"));
        assertEquals(1, jo.getJsonArray("properties").size());
        
        SearchFilter roundTrip = serializer.unmarshall(json, SearchFilter.class);
        assertEquals(filter.getSearchRoot(), roundTrip.getSearchRoot());
        assertEquals(filter.getRecordsFound(), roundTrip.getRecordsFound());
        assertEquals(1, roundTrip.getProperties().size());
        assertEquals("p1", roundTrip.getProperties().get(0).getName());
        assertEquals("v1", roundTrip.getProperties().get(0).getValue());
    }

    @Test
    public void testDomainContextMarshalling() throws Exception {
        String domainPath = "/test";
        DomainContext context = new DomainContext(domainPath);
        // name is generated as TestContext
        
        String json = serializer.marshall(context);
        JsonObject jo = new JsonObject(json);
        
        assertEquals(domainPath, jo.getString("domainPath"));
        assertEquals("TestContext", jo.getString("name"));
        
        DomainContext roundTrip = serializer.unmarshall(json, DomainContext.class);
        assertEquals(context.getDomainPath(), roundTrip.getDomainPath());
        assertEquals(context.getName(), roundTrip.getName());
    }
}
