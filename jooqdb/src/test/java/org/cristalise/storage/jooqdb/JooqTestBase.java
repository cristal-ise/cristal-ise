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

import static org.jooq.impl.DSL.using;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.UUID;

import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.process.Gateway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class JooqTestBase {
    DSLContext context;
    UUID       uuid = UUID.randomUUID();

    @BeforeClass
    public static void beforeClass() throws Exception {
        Gateway.init(new Properties());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    public void before() throws Exception {
        String userName = "sa";
        String password = "sa";
        String url      = "jdbc:h2:mem:";

        Connection conn = DriverManager.getConnection(url, userName, password);
        context = using(conn, SQLDialect.H2);
    }
    
    public static void compareTimestramps(GTimeStamp actual, GTimeStamp expected) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.mYear,       actual.mYear);
        Assert.assertEquals(expected.mMonth,      actual.mMonth);
        Assert.assertEquals(expected.mDay,        actual.mDay);
        Assert.assertEquals(expected.mHour,       actual.mHour);
        Assert.assertEquals(expected.mMinute,     actual.mMinute);
        Assert.assertEquals(expected.mSecond,     actual.mSecond);
        //Assert.assertEquals(expected.mTimeOffset, actual.mTimeOffset);
    }
}
