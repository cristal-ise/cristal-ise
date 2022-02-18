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
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import org.cristalise.JooqTestConfigurationBase;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.ElementSelectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqClusterStorageTest extends JooqTestConfigurationBase {
    static String uuid = "fcecd4ad-40eb-421c-a648-edc1d74f339b";
    static ItemPath itemPath;

    public static boolean compareXML(String expected, String actual)  {
        Diff diffIdentical = DiffBuilder.compare(expected).withTest(actual)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForSimilar()
                .build();

        if(diffIdentical.hasDifferences()) {
            log.info("actual:{}", actual);
            log.warn(diffIdentical.toString());
        }

        return !diffIdentical.hasDifferences();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties props = new Properties();

        props.put("ClusterStorage", "org.cristalise.storage.jooqdb.JooqClusterStorage");
        props.put("BulkImport.rootDirectory", "src/test/data");
        props.put("Gateway.clusteredVertx", false);

        setUpStorage(props);

        Gateway.init(props);
        Gateway.connect();

        itemPath = new ItemPath(uuid);
    }

    @Before
    public void importItem() throws Exception {
        BulkImport importer = new BulkImport();
        importer.initialise();
        importer.importAllClusters(null);
    }

    @After
    public void deleteItem() throws Exception {
        //NOTE: implicitly tests deleting full item data
        Gateway.getStorage().removeCluster(itemPath, null);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Gateway.close();
    }

    @Test
    public void clusterTypesTest() throws Exception {
        String[] types = Gateway.getStorage().getClusterContents(itemPath, "");

        assertThat(Arrays.asList(types), IsIterableContainingInAnyOrder.containsInAnyOrder(
                "Property", "LifeCycle", "Outcome", "ViewPoint", "AuditTrail"));
    }

    @Test
    public void propertyClusterTest() throws Exception {
        String[] contents = Gateway.getStorage().getClusterContents(itemPath, PROPERTY);

        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder(
                "BatchResult", "Creator", "Molecule", "Name", "ProductionDate", "ProductionLine", "ProductionSite", "Status", "Type"));

        assertNotNull( Gateway.getStorage().get(itemPath, PROPERTY+"/Name", null) );
        assertNotNull( Gateway.getStorage().get(itemPath, PROPERTY+"/Molecule", null) );
    }

    @Test
    public void lifeCycleClusterTest() throws Exception {
        String[] contents = Gateway.getStorage().getClusterContents(itemPath, LIFECYCLE);

        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder("workflow"));
        assertNotNull( Gateway.getStorage().get(itemPath, LIFECYCLE+"/workflow", null) );
    }

    @Test
    public void outcomeClusterTest() throws Exception {
        String[] contents = Gateway.getStorage().getClusterContents(itemPath, OUTCOME);

        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder("PredefinedStepOutcome"));

        contents = Gateway.getStorage().getClusterContents(itemPath, OUTCOME+"/PredefinedStepOutcome/0");
        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));

        assertNotNull( Gateway.getStorage().get(itemPath, OUTCOME+"/PredefinedStepOutcome/0/0", null) );
    }

    @Test
    public void viewPointClusterTest() throws Exception {
        String[] contents = Gateway.getStorage().getClusterContents(itemPath, VIEWPOINT);

        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder(
                "Dispensing2Data", "DispensingData", "CommercialDataData"));

        contents = Gateway.getStorage().getClusterContents(itemPath, VIEWPOINT+"/Dispensing2Data");
        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder("last"));

        contents = Gateway.getStorage().getClusterContents(itemPath, VIEWPOINT+"/DispensingData");
        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder("old", "last"));

        assertNotNull( Gateway.getStorage().get(itemPath, VIEWPOINT+"/CommercialDataData/last", null) );
    }

    @Test
    public void historyClusterTest() throws Exception {
        String[] contents = Gateway.getStorage().getClusterContents(itemPath, HISTORY);

        assertThat(Arrays.asList(contents), IsIterableContainingInAnyOrder.containsInAnyOrder(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));

        assertNotNull( Gateway.getStorage().get(itemPath, HISTORY+"/0", null) );
        assertNotNull( Gateway.getStorage().get(itemPath, HISTORY+"/9", null) );
    }

    @Test
    public void queryHistoryTest() throws Exception {
        String queryXml          = new String(Files.readAllBytes(Paths.get("src/test/data/TestQueryHistory.xml")));
        String expectedResultXml = new String(Files.readAllBytes(Paths.get("src/test/data/TestQueryHistoryResult.xml")));

        Query q = new Query(queryXml);
        q.setStringParameter("schemaName", "PredefinedStepOutcome");
        q.setStringParameter("itemUUID", uuid);

        String resultXml = Gateway.getStorage().executeQuery(q);
        assertTrue(compareXML(expectedResultXml, resultXml));
    }

    @Test
    public void queryEventTest() throws Exception {
        String queryXml          = new String(Files.readAllBytes(Paths.get("src/test/data/TestQueryEvent.xml")));
        String expectedResultXml = new String(Files.readAllBytes(Paths.get("src/test/data/TestQueryEventResult.xml")));

        Query q = new Query(queryXml);
        q.setStringParameter("eventId", 0);
        q.setStringParameter("itemUUID", uuid);

        String resultXml = Gateway.getStorage().executeQuery(q);
        assertTrue(compareXML(expectedResultXml, resultXml));
    }

    @Test
    public void queryOutcomeTest() throws Exception {
        String queryXml          = new String(Files.readAllBytes(Paths.get("src/test/data/TestQueryOutcome.xml")));
        String expectedResultXml = new String(Files.readAllBytes(Paths.get("src/test/data/"+uuid+"/Outcome.PredefinedStepOutcome.0.0")));

        Query q = new Query(queryXml);
        q.setStringParameter("itemUUID", uuid);
        q.setStringParameter("schemaName", "PredefinedStepOutcome");
        q.setStringParameter("schemaVersion", 0);
        q.setStringParameter("eventId", 0);

        String resultXml = Gateway.getStorage().executeQuery(q);
        assertTrue(compareXML(expectedResultXml, resultXml));
    }
}
