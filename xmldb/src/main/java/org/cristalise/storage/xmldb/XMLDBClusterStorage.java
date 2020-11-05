/**
 * This file is part of the CRISTAL-iSE eXist-DB storage plugin.
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
package org.cristalise.storage.xmldb;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import javax.xml.transform.OutputKeys;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Parameter;
import org.cristalise.kernel.querying.Query;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.XQueryService;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.CompiledExpression;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 */
@Slf4j
public class XMLDBClusterStorage extends ClusterStorage {

    public static final String XMLDB_URI      = "XMLDB.URI";
    public static final String XMLDB_USER     = "XMLDB.user";
    public static final String XMLDB_PASSWORD = "XMLDB.password";
    public static final String XMLDB_ROOT     = "XMLDB.root";

    protected Database    database;
    protected Collection  root;

    public XMLDBClusterStorage() {}

    /**
     * Retrieves the collection from the root created for each Item (UUID)
     * 
     * @param itemPath the Path representing the Item
     * @param create whether to create the missing collection or not
     * @return returns the actual Collection or null if the collection was not found or created
     * @throws PersistencyException
     */
    protected Collection getItemCollection(ItemPath itemPath, boolean create) throws PersistencyException {
        return verifyCollection(root, itemPath.getUUID().toString(), create);
    }

    /**
     * Retrieves the collection from the given parent collection
     * 
     * @param parent the parent collection
     * @param name name of the child collection
     * @param create whether to create the missing collection or not
     * @return returns the actual Collection or null if the collection was not found or created
     * @throws PersistencyException
     */
    protected static Collection verifyCollection(Collection parent, String name, boolean create) throws PersistencyException {
        Collection coll;
        try {
            coll = parent.getChildCollection(name);
            if (coll == null) throw new XMLDBException(ErrorCodes.NO_SUCH_COLLECTION);
        }
        catch (XMLDBException ex) {
            if (ex.errorCode == ErrorCodes.NO_SUCH_COLLECTION) {
                if (create) {
                    try {
                        CollectionManagementService collManager = 
                                (CollectionManagementService) parent.getService("CollectionManagementService", "1.0");
                        coll = collManager.createCollection(name);
                    }
                    catch (Exception ex2) {
                        throw new PersistencyException("Could not create XMLDB collection for item " + name);
                    }
                }
                else // not found
                    return null;
            }
            else {
                log.error("Error loading XMLDB collection for item " + name, ex);
                throw new PersistencyException("Error loading XMLDB collection for item " + name);
            }
        }
        return coll;
    }

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        final String driver = "org.exist.xmldb.DatabaseImpl";
        // Uncomment the following for embedded existdb
        // System.setProperty("exist.initdb", "true");
        // System.setProperty("exist.home", Gateway.getProperty("XMLDB.home"));
        try {
            Class<?> cl = Class.forName(driver);
            database = (Database) cl.newInstance();
            database.setProperty("create-database", "true");
            DatabaseManager.registerDatabase(database);

            Collection db = DatabaseManager.getCollection(
                    Gateway.getProperties().getString(XMLDB_URI),
                    Gateway.getProperties().getString(XMLDB_USER),
                    Gateway.getProperties().getString(XMLDB_PASSWORD));

            String rootColl = Gateway.getProperties().getString(XMLDB_ROOT);

            if (rootColl != null && rootColl.length() > 0) {
                root = verifyCollection(db, rootColl, true);
                db.close();
            }
            else root = db;
        }
        catch (Exception ex) {
            log.error("Error initializing eXist XMLDB", ex);
            throw new PersistencyException("Error initializing XMLDB");
        }

        if (root == null) throw new PersistencyException("Root collection is null. Problem connecting to XMLDB.");
    }

    @Override
    public void close() throws PersistencyException {
        try {
            root.close();
            //DatabaseInstanceManager manager = (DatabaseInstanceManager)db.getService("DatabaseInstanceManager","1.0");
            //manager.shutdown();
        }
        catch (XMLDBException e) {
            log.error("Error shutting down eXist XMLDB", e);
            throw new PersistencyException("Error shutting down eXist XMLDB");
        }
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

    @Override
    public short queryClusterSupport(ClusterType clusterType) {
        return READWRITE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        return "existdb:xquery".equals(language.trim().toLowerCase());
    }

    @Override
    public String getName() {
        return "XMLDB";
    }

    @Override
    public String getId() {
        return "XMLDB";
    }

    @Override
    public String executeQuery(Query query, Object transactionKey) throws PersistencyException {
        if(!checkQuerySupport(query.getLanguage())) throw new PersistencyException("Unsupported query:"+query.getLanguage());

        try {
            XQueryService xqs = (XQueryService) root.getService("XQueryService", "1.0");
            xqs.setProperty(OutputKeys.INDENT, "yes");
            xqs.setProperty(OutputKeys.ENCODING, "UTF-8");

            //XML-RPC server automatically caches compiled expressions
            if (query.hasParameters()) {
                for(Parameter p: query.getParameters()) {
                    if (p.getValue() != null) {
                        log.debug("executeQuery() - declareVariable:'"+p.getName()+"' = '"+p.getValue()+"'");
                        xqs.declareVariable(p.getName(), (String)p.getValue());
                    }
                }
            }

            CompiledExpression compiled = xqs.compileAndCheck(query.getQuery());
            ResourceIterator resourceIter = xqs.execute(compiled).getIterator();
            Resource resource = null;
            StringBuffer resultBuffer = new StringBuffer();

            while(resourceIter.hasMoreResources()) {
                try {
                    resource = resourceIter.nextResource();
                    resultBuffer.append(resource.getContent());
                }
                finally {
                    //dont forget to cleanup resources
                    try { ((EXistResource)resource).freeResources(); } 
                    catch(XMLDBException xe) {
                        log.error("", xe);
                    }
                }
            }
            log.debug("executeQuery() - returning:{}", resultBuffer);
            return resultBuffer.toString();
        }
        catch (Exception e) {
            log.error("executeQuery() - {}", query.getQuery(), e);
            throw new PersistencyException(e.getMessage());
        }
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        ClusterType type = ClusterStorage.getClusterType(path);
        // Get item collection
        Collection itemColl = getItemCollection(itemPath, false);
        if (itemColl == null) return null; // doesn't exist

        try {
            String resourceName = path.replace('/', '.');
            Resource resource = itemColl.getResource(resourceName);
            if (resource != null) {
                String objString = (String) resource.getContent();
                itemColl.close();

                if (type == ClusterType.OUTCOME) return new Outcome(path, objString);
                else                             return (C2KLocalObject) Gateway.getMarshaller().unmarshall(objString);
            }
            else return null;
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("XMLDB error");
        }
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, Object transactionKey) throws PersistencyException {
        String resName = getPath(obj);
        Collection itemColl = getItemCollection(itemPath, true);

        try {
            resName = resName.replace('/', '.');
            String objString = Gateway.getMarshaller().marshall(obj);
            Resource res = itemColl.getResource(resName);
            if (res == null) res = itemColl.createResource(resName, "XMLResource");
            res.setContent(objString);
            itemColl.storeResource(res);
            itemColl.close();
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("XMLDB error");
        }
    }

    @Override
    public void delete(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        Collection itemColl = getItemCollection(itemPath, false);
        if (itemColl == null) return;

        try {
            Resource res = itemColl.getResource(path.replace('/', '.'));
            if (res != null) itemColl.removeResource(res);
            itemColl.close();
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("Could not delete " + path + " to " + itemPath);
        }
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        Collection itemCollection = getItemCollection(itemPath, false);
        if (itemCollection == null) return new String[0];

        ArrayList<String> contents = new ArrayList<String>();

        // Find prefix for our path level
        StringBuffer searchPrefix = new StringBuffer();
        String[] pathComps = path.split("/");

        if (pathComps.length > 0) {
            for (int i = 0; i < pathComps.length; i++) {
                if (pathComps[i].length() > 0) searchPrefix.append(pathComps[i]).append(".");
            }
        }

        log.debug("getClusterContents() - path:"+path+" converted to:"+searchPrefix);

        // Look at each entry for matches. Trim off the ends.
        try {
            for (String dbResource : itemCollection.listResources()) {
                log.trace("getClusterContents() - dbResource:"+dbResource);

                if (dbResource.startsWith(searchPrefix.toString())) {
                    //Get string which is after the searchPerfix
                    String contentName = URLDecoder.decode(dbResource.substring(searchPrefix.length()), "UTF-8");

                    log.trace("getClusterContents() - contentName:"+contentName);

                    //Only add the name of the next section (sections are separated by '.')
                    if (contentName.contains(".")) contentName = contentName.substring(0, contentName.indexOf('.'));

                    //Do not add duplicates
                    if (!contents.contains(contentName)) contents.add(contentName);
                }
            }
        }
        catch (XMLDBException e) {
            log.error("", e);
            throw new PersistencyException("Error listing collection resources for item " + itemPath);
        }
        catch (UnsupportedEncodingException e) {
            log.error("", e);
            throw new PersistencyException("Error listing decoding resource name for item " + itemPath);
        }

        return contents.toArray(new String[contents.size()]);
    }

    /**
     * FIXME use xquery instead of calling getClusterContents()
     */
    @Override
    public int getLastIntegerId(ItemPath itemPath, String path, Object transactionKey) throws PersistencyException {
        int lastId = -1;
        try {
            String[] keys = getClusterContents(itemPath, path, transactionKey);
            for (String key : keys) {
                int newId = Integer.parseInt(key);
                lastId = newId > lastId ? newId : lastId;
            }
        }
        catch (NumberFormatException e) {
           log.error("Error parsing keys", e);
           throw new PersistencyException(e.getMessage());
        }

        return lastId;
    }

    @Override
    public void begin(Object transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void commit(Object transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void abort(Object transactionKey) throws PersistencyException {
        // TODO Auto-generated method stub
        
    }
}
