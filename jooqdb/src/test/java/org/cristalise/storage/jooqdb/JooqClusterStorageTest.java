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

import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JooqClusterStorageTest {
    static ItemPath itemPath;

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = new Properties();

        props.put("ClusterStorage", "org.cristalise.storage.jooqdb.JooqClusterStorage");

        props.put(JooqHandler.JOOQ_URI,      "jdbc:h2:mem:");
        props.put(JooqHandler.JOOQ_USER,     "sa");
        props.put(JooqHandler.JOOQ_PASSWORD, "sa");
        props.put(JooqHandler.JOOQ_DIALECT,  "H2");

        props.put("BulkImport.rootDirectory", "src/test/data");

        Gateway.init(props);
        Gateway.connect();

        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");

        BulkImport importer = new BulkImport();
        importer.initialise();

        importer.importProperty (itemPath, null);
        importer.importLifeCycle(itemPath, null);
        importer.importOutcome  (itemPath, null);
        importer.importViewPoint(itemPath, null);
        importer.importHistory  (itemPath, null);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    @Test
    public void checkClusterStorage() throws Exception {
        String[] types = Gateway.getStorage().getClusterContents(itemPath, "");

        assertEquals(5, types.length);

        for (String typeName : types) {
            String[] contents = Gateway.getStorage().getClusterContents(itemPath, typeName);

            switch (ClusterType.getValue(typeName)) {
                case PROPERTY:
                    assertEquals(9, contents.length);
                    assertNotNull( Gateway.getStorage().get(itemPath, PROPERTY+"/Name", null) );
                    assertNotNull( Gateway.getStorage().get(itemPath, PROPERTY+"/Molecule", null) );
                    break;

                case LIFECYCLE:
                    assertEquals(1,  contents.length);
                    assertNotNull( Gateway.getStorage().get(itemPath, LIFECYCLE+"/workflow", null) );
                    break;

                case OUTCOME:
                    assertEquals(1, contents.length);
                    assertEquals(10, Gateway.getStorage().getClusterContents(itemPath, OUTCOME+"/PredefinedStepOutcome/0").length);
                    assertNotNull( Gateway.getStorage().get(itemPath, OUTCOME+"/PredefinedStepOutcome/0/0", null) );
                    break;

                case VIEWPOINT:
                    assertEquals(3, contents.length);
                    assertNotNull( Gateway.getStorage().get(itemPath, VIEWPOINT+"/CommercialDataData/last", null) );
                    break;

                case HISTORY:
                    assertEquals(10, contents.length);
                    assertNotNull( Gateway.getStorage().get(itemPath, HISTORY+"/0", null) );
                    assertNotNull( Gateway.getStorage().get(itemPath, HISTORY+"/9", null) );
                    break;

                default:
                    fail("Test data does not contain ClusterType:"+typeName);
            }
        }
        Logger.msg("-----------------------------------------------------------------------");
        Gateway.getStorage().removeCluster(itemPath, "", null);
    }
}
