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
package org.cristalise.kernel.test.utils;

import static org.cristalise.kernel.SystemProperties.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.cristalise.kernel.process.Gateway;
import org.junit.jupiter.api.BeforeAll;

public class SystemPropertiesTest {
    
    @BeforeAll
    public static void beforeAll() throws Exception {
        Properties systemProps = new Properties();

        systemProps.put("Trigger.permissions", "Patient:*:kovax,Doctor:Update:*");
        systemProps.put("Module.dsl.reset", true);
        systemProps.put("Module.dev.reset", "yes");

        systemProps.put("UserCode.StateMachine.version", 10);
        systemProps.put("Actor.StateMachine.version", 11);

        systemProps.put(ClusterStorage.toString(), "org.cristalise.storage.jooqdb.JooqClientReader");

        Gateway.init(systemProps);

        Activity_validateOutcome.set(true);
        ItemServer_Telnet_port.set(1234);
    }

    @Test
    void getString() throws Exception {
        assertNull(Lookup.getString());
        assertEquals("10", ItemVerticle_requestTimeoutSeconds.getString());
        assertEquals("true", LocalChangeVerticle_publishLocalMessage.getString());

        assertEquals("LDAP", Lookup.getString("LDAP"));
        assertEquals("Shiro", Authenticator.getString());
        assertEquals("org.cristalise.storage.jooqdb.JooqClientReader", ClusterStorage.getString());
        assertEquals("org.cristalise.storage.jooqdb.JooqClientReader", ClusterStorage.getString("XMLDB"));
    }

    @Test
    void getStringByFormat() throws Exception {
        assertNull(                                     $UserCodeRole_permissions.getString(null, "Actor"));
        assertNull(                                     $UserCodeRole_permissions.getString((Object)"Actor"));
        assertNull(                                     $UserCodeRole_permissions.getString(new Object[] {"Actor"}));
        assertEquals("*",                               $UserCodeRole_permissions.getString("*", "Actor"));
        assertEquals("Patient:*:kovax,Doctor:Update:*", $UserCodeRole_permissions.getString(null, "Trigger"));
    }

    @Test
    void getBoolean() throws Exception {
        assertNull(Lookup.getBoolean());
        assertNull(Lifecycle_Sign_passwordField.getBoolean());

        assertTrue(Activity_validateOutcome.getBoolean());
        assertTrue(Activity_validateOutcome.getBoolean(false));
        assertFalse(StateMachine_enableErrorHandling.getBoolean());
        assertTrue(StateMachine_enableErrorHandling.getBoolean(true));
    }

    @Test
    void getBooleanByFormat() throws Exception {
        assertNull (Module_$Namespace_reset.getBoolean("kernel"));
        assertFalse(Module_$Namespace_reset.getBoolean(Module_reset.getBoolean(), "kernel"));
        assertTrue (Module_$Namespace_reset.getBoolean("dsl"));
        assertTrue (Module_$Namespace_reset.getBoolean(false, "dsl"));
        assertTrue (Module_$Namespace_reset.getBoolean("dev"));
    }

    @Test
    void getInteger() throws Exception {
        assertNull(Lookup.getInteger());
        assertNull(Lifecycle_Sign_passwordField.getInteger());

        assertEquals(8, ItemVerticle_instances.getInteger());
        assertEquals(64, ItemVerticle_instances.getInteger(64));
        assertEquals(1234, ItemServer_Telnet_port.getInteger());
        assertEquals(1234, ItemServer_Telnet_port.getInteger(123456));
    }

    @Test
    void getIntegerByFormat() throws Exception {
        assertNull($UserCodeRole_StateMachine_version.getInteger("Trigger"));
        assertEquals(10, $UserCodeRole_StateMachine_version.getInteger("UserCode"));
        assertEquals(10, $UserCodeRole_StateMachine_version.getInteger(20, "UserCode"));
        assertEquals(11, $UserCodeRole_StateMachine_version.getInteger("Actor"));
    }
}
