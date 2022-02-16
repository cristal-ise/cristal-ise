/**
 * This file is part of the CRISTAL-iSE LDAP lookup plugin.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.lookup.ldap;

import java.util.ArrayList;
import java.util.StringTokenizer;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.querying.Query;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LDAPClusterStorage extends ClusterStorage {
    LDAPPropertyManager ldapStore;

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        Lookup lookup = Gateway.getLookup();
        if (lookup instanceof LDAPLookup)
            ldapStore = ((LDAPLookup) lookup).getPropManager();
        else
            throw new PersistencyException("Cannot use LDAP cluster storage without LDAP Lookup");

    }

    @Override
    public void close() throws PersistencyException {
    }

    @Override
    public void postBoostrap() {
        //nothing to be done
    }

    @Override
    public void postStartServer() {
        //nothing to be done
    }

    @Override
    public void postConnect() {
        //nothing to be done
    }

    // introspection
    @Override
    public short queryClusterSupport(ClusterType clusterType) {
        if (clusterType.equals(ClusterType.PROPERTY.getName()))
            return READWRITE;
        else
            return NONE;
    }

    @Override
    public String getName() {
        return "LDAP Cluster Storage";
    }

    @Override
    public String getId() {
        return "LDAP";
    }

    // retrieve object by path
    @Override
    public C2KLocalObject get(ItemPath thisItem, String path, TransactionKey transactionKey) throws PersistencyException {
        StringTokenizer tok = new StringTokenizer(path, "/");
        int pathLength = tok.countTokens();
        if (pathLength != 2) throw new PersistencyException("Path length was invalid: " + path);
        ClusterType type = ClusterType.getValue(tok.nextToken());

        String objName = tok.nextToken();
        C2KLocalObject newObj;

        if (type == ClusterType.PROPERTY) {
            try {
                Property newProperty = ldapStore.getProperty(thisItem, objName);
                newObj = newProperty;
            }
            catch (ObjectNotFoundException ex) {
                throw new PersistencyException("Property " + objName + " not found in " + thisItem);
            }

        }
        else
            throw new PersistencyException("Cluster type " + type + " not supported.");

        return newObj;
    }

    // store object by path
    @Override
    public void put(ItemPath thisItem, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        ClusterType type = obj.getClusterType();

        if (type == ClusterType.PROPERTY) {
            try {
                ldapStore.setProperty(thisItem, (Property) obj);
            }
            catch (Exception e1) {
                log.error("",e1);
                throw new PersistencyException("LDAPClusterStorage - could not write property");
            }
        }
        else
            throw new PersistencyException("Cluster type " + type + " not supported.");
    }

    // delete full cluster
    @Override
    public void delete(ItemPath thisItem, ClusterType cluster, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnctionality");
    }

    // delete cluster
    @Override
    public void delete(ItemPath thisItem, String path, TransactionKey transactionKey) throws PersistencyException {
        StringTokenizer tok = new StringTokenizer(path, "/");
        int pathLength = tok.countTokens();
        if (pathLength != 2) throw new PersistencyException("Path length was invalid: " + path);
        ClusterType type = ClusterType.getValue(tok.nextToken());

        if (type == ClusterType.PROPERTY) {
            try {
                ldapStore.deleteProperty(thisItem, tok.nextToken());
            }
            catch (Exception e1) {
                log.error("",e1);
                throw new PersistencyException("LDAPClusterStorage - could not delete property");
            }
        }
        else
            throw new PersistencyException("Cluster type " + type + " not supported.");

    }

    /* navigation */

    // directory listing
    @Override
    public String[] getClusterContents(ItemPath thisItem, String path, TransactionKey transactionKey) throws PersistencyException {
        StringTokenizer tok = new StringTokenizer(path, "/");
        int pathLength = tok.countTokens();
        if (pathLength > 1) return new String[0];

        ClusterType type = getClusterType(path);

        try {
            if (type == ClusterType.PROPERTY) {
                return ldapStore.getPropertyNames(thisItem);
            }
            else if (type.equals("")) { // root query
                String[] allClusters = new String[0];
                ArrayList<String> clusterList = new ArrayList<String>();
                if (ldapStore.hasProperties(thisItem)) clusterList.add(ClusterType.class.getName());
                allClusters = clusterList.toArray(allClusters);
                return allClusters;
            }
            else
                throw new PersistencyException("Cluster type " + type + " not supported.");
        }
        catch (ObjectNotFoundException e) {
            throw new PersistencyException("Item " + thisItem + " does not exist");
        }
    }

    @Override
    public boolean checkQuerySupport(String language) {
        return false;
    }

    @Override
    public String executeQuery(Query query, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnctionality");
    }

    @Override
    public int getLastIntegerId(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED funnctionality");
    }

    @Override
    public void begin(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void commit(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void abort(TransactionKey transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }
}
