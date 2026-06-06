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
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.utils.FileStringUtility;

import lombok.extern.slf4j.Slf4j;

/**
 * Common implementation for file-based cluster storages.
 */
@Slf4j
public abstract class FileBasedClusterStorage extends ClusterStorage {
    protected String rootDir = null;
    protected String fileExtension;
    protected boolean useDirectories = true;

    protected FileBasedClusterStorage(String root, String ext, Boolean useDir, String defaultFileExtension) {
        fileExtension = defaultFileExtension;
        rootDir = new File(root).getAbsolutePath();

        if (ext != null) fileExtension = ext;
        if (useDir != null) useDirectories = useDir;
    }

    @Override
    public void open() throws PersistencyException {
        if (!FileStringUtility.checkDir(rootDir)) {
            log.error("open() - Path {}' does not exist. Attempting to create.", rootDir);
            boolean success = FileStringUtility.createNewDir(rootDir);

            if (!success) {
                throw new PersistencyException("Could not create dir " + rootDir + ". Cannot continue.");
            }
        }

        log.info("open() - DONE rootDir:'{}' ext:'{}' userDir:{}", rootDir, fileExtension, useDirectories);
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

    @Override
    public short queryClusterSupport(ClusterType clusterType) {
        return ClusterStorage.READWRITE;
    }

    @Override
    public boolean checkQuerySupport(String language) {
        log.warn("{} DOES NOT Support any query", getClass().getSimpleName());
        return false;
    }

    @Override
    public String executeQuery(Query query, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("UNIMPLEMENTED function");
    }

    @Override
    public C2KLocalObject get(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        try {
            ClusterType type = ClusterStorage.getClusterType(path);
            String filePath = getFilePath(itemPath, path + fileExtension);

            File dataFile = new File(filePath);
            if (!dataFile.exists() && returnNullForMissingClusterFile()) return null;

            String objString = FileStringUtility.file2String(filePath);
            if (objString.isEmpty()) return null;

            log.trace("get() - objString:{}", objString);

            if (type == ClusterType.OUTCOME) return new Outcome(path, objString);
            return deserializeObject(objString);
        }
        catch (Exception e) {
            log.error("get() - The path {} from {} does not exist", path, itemPath, e);
            throw new PersistencyException(e);
        }
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        try {
            String filePath = getFilePath(itemPath, getPath(obj) + fileExtension);
            log.trace("put() - Writing {}", filePath);

            String data = serializeForStorage(obj);
            String dir = filePath.substring(0, filePath.lastIndexOf('/'));

            if (!FileStringUtility.checkDir(dir)) {
                boolean success = FileStringUtility.createNewDir(dir);
                if (!success) {
                    throw new PersistencyException("Could not create dir " + dir + ". Cannot continue.");
                }
            }
            FileStringUtility.string2File(filePath, data);
        }
        catch (Exception e) {
            log.error("", e);
            throw new PersistencyException("Could not write " + getPath(obj) + " to " + itemPath, e);
        }
    }

    @Override
    public void delete(ItemPath itemPath, ClusterType cluster, TransactionKey transactionKey) throws PersistencyException {
        delete(itemPath, cluster.getName(), transactionKey);
    }

    public void delete(ItemPath itemPath, TransactionKey transactionKey) throws PersistencyException {
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
            throw new PersistencyException("itemPath:" + itemPath + " Could not get contents of " + path + " from "
                    + itemPath + ": " + e.getMessage());
        }
    }

    protected boolean returnNullForMissingClusterFile() {
        return false;
    }

    protected String serializeForStorage(C2KLocalObject obj) throws Exception {
        if (obj instanceof Outcome) return serializeOutcome((Outcome) obj);
        return serializeObject(obj);
    }

    protected String serializeOutcome(Outcome outcome) throws Exception {
        return serializeObject(outcome);
    }

    protected abstract String serializeObject(C2KLocalObject obj) throws Exception;

    protected abstract C2KLocalObject deserializeObject(String serializedData) throws Exception;

    private void removeCluster(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        String[] children = getClusterContents(itemPath, path, transactionKey);

        for (String element : children) {
            removeCluster(itemPath, path + (!path.isEmpty() ? "/" : "") + element, transactionKey);
        }

        if (children.length == 0 && path.indexOf('/') > -1) {
            delete(itemPath, path, transactionKey);
        }
    }

    private String[] getContentsFromFileNames(ItemPath itemPath, String path) throws IOException {
        TreeSet<String> result = new TreeSet<>();

        String resource = getResourceName(path);
        String[] resourceArray = !resource.isEmpty() ? resource.split("\\.") : new String[0];

        try (Stream<Path> pathes = Files.list(Paths.get(rootDir + "/" + itemPath.getUUID()))) {
            pathes.filter(p -> {
                    if (resourceArray.length == 0) {
                        return true;
                    }
                    else {
                        String fileName = p.getFileName().toString();
                        String[] fileNameArray = fileName.split("\\.");
                        String[] fileNameSubArray = fileNameArray.length >= resourceArray.length ? 
                                Arrays.copyOfRange(fileNameArray, 0, resourceArray.length) :
                                new String[0];
                        return Arrays.equals(resourceArray, fileNameSubArray);
                    }
                })
                .forEach(p -> {
                    String fileName = p.getFileName().toString();
                    String content = fileName;
                    if (!resource.isEmpty()) {
                        if (fileName.startsWith(resource + ".")) {
                            content = fileName.substring(resource.length() + 1);
                        } else {
                            // Should not happen due to filter, but to be safe
                            return;
                        }
                    }

                    log.trace("getContentsFromFileNames() - resource:'{}' fileName:'{}' content:'{}'", resource, fileName, content);

                    content = trimKnownExtension(content);

                    int i = content.indexOf('.');
                    if (i != -1) content = content.substring(0, i);
                    if (!content.isEmpty()) result.add(content);
                });
        }
        return result.toArray(new String[0]);
    }

    private String[] getContentsFromDirectories(ItemPath itemPath, String path) {
        String[] result = new String[0];

        String filePath = getFilePath(itemPath, path);
        ArrayList<String> paths = FileStringUtility.listDir(filePath, true, false);
        if (paths == null) return result;

        TreeSet<String> contents = new TreeSet<String>();
        for (String next : paths) {
            next = trimKnownExtension(next);

            if (next.indexOf('/') > -1) next = next.substring(next.lastIndexOf('/') + 1);
            contents.add(next);
        }

        result = contents.toArray(result);

        return result;
    }

    private String trimKnownExtension(String fileName) {
        if (fileName.endsWith(fileExtension)) return fileName.substring(0, fileName.length() - fileExtension.length());
        return fileName;
    }

    protected String getFilePath(ItemPath itemPath, String path) {
        path = getResourceName(path);

        String filePath = rootDir + "/" + itemPath.getUUID() + "/" + path;
        log.trace("getFilePath() - " + filePath);

        return filePath;
    }

    protected String getResourceName(String path) {
        if (!path.isEmpty() && path.charAt(0) == '/') path = path.substring(1);

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
        // no-op
    }

    @Override
    public void commit(TransactionKey transactionKey) throws PersistencyException {
        // no-op
    }

    @Override
    public void abort(TransactionKey transactionKey) throws PersistencyException {
        // no-op
    }
}
