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

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.Logger;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.ErrorCodes;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;


public class XMLDBClusterStorage extends ClusterStorage {

	public static final String XMLDB_URI = "XMLDB.URI";
	public static final String XMLDB_USER = "XMLDB.user";
	public static final String XMLDB_PASSWORD = "XMLDB.password";
	public static final String XMLDB_ROOT = "XMLDB.root";
	protected Database database;
	protected Collection root;
	
	public XMLDBClusterStorage() throws Exception {

	}

	protected static Collection verifyCollection(Collection parent, String name, boolean create) throws PersistencyException {
		Collection coll;
		try {
			coll = parent.getChildCollection(name);
			if (coll == null)
				throw new XMLDBException(ErrorCodes.NO_SUCH_COLLECTION);
		} catch (XMLDBException ex) {
			if (ex.errorCode == ErrorCodes.NO_SUCH_COLLECTION) {
				if (create) {
					try {
						CollectionManagementService collManager = (CollectionManagementService)parent.getService("CollectionManagementService", "1.0");
						coll = collManager.createCollection(name);
					} catch (Exception ex2) {
						throw new PersistencyException("Could not create XMLDB collection for item "+name);
					}
				}
				else // not found
					return null;
			}
			else {
				Logger.error(ex);
				throw new PersistencyException("Error loading XMLDB collection for item "+name);
			}
		}
		return coll;
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#open()
	 */
	@Override
	public void open(Authenticator auth) throws PersistencyException {
		
		final String driver = "org.exist.xmldb.DatabaseImpl";
		// Uncomment the following for integrated existdb
		//System.setProperty("exist.initdb", "true");
		//System.setProperty("exist.home", Gateway.getProperty("XMLDB.home"));
		try {
			Class<?> cl = Class.forName(driver);
			database = (Database) cl.newInstance();
			database.setProperty("create-database", "true");
			DatabaseManager.registerDatabase(database);
			Collection db = DatabaseManager.getCollection(Gateway.getProperties().getString(XMLDB_URI), 
					Gateway.getProperties().getString(XMLDB_USER), Gateway.getProperties().getString(XMLDB_PASSWORD));
			String rootColl = Gateway.getProperties().getString(XMLDB_ROOT);
			if (rootColl != null && rootColl.length()>0) {
				root = verifyCollection(db, rootColl, true);
				db.close();
			}
			else
				root = db;
			
		} catch (Exception ex) {
			Logger.error(ex);
			throw new PersistencyException("Error initializing XMLDB");
		}
		
		if (root == null)
			throw new PersistencyException("Root collection is null. Problem connecting to XMLDB.");	
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#close()
	 */
	@Override
	public void close() throws PersistencyException {
		try {
			root.close();
			//DatabaseInstanceManager manager = (DatabaseInstanceManager)db.getService("DatabaseInstanceManager", "1.0");
			//manager.shutdown();
		} catch (XMLDBException e) {
			Logger.error(e);
			throw new PersistencyException("Error shutting down eXist XMLDB");
		}


	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#queryClusterSupport(java.lang.String)
	 */
	@Override
	public short queryClusterSupport(String clusterType) {
		return READWRITE;
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#getName()
	 */
	@Override
	public String getName() {
		return "XMLDB";
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#getId()
	 */
	@Override
	public String getId() {
		return "XMLDB";
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#get(java.lang.Integer, java.lang.String)
	 */
	@Override
	public C2KLocalObject get(ItemPath itemPath, String path)
			throws PersistencyException {
		String type = ClusterStorage.getClusterType(path);
		// Get item collection
		String subPath = itemPath.getUUID().toString();
		Collection itemColl = verifyCollection(root, subPath, false);
		if (itemColl == null) return null; // doesn't exist
		
		try {
			String resourceName = path.replace('/', '.');
			Resource resource = itemColl.getResource(resourceName);
			if (resource != null) {
				String objString = (String)resource.getContent();
				itemColl.close();
				if (type.equals(OUTCOME))
					return new Outcome(path, objString);
				else {
					C2KLocalObject obj = (C2KLocalObject)Gateway.getMarshaller().unmarshall(objString);
					return obj;
				}
			}
			else
				return null;
		} catch (Exception e) {
			Logger.error(e);
			throw new PersistencyException("XMLDB error");
		}
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#put(java.lang.Integer, org.cristalise.kernel.entity.C2KLocalObject)
	 */
	@Override
	public void put(ItemPath itemPath, C2KLocalObject obj)
			throws PersistencyException {
		
		String resName = getPath(obj);
		String subPath = itemPath.getUUID().toString();
		Collection itemColl = verifyCollection(root, subPath, true);
		
		try {
			resName = resName.replace('/', '.');
			String objString = Gateway.getMarshaller().marshall(obj);
			Resource res = itemColl.getResource(resName);
			if (res == null)
				res = itemColl.createResource(resName, "XMLResource");
			res.setContent(objString);
			itemColl.storeResource(res);
			itemColl.close();
		} catch (Exception e) {
			Logger.error(e);
			throw new PersistencyException("XMLDB error");
		}
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#delete(java.lang.Integer, java.lang.String)
	 */
	@Override
	public void delete(ItemPath itemPath, String path)
			throws PersistencyException {
		String subPath = itemPath.getUUID().toString();
		Collection itemColl = verifyCollection(root, subPath, false);
		if (itemColl == null) return;

		
		try {
			String resource = path.replace('/', '.');
			Resource res = itemColl.getResource(resource);
			if (res != null) itemColl.removeResource(res);
			itemColl.close(); itemColl.close();
        } catch (Exception e) {
            Logger.error(e);
            throw new PersistencyException("XMLClusterStorage.delete() - Could not delete "+path+" to "+itemPath);
        }
	}

	/* (non-Javadoc)
	 * @see org.cristalise.kernel.persistency.ClusterStorage#getClusterContents(java.lang.Integer, java.lang.String)
	 */
	@Override
	public String[] getClusterContents(ItemPath itemPath, String path)
			throws PersistencyException {
		String subPath = itemPath.getUUID().toString();
		Collection coll = verifyCollection(root, subPath, false);
		if (coll == null) return new String[0];
		ArrayList<String> contents = new ArrayList<String>();
		
		// Find prefix for our path level
		StringBuffer resPrefix = new StringBuffer();
		String[] pathComps = path.split("/");
		if (pathComps.length > 0)
			for (int i = 0; i < pathComps.length; i++) {
				if (pathComps[i].length()>0) resPrefix.append(pathComps[i]).append(".");
			}
		// Look at each entry for matches. Trim off the ends.
		try {
			for (String res: coll.listResources()) {
				if (res.startsWith(resPrefix.toString())) {
					String resName = URLDecoder.decode(res.substring(resPrefix.length()), "UTF-8");
					if (resName.indexOf('.')>-1)
						resName = resName.substring(0, resName.indexOf('.'));
					if (!contents.contains(resName)) contents.add(resName);
				}
			}
		} catch (XMLDBException e) {
			Logger.error(e);
			throw new PersistencyException("Error listing collection resources for item "+itemPath);
		} catch (UnsupportedEncodingException e) {
			Logger.error(e);
			throw new PersistencyException("Error listing decoding resource name for item "+itemPath);
		}
		return contents.toArray(new String[contents.size()]);
	}

}
