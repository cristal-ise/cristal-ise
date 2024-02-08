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
package org.cristalise.storage;

import static org.cristalise.kernel.SystemProperties.XMLStorage_root;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.FileStringUtility;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of ClusterStorage providing the XML file based persistence.
 */
@Slf4j
public class XMLClusterStorage extends ClusterStorage {
    String  rootDir        = null;
    String  fileExtension  = ".xml";
    boolean useDirectories = true;

    public XMLClusterStorage() {}

    /**
     * Create new XMLClusterStorage with specific setup, Used in predefined step 
     * {@link org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport}
     * 
     * @param root specify the root directory
     */
    public XMLClusterStorage(String root) {
        this(root, null, null);
    }

    /**
     * Create new XMLClusterStorage with specific setup, Used in predefined step 
     * {@link org.cristalise.kernel.lifecycle.instance.predefined.server.BulkImport}
     * 
     * @param root specify the root directory
     * @param ext the extension of the files with dot, e.g. '.xml', used to save the cluster content.
     *        If it is null the default '.xml' extension is used.
     * @param useDir specify if the files should be stored in directories or in single files, e.g. Property.Type,xml
     *        If it is null the default is true.
     */
    public XMLClusterStorage(String root, String ext, Boolean useDir) {
        rootDir = new File(root).getAbsolutePath();

        if (ext    != null) fileExtension  = ext;
        if (useDir != null) useDirectories = useDir;
    }

    @Override
    public void open(Authenticator auth) throws PersistencyException {
        if (StringUtils.isBlank(rootDir)) {
            String rootProp = XMLStorage_root.getString();

            if (rootProp == null)
                throw new PersistencyException("Root path not given in config file.");

            rootDir = new File(rootProp).getAbsolutePath();
        }

        if (!FileStringUtility.checkDir(rootDir)) {
            log.error("open() - Path " + rootDir + "' does not exist. Attempting to create.");
            boolean success = FileStringUtility.createNewDir(rootDir);

            if (!success)
                throw new PersistencyException("Could not create dir " + rootDir + ". Cannot continue.");
        }
        
        log.info("open() - DONE rootDir:'" + rootDir + "' ext:'" + fileExtension + "' userDir:" + useDirectories);
    }

    @Override
    public void close() {
        rootDir = null;
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
        return ClusterStorage.READWRITE;
    }

    @Override
    public String getName() {
        return "XML File Cluster Storage";
    }

    @Override
    public String getId() {
        return "XML";
    }

    @Override
    public boolean checkQuerySupport(String language) {
        log.warn("XMLClusterStorage DOES NOT Support any query");
        return false;
    }

    @Override
    public String executeQuery(Query query, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED function");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        try {
            ClusterType type      = ClusterStorage.getClusterType(path);
            String      filePath  = getFilePath(itemPath, path) + fileExtension;
            String      objString = FileStringUtility.file2String(filePath);

            if (objString.length() == 0) return null;

            log.trace("get() - objString:" + objString);

            if (type == ClusterType.OUTCOME) return new Outcome(path, objString);
            else                             return (C2KLocalObject) Gateway.getMarshaller().unmarshall(objString);
        }
        catch (Exception e) {
            log.error("get() - The path " + path + " from " + itemPath + " does not exist", e);
            throw new PersistencyException(e);
        }
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, getPath(obj) + fileExtension);
            log.trace("put() - Writing " + filePath);
            String data = Gateway.getMarshaller().marshall(obj);

            String dir = filePath.substring(0, filePath.lastIndexOf('/'));

            if (!FileStringUtility.checkDir(dir)) {
                boolean success = FileStringUtility.createNewDir(dir);
                if (!success)
                    throw new PersistencyException("Could not create dir " + dir + ". Cannot continue.");
            }
            FileStringUtility.string2File(filePath, data);
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("Could not write " + getPath(obj) + " to " + itemPath, e);
        }
    }

    private void removeCluster(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        String[] children = getClusterContents(itemPath, path, transactionKey);

        for (String element : children) {
            removeCluster(itemPath, path+(path.length()>0?"/":"")+element, transactionKey);
        }

        if (children.length == 0 && path.indexOf("/") > -1) {
            delete(itemPath, path, transactionKey);
        }
    }

    @Override
    public void delete(ItemPath itemPath, ClusterType cluster, TransactionKey transactionKey) throws PersistencyException {
        delete(itemPath, cluster.getName(), transactionKey);
    }

    public void delete(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException {
        // TODO: use delete full directory when useDirectories = true
        removeCluster(itemPath, "", transactionKey);
    }

    @Override
    public void delete(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, path + fileExtension);
            boolean success = FileStringUtility.deleteDir(filePath, true, true);
            if (success) return;

            filePath = getFilePath(itemPath, path);
            success = FileStringUtility.deleteDir(filePath, true, true);
            if (success) return;
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("Failure deleting path " + path + " in " + itemPath + " Error: " + e.getMessage());
        }

        throw new PersistencyException("delete() - Failure deleting path " + path + " in " + itemPath);
    }

    @Override
    public String[] getClusterContents(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        try {
            if (useDirectories) return getContentsFromDirectories(itemPath, path);
            else                return getContentsFromFileNames(itemPath, path);
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("itemPath:"+itemPath+" Could not get contents of " + path + " from "
                    + itemPath + ": " + e.getMessage());
        }
    }

    private String[] getContentsFromFileNames(ItemPath itemPath, String path) throws IOException {
        TreeSet<String> result = new TreeSet<>();

        String resource = getResourceName(path);
        String[] resourceArray = resource.length() > 0 ? resource.split("\\.") : new String[0];

        try (Stream<Path> pathes = Files.list(Paths.get(rootDir + "/" + itemPath.getUUID()))) {
            pathes.filter(p -> {
                    if (resourceArray.length == 0) {
                        return true;
                    }
                    else {
                        String fileName = p.getFileName().toString();
                        String[] fileNameArray = fileName.split("\\.");
                        String[] fileNameSubArray = Arrays.copyOfRange(fileNameArray, 0, resourceArray.length);
                        return Arrays.equals(resourceArray, fileNameSubArray);
                    }
                })
                .forEach(p -> {
                    String fileName = p.getFileName().toString();
                    String content = resource.length() != 0 ? fileName.substring(resource.length()+1) : fileName.substring(resource.length());

                    log.trace("getContentsFromFileNames() - resource:'"+resource+"' fileName:'"+fileName+"' content:'"+content+"'");

                    if (content.endsWith(fileExtension)) content = content.substring(0, content.length() - fileExtension.length());

                    int i = content.indexOf('.');
                    if (i != -1) content = content.substring(0, i);

                    result.add(content);
                });
        }
        return result.toArray(new String[0]);
    }

    private String[] getContentsFromDirectories(ItemPath itemPath, String path) {
        String[] result = new String[0];

        String filePath = getFilePath(itemPath, path);
        ArrayList<String> paths = FileStringUtility.listDir(filePath, true, false);
        if (paths == null) return result; // dir doesn't exist yet

        ArrayList<String> contents = new ArrayList<String>();
        String previous = null;
        for (int i = 0; i < paths.size(); i++) {
            String next = paths.get(i);

            // trim off the extension (e.g '.xml') from the end if it's there
            if (next.endsWith(fileExtension)) next = next.substring(0, next.length() - fileExtension.length());

            // avoid duplicates (xml and dir)
            if (next.equals(previous)) continue;
            previous = next;

            // only keep the last bit of the path
            if (next.indexOf('/') > -1) next = next.substring(next.lastIndexOf('/') + 1);
            contents.add(next);
        }

        result = contents.toArray(result);

        return result;
    }

    protected String getFilePath(ItemPath itemPath, String path)  {
        path = getResourceName(path);

        String filePath = rootDir + "/" + itemPath.getUUID() + "/" + path;
        log.trace("getFilePath() - " + filePath);

        return filePath;
    }

    protected String getResourceName(String path) {
        //remove leading '/' if exists
        if (path.length() != 0 && path.charAt(0) == '/') path = path.substring(1);

        if (!useDirectories) path = path.replace("/", ".");

        return path;
    }

    @Override
    public int getLastIntegerId(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
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
