package org.cristalise.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.cristalise.JooqTestConfigurationBase;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.StandardServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JooqTransactionTest extends JooqTestConfigurationBase {

    String transactionKey = "JooqTransactionTest-"+System.currentTimeMillis();

    @BeforeClass
    public static void beforeClass() throws Exception {
        dbType = DBModes.PostgreSQL;

        Properties props = new Properties();

        props.put("ClusterStorage", "org.cristalise.storage.jooqdb.JooqClusterStorage");
        props.put("Lookup",         "org.cristalise.storage.jooqdb.lookup.JooqLookupManager");
        props.put(AbstractMain.MAIN_ARG_SKIPBOOTSTRAP, true);

        setUpStorage(props);

        StandardServer.standardInitialisation(props, null);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        
    }

    @Test
    public void transCommit() throws Exception {
        ItemPath itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");

        Gateway.getStorage().begin(transactionKey);

        assertFalse(itemPath.exists(transactionKey));

        BulkImport importer = new BulkImport("src/test/data");
        importer.initialise();
        importer.importAllClusters(transactionKey);

        Gateway.getLookupManager().add(itemPath, transactionKey);

        assertTrue(itemPath.exists(transactionKey));
        assertFalse(itemPath.exists());

        Gateway.getStorage().commit(transactionKey);

        assertTrue(itemPath.exists());
    }
}
