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

import java.io.File;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.process.MainTest;
import org.cristalise.kernel.utils.FileStringUtility;
import org.cristalise.storage.JSONClusterStorage;
import org.cristalise.storage.XMLClusterStorage;
import org.junit.Test;

@Slf4j
public class StorageFixtureMigrator {

    @Test
    public void migrateFixtures() throws Exception {
        Properties props = FileStringUtility.loadConfigFile(MainTest.class.getResource("/server.conf").getPath());
        props.put("ClusterStorage", "org.cristalise.storage.jooqdb.JooqClusterStorage");
        props.put("Lookup",         "org.cristalise.storage.jooqdb.lookup.JooqLookupManager");
        props.put(AbstractMain.MAIN_ARG_SKIPBOOTSTRAP, true);
        props.put("Gateway.clusteredVertx", false);

        Gateway.init(props);
        ItemPath itemPath = new ItemPath("fcecd4ad-40eb-421c-a648-edc1d74f339b");

        String root = new File("src/test/data").getAbsolutePath();
        log.info("Data root: {}", root);
        migrate(root + "/xmlstorage/filebased", root + "/jsonstorage/filebased", false, itemPath);
        migrate(root + "/xmlstorage/directorybased", root + "/jsonstorage/directorybased", true, itemPath);
    }

    private void migrate(String sourceRoot, String targetRoot, boolean useDir, ItemPath itemPath) throws Exception {


        XMLClusterStorage source = new XMLClusterStorage(sourceRoot, "", useDir);
        JSONClusterStorage target = new JSONClusterStorage(targetRoot, ".json", useDir);

        ClusterType[] types = source.getClusters(itemPath, null);

        for (ClusterType type : types) {
            log.info("Migrating cluster type:{}", type);
            String[] contents = source.getClusterContents(itemPath, type.getName(), null);
            for (String content : contents) {
                String path = type + "/" + content;
                log.info("Migrating cluster path:{}", path);
                Object obj = source.get(itemPath, path, null);
                if (obj instanceof org.cristalise.kernel.entity.C2KLocalObject) {
                    target.put(itemPath, (org.cristalise.kernel.entity.C2KLocalObject) obj, null);
                }
            }
        }
    }
}
