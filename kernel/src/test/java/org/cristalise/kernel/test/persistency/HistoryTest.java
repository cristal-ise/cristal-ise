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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.cristalise.kernel.SystemProperties;
import org.cristalise.kernel.events.Event;
import org.cristalise.kernel.events.History;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HistoryTest {

    static ItemPath itemPath;
    static String uuid = "fcecd4ad-40eb-421c-a648-edc1d74f339b";
    static String storageDir;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
        FieldUtils.writeDeclaredStaticField(Gateway.class, "mStorage", new ClusterStorageManager(null), true);
        itemPath = new ItemPath(uuid);
        storageDir = SystemProperties.XMLStorage_root.getString() + "/" + uuid;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    @Test
    public void checkHistory() throws Exception {
        History history = new History(itemPath, null);

        assertEquals(29, history.getLastId());

        assertTrue(history.containsKey("0"));
        assertTrue(history.containsKey(0));
        assertFalse(history.containsKey("30"));
        assertFalse(history.containsKey(30));

        String event0String = FileStringUtility.file2String(storageDir+"/AuditTrail/0.xml");
        Event event0 = (Event)Gateway.getMarshaller().unmarshall(event0String);
        String event29String = FileStringUtility.file2String(storageDir+"/AuditTrail/29.xml");
        Event event29 = (Event)Gateway.getMarshaller().unmarshall(event29String);

        assertThat(event0).isEqualToComparingFieldByFieldRecursively(history.getEvent(0));
        assertThat(event0).isEqualToComparingFieldByFieldRecursively(history.get(0));
        assertThat(event0).isEqualToComparingFieldByFieldRecursively(history.get("0"));
        assertThat(event0.getTimeStamp().mNano).isEqualTo(123000000);

        assertThat(event29).isEqualToComparingFieldByFieldRecursively(history.getEvent(29));
        assertThat(event29).isEqualToComparingFieldByFieldRecursively(history.get(29));
        assertThat(event29).isEqualToComparingFieldByFieldRecursively(history.get("29"));
        assertThat(event29.getTimeStamp().mNano).isEqualTo(0);
    }

}
