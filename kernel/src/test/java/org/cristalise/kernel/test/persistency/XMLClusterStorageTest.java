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

import static org.cristalise.kernel.persistency.ClusterType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Properties;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.storage.XMLClusterStorage;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class XMLClusterStorageTest {
    static ItemPath itemPath;

    @BeforeClass
    public static void beforeClass() throws Exception {

        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        Gateway.init(props);
        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    public void checkXMLClusterStorage(XMLClusterStorage importCluster) throws Exception {
        ClusterType[] types = importCluster.getClusters(itemPath, null);

        assertEquals(7, types.length);

        for (ClusterType type : types) {
            String[] contents = importCluster.getClusterContents(itemPath, type, null);

            switch (type) {
                case PATH:
                    assertEquals(2,  contents.length);
                    assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder("Domain", "Item"));
                    assertNotNull( importCluster.get(itemPath, PATH+"/Item", null) );
                    assertNotNull( importCluster.get(itemPath, PATH+"/Domain/Batches2016FG160707C-08", null) );
                    break;

                case PROPERTY:
                    assertEquals(19, contents.length);
                    assertNotNull( importCluster.get(itemPath, PROPERTY+"/Name", null) );
                    break;

                case LIFECYCLE:
                    assertEquals(1,  contents.length);
                    assertNotNull( importCluster.get(itemPath, LIFECYCLE+"/workflow", null) );
                    break;

                case OUTCOME:
                    assertEquals(14, contents.length);
                    assertNotNull( importCluster.get(itemPath, OUTCOME+"/PredefinedStepOutcome/0/7", null) );
                    break;

                case VIEWPOINT:
                    assertEquals(14, contents.length);
                    assertNotNull( importCluster.get(itemPath, VIEWPOINT+"/NextStepData/last", null) );
                    break;

                case HISTORY:
                    assertEquals(30, contents.length);
                    assertNotNull( importCluster.get(itemPath, HISTORY+"/0", null) );
                    assertNotNull( importCluster.get(itemPath, HISTORY+"/29", null) );
                    assertEquals(29, importCluster.getLastIntegerId(itemPath, HISTORY.getName(), null));
                    break;

                case JOB:
                    assertEquals(2, contents.length);
                    checkJobs(importCluster);
                    break;

                default:
                    fail("Unhandled ClusterType:"+type);
            }
        }
    }

    private void checkJobs(ClusterStorage importCluster) throws PersistencyException {
        assertNotNull( importCluster.get(itemPath, JOB+"/TestStep/Done", null) );
        Job aJob = (Job)importCluster.get(itemPath, JOB+"/TestStep/Done", null);
        assertEquals("TestStep", aJob.getStepName());
        assertEquals("Done", aJob.getTransitionName());
        assertNotNull(aJob.getTransition());
        assertEquals(aJob.getTransitionName(), aJob.getTransition().getName());

        assertNotNull( importCluster.get(itemPath, JOB+"/TestStep/Start", null) );
        aJob = (Job)importCluster.get(itemPath, JOB+"/TestStep/Start", null);
        assertEquals("TestStep", aJob.getStepName());
        assertEquals("Start", aJob.getTransitionName());
        assertNotNull(aJob.getTransition());
        assertEquals(aJob.getTransitionName(), aJob.getTransition().getName());

        assertNotNull( importCluster.get(itemPath, JOB+"/TestStep2/Start", null) );
        aJob = (Job)importCluster.get(itemPath, JOB+"/TestStep2/Start", null);
        assertEquals("TestStep2", aJob.getStepName());
        assertEquals("Start", aJob.getTransitionName());
        assertNotNull(aJob.getTransition());
        assertEquals(aJob.getTransitionName(), aJob.getTransition().getName());
    }

    @Test
    public void checkFileBasedStorage() throws Exception {
        checkXMLClusterStorage(new XMLClusterStorage("src/test/data/xmlstorage/filebased", "", false));
    }

    @Test
    public void checkDirectoryBasedStorage() throws Exception {
        checkXMLClusterStorage(new XMLClusterStorage("src/test/data/xmlstorage/directorybased"));
    }
}
