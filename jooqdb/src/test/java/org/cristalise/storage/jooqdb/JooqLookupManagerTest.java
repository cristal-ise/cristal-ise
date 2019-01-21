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

import java.util.Properties;

import org.cristalise.JooqTestBase;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.storage.jooqdb.lookup.JooqLookupManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JooqLookupManagerTest extends JooqTestBase {

    JooqLookupManager jooq;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = new Properties();
        
        setUpStorage(props);

        Gateway.init(props);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    @Before
    public void before() throws Exception {
        jooq = new JooqLookupManager();
        jooq.open(null);
    }

    @Test
    public void openConnectionAndCreateTables() {
    }
}
