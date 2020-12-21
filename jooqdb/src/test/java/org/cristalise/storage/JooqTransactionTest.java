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
import org.cristalise.storage.jooqdb.JooqDataSourceHandler;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class JooqTransactionTest extends JooqTestConfigurationBase {

    String transactionKey = "JooqTransactionTest-"+System.currentTimeMillis();
    static ItemPath itemPath;

    @BeforeClass
    public static void beforeClass() throws Exception {
        itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");

        Properties props = new Properties();

        props.put("ClusterStorage", "org.cristalise.storage.jooqdb.JooqClusterStorage");
        props.put("Lookup",         "org.cristalise.storage.jooqdb.lookup.JooqLookupManager");
        props.put(AbstractMain.MAIN_ARG_SKIPBOOTSTRAP, true);

        setUpStorage(props);
        props.remove(JooqDataSourceHandler.JOOQ_AUTOCOMMIT);

        StandardServer.standardInitialisation(props, null);
    }

    @After
    public void tearDown() throws Exception {
        if (itemPath.exists()) {
            String transactionKey = "JooqTransactionTest-"+System.currentTimeMillis();
            
            Gateway.getStorage().begin(transactionKey);
            Gateway.getStorage().removeCluster(itemPath, "", transactionKey); 
            Gateway.getLookupManager().delete(itemPath, transactionKey);
            Gateway.getStorage().commit(transactionKey);
        }
    }

    @Test
    public void transCommit() throws Exception {
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

    @Test
    public void transAbort() throws Exception {
        Gateway.getStorage().begin(transactionKey);

        assertFalse(itemPath.exists(transactionKey));

        BulkImport importer = new BulkImport("src/test/data");
        importer.initialise();
        importer.importAllClusters(transactionKey);

        Gateway.getLookupManager().add(itemPath, transactionKey);

        assertTrue(itemPath.exists(transactionKey));
        assertFalse(itemPath.exists());

        Gateway.getStorage().abort(transactionKey);

        assertFalse(itemPath.exists());
    }
}
