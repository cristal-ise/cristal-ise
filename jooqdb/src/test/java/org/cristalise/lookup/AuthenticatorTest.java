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
package org.cristalise.lookup;

import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.ObjectProperties;
import org.cristalise.storage.jooqdb.JooqDataSourceHandler;
import org.cristalise.storage.jooqdb.auth.Argon2PasswordService;
import org.cristalise.storage.jooqdb.auth.JooqAuthenticator;
import org.cristalise.storage.jooqdb.lookup.JooqLookupManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AuthenticatorTest {
    
    JooqLookupManager lookup;
    JooqAuthenticator auth;

    @Before
    public void setUp() throws Exception {
        ObjectProperties c2kProps = new ObjectProperties();

        c2kProps.put(JooqDataSourceHandler.JOOQ_URI,      "jdbc:h2:mem:");
        c2kProps.put(JooqDataSourceHandler.JOOQ_USER,     "sa");
        c2kProps.put(JooqDataSourceHandler.JOOQ_PASSWORD, "sa");
        c2kProps.put(JooqDataSourceHandler.JOOQ_DIALECT,  "H2");

        Gateway.init(c2kProps);

        auth = new JooqAuthenticator();
        lookup  = new JooqLookupManager();

        lookup.open(null);
    }

    @After
    public void tearDown() {
        auth.disconnect();
        lookup.close();
    }

    @Test
    public void authentcateUser() throws Exception {
        JooqAuthenticator auth = new JooqAuthenticator();

        AgentPath agent = new AgentPath(new ItemPath(), "dummyUser");
        lookup.add(agent);
        lookup.setAgentPassword(agent, "123456");

        assert auth.authenticate("dummyUser", "123456", null);
    }

    @Test @Ignore("Not Implemented")
    public void authentcateUserShiro() throws Exception {
        AgentPath agent = new AgentPath(new ItemPath(), "dummyUser");
        lookup.add(agent);
        lookup.setAgentPassword(agent, "123456");

        Argon2PasswordService shiroPwdService = new Argon2PasswordService();
        assert shiroPwdService.passwordsMatch("dummyUser", "123456");
    }
}
