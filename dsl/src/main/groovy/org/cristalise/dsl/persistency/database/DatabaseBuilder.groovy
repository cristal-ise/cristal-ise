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
package org.cristalise.dsl.persistency.database

import groovy.transform.CompileStatic
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.utils.Logger

/**
 *
 */
@CompileStatic
class DatabaseBuilder {
    String module = ""
    String name = ""
    int version = -1

    Database database = null

    public DatabaseBuilder() {}

    /**
     *
     * @param name
     * @param version
     */
    public DatabaseBuilder(String name, int version) {
        this.name = name
        this.version = version
    }

    /**
     *
     * @param module
     * @param name
     * @param version
     */
    public DatabaseBuilder(String module, String name, int version) {
        this.module = module
        this.name = name
        this.version = version
    }

    /**
     *
     * @param dbCreateFile
     * @param dbInsertFile
     * @param dbSelectFile
     * @param dbUpdateFile
     * @return
     */
    public DatabaseBuilder loadDB(String dbCreateFile, String dbInsertFile, String dbSelectFile, 
		String dbSelectAllFile, String dbUpdateFile, String dbDeleteFile, String dbScriptFile) {
		
        Logger.msg 5, "DatabaseBuilder.loadDB() - From file:$dbCreateFile"
        Logger.msg 5, "DatabaseBuilder.loadDB() - From file:$dbInsertFile"
        Logger.msg 5, "DatabaseBuilder.loadDB() - From file:$dbSelectFile"
        Logger.msg 5, "DatabaseBuilder.loadDB() - From file:$dbUpdateFile"
        Logger.msg 5, "DatabaseBuilder.loadDB() - From file:$dbDeleteFile"
        Logger.msg 5, "DatabaseBuilder.loadDB() - From file:$dbScriptFile"

        database = new Database(name, version, new File(dbCreateFile).text, new File(dbInsertFile).text,
                new File(dbSelectFile).text, new File(dbSelectFile).text,  new File(dbUpdateFile).text, 
				new File(dbDeleteFile).text, new File(dbScriptFile).text)
        return this
    }

    /**
     *
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public static Database build(String name, int version, Closure cl) {
        def db = new DatabaseBuilder(name, version)
        generateDatabase(db, cl)
        return db.database
    }

    /**
     *
     * @param module
     * @param name
     * @param version
     * @param cl
     * @return
     */
    public static DatabaseBuilder build(String module, String name, int version, Closure cl) {
        def db = new DatabaseBuilder(module, name, version)
        generateDatabase(db, cl)
        return db
    }

    private static void generateDatabase(DatabaseBuilder db, Closure cl) {
        def dbDelegate = new DatabaseDelegate()
        try {
            dbDelegate.processClosure(db.name, cl)
            Logger.msg 5, "DatabaseBuilder - generated database create:\n" + dbDelegate.dbCreateString
            Logger.msg 5, "DatabaseBuilder - generated database insert:\n" + dbDelegate.dbInsertString
            Logger.msg 5, "DatabaseBuilder - generated database select:\n" + dbDelegate.dbSelectString
            Logger.msg 5, "DatabaseBuilder - generated database update:\n" + dbDelegate.dbUpdateString
            Logger.msg 5, "DatabaseBuilder - generated database delete:\n" + dbDelegate.dbDeleteString

            db.database = new Database(db.name, db.version, dbDelegate.dbCreateString, dbDelegate.dbInsertString,
                    dbDelegate.dbSelectString, dbDelegate.dbSelectAllString, dbDelegate.dbUpdateString, dbDelegate.dbDeleteString, dbDelegate.dbScriptsString)
        } catch (Exception e) {
            throw new InvalidDataException(e.getMessage())
        }

    }

    /**
     *
     * @param module
     * @param name
     * @param version
     * @param dbCreateFile
     * @param dbInsertFile
     * @param dbSelectFile
     * @param dbUpdateFile
     * @return
     */
    public static DatabaseBuilder build(String module, String name, int version, String dbCreateFile, String dbInsertFile,
                                        String dbSelectFile, String dbSelectAllFile, String dbUpdateFile, String dbDeleteFile, String dbScriptFile) {
        def sb = new DatabaseBuilder(module, name, version)
        return sb.loadDB(dbCreateFile, dbInsertFile, dbSelectFile, dbSelectAllFile, dbUpdateFile, dbDeleteFile, dbScriptFile)
    }
}
