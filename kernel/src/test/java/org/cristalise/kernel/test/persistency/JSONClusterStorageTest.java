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

import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PATH;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.storage.JSONClusterStorage;
import org.cristalise.storage.XMLClusterStorage;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Slf4j
@Ignore
public class JSONClusterStorageTest {
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

    @Test
    public void checkDirectoryBasedStorage() throws Exception {
        XMLClusterStorage source = new XMLClusterStorage("src/test/data/xmlstorage/directorybased");
        Path root = Files.createTempDirectory("json-storage-dir");
        JSONClusterStorage target = new JSONClusterStorage(root.toString());

        migrateStorage(source, target);
        checkJsonClusterStorage(target);
    }

    @Test
    public void checkFileBasedStorage() throws Exception {
        XMLClusterStorage source = new XMLClusterStorage("src/test/data/xmlstorage/filebased", "", false);
        Path root = Files.createTempDirectory("json-storage-file");
        JSONClusterStorage target = new JSONClusterStorage(root.toString(), "", false);

        migrateStorage(source, target);
        checkJsonClusterStorage(target);
    }

    private void migrateStorage(ClusterStorage source, ClusterStorage target) throws Exception {
        for (ClusterType clusterType : source.getClusters(itemPath, null)) {
            switch (clusterType) {
                case PATH:
                    migratePathCluster(source, target);
                    break;
                case OUTCOME:
                case VIEWPOINT:
                    migrateDepth3Cluster(source, target, clusterType);
                    break;
                case JOB:
                    migrateDepth2Cluster(source, target, clusterType);
                    break;
                default:
                    migrateFlatCluster(source, target, clusterType);
                    break;
            }
        }
    }

    private void migrateFlatCluster(ClusterStorage source, ClusterStorage target, ClusterType clusterName) throws Exception {
        for (String name : source.getClusterContents(itemPath, clusterName, null)) {
            target.put(itemPath, source.get(itemPath, clusterName + "/" + name, null), null);
        }
    }

    private void migratePathCluster(ClusterStorage source, ClusterStorage target) throws Exception {
        String clusterName = PATH.getName();
        for (String pathType : source.getClusterContents(itemPath, clusterName, null)) {
            String pathPrefix = clusterName + "/" + pathType;
            
            log.info("Migrating path:{}", pathPrefix);

            if ("Item".equals(pathType)) {
                target.put(itemPath, source.get(itemPath, pathPrefix, null), null);
            }
            else {
                for (String name : source.getClusterContents(itemPath, pathPrefix, null)) {
                    target.put(itemPath, source.get(itemPath, pathPrefix + "/" + name, null), null);
                }
            }
        }
    }

    private void migrateDepth2Cluster(ClusterStorage source, ClusterStorage target, ClusterType clusterType) throws Exception {
        for (String level1 : source.getClusterContents(itemPath, clusterType, null)) {
            String level1Path = clusterType + "/" + level1;
            for (String level2 : source.getClusterContents(itemPath, level1Path, null)) {
                log.info("Migrating level 2 path:{}", level1Path + "/" + level2);
                target.put(itemPath, source.get(itemPath, level1Path + "/" + level2, null), null);
            }
        }
    }

    private void migrateDepth3Cluster(ClusterStorage source, ClusterStorage target, ClusterType clusterType) throws Exception {
        for (String level1 : source.getClusterContents(itemPath, clusterType, null)) {
            String level1Path = clusterType + "/" + level1;
            for (String level2 : source.getClusterContents(itemPath, level1Path, null)) {
                String level2Path = level1Path + "/" + level2;
                for (String level3 : source.getClusterContents(itemPath, level2Path, null)) {
                    log.info("Migrating level 3 path:{}", level2Path + "/" + level3);
                    target.put(itemPath, source.get(itemPath, level2Path + "/" + level3, null), null);
                }
            }
        }
    }

    public void checkJsonClusterStorage(JSONClusterStorage storage) throws Exception {
        ClusterType[] types = storage.getClusters(itemPath, null);

        assertEquals(7, types.length);

        for (ClusterType type : types) {
            String[] contents = storage.getClusterContents(itemPath, type, null);

            switch (type) {
                case PATH:
                    assertEquals(2, contents.length);
                    assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder("Domain", "Item"));
                    assertNotNull(storage.get(itemPath, PATH + "/Item", null));
                    assertNotNull(storage.get(itemPath, PATH + "/Domain/Batches2016FG160707C-08", null));
                    break;

                case PROPERTY:
                    assertEquals(19, contents.length);
                    assertNotNull(storage.get(itemPath, PROPERTY + "/Name", null));
                    break;

                case LIFECYCLE:
                    assertEquals(1, contents.length);
                    assertNotNull(storage.get(itemPath, LIFECYCLE + "/workflow", null));
                    break;

                case OUTCOME:
                    assertEquals(14, contents.length);
                    assertNotNull(storage.get(itemPath, OUTCOME + "/PredefinedStepOutcome/0/7", null));
                    break;

                case VIEWPOINT:
                    assertEquals(14, contents.length);
                    assertNotNull(storage.get(itemPath, VIEWPOINT + "/NextStepData/last", null));
                    break;

                case HISTORY:
                    assertEquals(30, contents.length);
                    assertNotNull(storage.get(itemPath, HISTORY + "/0", null));
                    assertNotNull(storage.get(itemPath, HISTORY + "/29", null));
                    assertEquals(29, storage.getLastIntegerId(itemPath, HISTORY.getName(), null));
                    break;

                case JOB:
                    assertEquals(2, contents.length);
                    checkJobs(storage);
                    break;

                default:
                    fail("Unhandled ClusterType:" + type);
            }
        }
    }

    private void checkJobs(ClusterStorage importCluster) throws PersistencyException {
        Job aJob = (Job) importCluster.get(itemPath, JOB + "/TestStep/Done", null);
        assertNotNull(aJob);
        assertEquals("TestStep", aJob.getStepName());
        assertEquals("Done", aJob.getTransitionName());
        assertEquals("Admin", aJob.getRoleOverride());
        assertEquals("da8c7b53-f0ab-4532-9773-64233c536415", aJob.getAgentUUID());
        assertNotNull(aJob.getTransition());
        assertEquals(aJob.getTransitionName(), aJob.getTransition().getName());

        assertNotNull(importCluster.get(itemPath, JOB + "/TestStep/Start", null));
        aJob = (Job) importCluster.get(itemPath, JOB + "/TestStep/Start", null);
        assertEquals("TestStep", aJob.getStepName());
        assertEquals("Start", aJob.getTransitionName());
        assertNull(aJob.getRoleOverride());
        assertEquals("da8c7b53-f0ab-4532-9773-64233c536415", aJob.getAgentUUID());
        assertNotNull(aJob.getTransition());
        assertEquals(aJob.getTransitionName(), aJob.getTransition().getName());

        assertNotNull(importCluster.get(itemPath, JOB + "/TestStep2/Start", null));
        aJob = (Job) importCluster.get(itemPath, JOB + "/TestStep2/Start", null);
        assertEquals("TestStep2", aJob.getStepName());
        assertEquals("Start", aJob.getTransitionName());
        assertNull(aJob.getRoleOverride());
        assertNull(aJob.getAgentUUID());
        assertNotNull(aJob.getTransition());
        assertEquals(aJob.getTransitionName(), aJob.getTransition().getName());
    }
}
