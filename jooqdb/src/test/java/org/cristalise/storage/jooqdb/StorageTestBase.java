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
import java.util.UUID;

import org.cristalise.JooqTestConfigurationBase;
import org.cristalise.kernel.common.GTimeStamp;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.jooq.DSLContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

public class StorageTestBase extends JooqTestConfigurationBase {
    DSLContext context;
    UUID       uuid = UUID.randomUUID();

    @BeforeClass
    public static void beforeClass() throws Exception {
        Logger.addLogStream(System.out, 8);
        Gateway.init(new Properties());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    public static void compareTimestramps(GTimeStamp actual, GTimeStamp expected) {
        expected.mHour = expected.mHour - expected.mTimeOffset / 3600000;

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
