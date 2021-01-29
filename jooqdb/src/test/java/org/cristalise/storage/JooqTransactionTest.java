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
package org.cristalise.storage;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.cristalise.JooqTestConfigurationBase;
import org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.StandardServer;
import org.cristalise.storage.jooqdb.JooqDataSourceHandler;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class JooqTransactionTest extends JooqTestConfigurationBase {

    TransactionKey transactionKey = new TransactionKey("JooqTransactionTest");
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
            TransactionKey transactionKey = new TransactionKey("JooqTransactionTest");

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
