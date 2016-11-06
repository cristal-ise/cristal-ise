/**
 * This file is part of the CRISTAL-iSE MySQL Cluster Storage Module.
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
package org.cristalise.storage.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionalClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.Logger;

/**
 *
 */
public class MySQLClusterStorage extends TransactionalClusterStorage {
    public static final String MYSQLDB_URI      = "MYSQLDB.URI";
    public static final String MYSQLDB_USER     = "MYSQLDB.user";
    public static final String MYSQLDB_PASSWORD = "MYSQLDB.password";

    Connection conn = null;

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        try {
            String uri  = Gateway.getProperties().getString(MYSQLDB_URI); //jdbc:mysql://localhost/test
            String user = Gateway.getProperties().getString(MYSQLDB_USER); 
            String pwd  = Gateway.getProperties().getString(MYSQLDB_PASSWORD);
            
            conn = DriverManager.getConnection(uri+"?user="+user+"&password="+pwd);
        }
        catch (SQLException ex) {
            Logger.error("SQLState: "    + ex.getSQLState());
            Logger.error("VendorError: " + ex.getErrorCode());
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    @Override
    public void close() throws PersistencyException {
        try {
            conn.abort(null);
            conn.close();
        }
        catch (SQLException e) {
            Logger.error(e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public short queryClusterSupport(String clusterType) {
        if(OUTCOME.equals(clusterType)) return WRITE;
        else                            return NONE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        return false;
        //return "mysql:sql".equals(language.trim().toLowerCase());
    }

    @Override
    public String getName() {
        return "MySQL ClusterStorage";
    }

    @Override
    public String getId() {
        return "MySQLDB";
    }

    @Override
    public String executeQuery(Query query) throws PersistencyException {
        throw new PersistencyException("UnImplemented");
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path) throws PersistencyException {
        throw new PersistencyException("Read is not supported in MySQL");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path) throws PersistencyException {
        throw new PersistencyException("Read is not supported in MySQL");
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj) throws PersistencyException {
        if (obj instanceof Outcome) {
            Outcome o = (Outcome) obj;
        }
        else
            throw new PersistencyException("MySql implementation can only store Outcome");
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object locker) throws PersistencyException {
        put(itemPath, obj);
    }

    @Override
    public void delete(ItemPath itemPath, String path) throws PersistencyException {
        throw new PersistencyException("Delete is not supported in MySQL");
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object locker) throws PersistencyException {
        throw new PersistencyException("Delete is not supported in MySQL");
    }

    @Override
    public void begin(Object locker) {
        //TODO: Implement
    }

    @Override
    public void commit(Object locker) throws PersistencyException {
        //TODO: Implement
    }

    @Override
    public void abort(Object locker) {
        //TODO: Implement
    }
}
