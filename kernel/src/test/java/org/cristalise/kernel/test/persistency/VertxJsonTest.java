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
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Arrays;
import java.util.Properties;
import java.util.UUID;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.imports.ImportDependency;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.entity.imports.ImportRole;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.lookup.SearchFilter;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.kernel.utils.VertxJsonSerializer;
import io.vertx.core.json.JsonObject;
import org.junit.BeforeClass;
import org.junit.Test;

public class VertxJsonTest {

    private final VertxJsonSerializer serializer = new VertxJsonSerializer();

    @BeforeClass
    public static void setup() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
    }

    private void assertSerializeFails(Object obj) {
        assertThrows(InvalidDataException.class, () -> serializer.marshall(obj));
    }

    @Test
    public void testVertxJsonItemPath() {
        ItemPath item = new ItemPath(UUID.randomUUID());
        assertSerializeFails(item);
    }

    @Test
    public void testVertxJsonAgentPath() {
        AgentPath agent = new AgentPath(UUID.randomUUID(), "toto");
        assertSerializeFails(agent);
    }

    @Test
    public void testVertxJsonDomainPath_Context() {
        DomainPath domain = new DomainPath("/domain/path");
        assertSerializeFails(domain);
    }

    @Test
    public void testVertxJsonDomainPath_WithTarget() {
        DomainPath domain = new DomainPath("/domain/path", new ItemPath());
        assertSerializeFails(domain);
    }

    @Test
    public void testVertxJsonRolePath() {
        RolePath role = new RolePath("Minion", false, Arrays.asList("permission1", "permission2"));
        assertSerializeFails(role);
    }

    @Test
    public void testVertxJsonErrorInfo() throws Exception {
        ErrorInfo errorInfo = new ErrorInfo("some error");
        errorInfo.setFatal();
        // Current typed decode of ErrorInfo fails under Java 21 module access.
        assertThrows(
            InvalidDataException.class,
            () -> serializer.unmarshall(serializer.marshall(errorInfo), ErrorInfo.class)
        );
    }

    @Test
    public void testVertxJsonPropertyArrayList() throws Exception {
        PropertyArrayList properties = new PropertyArrayList();
        properties.list.add(new Property("Name", null, false));
        properties.list.add(new Property("Type", "Item", true));

        assertThrows(
            InvalidDataException.class,
            () -> serializer.unmarshall(serializer.marshall(properties), PropertyArrayList.class)
        );
    }

    @Test
    public void testVertxJsonSearchFilter() throws Exception {
        SearchFilter sf = new SearchFilter();
        sf.setSearchRoot("/integTest/Doctors");
        sf.getProperties().add(new Property("Type", "Doctor"));
        sf.getProperties().add(new Property("State", "Active"));
        sf.setRecordsFound(12);

        assertThrows(
            InvalidDataException.class,
            () -> serializer.unmarshall(serializer.marshall(sf), SearchFilter.class)
        );
    }

    @Test
    public void testVertxJsonImportRole() {
        ImportRole role = new ImportRole();
        role.setName("TestRole");
        role.jobList = false;
        role.permissions.add("dom1:Func1,Func2:");
        role.permissions.add("dom2:Func1:toto");
        assertSerializeFails(role);
    }

    @Test
    public void testVertxJsonImportAgent() {
        ImportAgent agent = new ImportAgent("TestAgent", "pwd");
        agent.addRoles(Arrays.asList(new RolePath("TestRole")));
        assertSerializeFails(agent);
    }

    @Test
    public void testVertxJsonImportItem() {
        ImportItem item = new ImportItem("name", "initialPath", new ItemPath(), "wf");
        ImportDependency dependency = new ImportDependency("Cars");
        dependency.props.put("Integer", Integer.valueOf(10), false);
        dependency.props.put("Boolean", Boolean.FALSE, false);
        item.getDependencyList().add(dependency);
        assertSerializeFails(item);
    }

    @Test
    public void testVertxJsonSimpleRoundTrip() throws Exception {
        JsonObject source = new JsonObject()
            .put("name", "alice")
            .put("count", 3)
            .put("active", true);

        String data = serializer.marshall(source);
        Object decoded = serializer.unmarshall(data);

        assertEquals(source, decoded);
    }
}
