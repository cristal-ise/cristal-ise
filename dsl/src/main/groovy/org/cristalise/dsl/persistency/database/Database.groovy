/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.dsl.persistency.database

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.utils.FileStringUtility

class Database {

    private final String scriptDir = "DB"
    private final String createScriptData
    private final String insertScriptData
    private final String selectScriptData
    private final String updateScriptData
    private final String deleteScriptData
    private final String itemScriptsData

    private String name
    private Integer version
    private ItemPath itemPath

    /**
     *
     * @param name
     * @param version
     * @param itemPath
     * @param createScriptData
     * @param insertScriptData
     * @param selectScriptData
     * @param updateScriptData
     * @param deleteScriptData
     * @param itemScriptsData
     */
    Database(String name, int version, ItemPath itemPath, String createScriptData, String insertScriptData,
             String selectScriptData, String updateScriptData, String deleteScriptData, String itemScriptsData) {
        super()
        this.name = name
        this.version = version
        this.itemPath = itemPath
        this.createScriptData = createScriptData
        this.insertScriptData = insertScriptData
        this.selectScriptData = selectScriptData
        this.updateScriptData = updateScriptData
        this.deleteScriptData = deleteScriptData
        this.itemScriptsData = itemScriptsData
    }

    /**
     *
     * @param name
     * @param version
     * @param createScriptData
     * @param insertScriptData
     * @param selectScriptData
     * @param updateScriptData
     * @param deleteScriptData
     * @param itemScriptsData
     */
    Database(String name, int version, String createScriptData, String insertScriptData, String selectScriptData,
             String updateScriptData, String deleteScriptData, String itemScriptsData) {
        super()
        this.name = name
        this.version = version
        this.createScriptData = createScriptData
        this.insertScriptData = insertScriptData
        this.selectScriptData = selectScriptData
        this.updateScriptData = updateScriptData
        this.deleteScriptData = deleteScriptData
        this.itemScriptsData = itemScriptsData
        this.itemPath = null
    }

    /**
     * Sets database name to 'Datbase' and version to 0
     *
     * @param createScriptData
     * @param insertScriptData
     * @param selectScriptData
     * @param updateScriptData
     * @param deleteScriptData
     * @param itemScriptsData
     */
    Database(String createScriptData, String insertScriptData, String selectScriptData, String updateScriptData,
             String deleteScriptData, String itemScriptsData) {
        this.createScriptData = createScriptData
        this.insertScriptData = insertScriptData
        this.selectScriptData = selectScriptData
        this.updateScriptData = updateScriptData
        this.deleteScriptData = deleteScriptData
        this.itemScriptsData = itemScriptsData
        name = "Database"
        version = 0
    }

    /**
     *
     * @return
     */
    String getName(){
        return this.name
    }

    /**
     *
     * @param imports
     * @param dir
     * @param shallow
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     * @throws IOException
     */
    void export(File dir) throws InvalidDataException, ObjectNotFoundException, IOException {

        String createFileName = getName() + DatabaseType.CREATE.getValue() + ".groovy"
        String insertFileName = getName() + DatabaseType.INSERT.getValue() + ".groovy"
        String selectFileName = getName() + DatabaseType.SELECT.getValue() + ".groovy"
        String updateFileName = getName() + DatabaseType.UPDATE.getValue() + ".groovy"
        String deleteFileName = getName() + DatabaseType.DELETE.getValue() + ".groovy"
        String scriptFileName = getName() + DatabaseType.SCRIPT.getValue() + ".groovy"

        FileStringUtility.string2File(new File(new File(dir, scriptDir), createFileName), createScriptData)
        FileStringUtility.string2File(new File(new File(dir, scriptDir), insertFileName), insertScriptData)
        FileStringUtility.string2File(new File(new File(dir, scriptDir), selectFileName), selectScriptData)
        FileStringUtility.string2File(new File(new File(dir, scriptDir), updateFileName), updateScriptData)
        FileStringUtility.string2File(new File(new File(dir, scriptDir), deleteFileName), deleteScriptData)
        FileStringUtility.string2File(new File(new File(dir, scriptDir), scriptFileName), itemScriptsData)
    }

}
