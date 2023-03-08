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
package org.cristalise.kernel.lifecycle.instance.predefined.server;

import static org.cristalise.kernel.SystemProperties.BulkImport_fileExtension;
import static org.cristalise.kernel.SystemProperties.BulkImport_rootDirectory;
import static org.cristalise.kernel.SystemProperties.BulkImport_useDirectories;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PATH;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.storage.XMLClusterStorage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BulkImport extends PredefinedStep {

    private String  root;
    private String  ext;
    private Boolean  useDir;

    XMLClusterStorage importCluster;

    public BulkImport() {
        super();

        root   = BulkImport_rootDirectory.getString();
        ext    = BulkImport_fileExtension.getString();
        useDir = BulkImport_useDirectories.getBoolean();
    }

    public BulkImport(String rootDir) {
        super();
        root = rootDir;
        ext = "";
        useDir = false;
    }

    public void initialise() throws InvalidDataException {
        if (importCluster == null) {
            if (root == null)
                throw new InvalidDataException("Root path not given in config file.");

            importCluster = new XMLClusterStorage(root, ext, useDir);
        }
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException, ObjectCannotBeUpdated,
                   ObjectNotFoundException, PersistencyException, CannotManageException
    {
        log.debug("Called by {} on {}", agent.getAgentName(transactionKey), itemPath);

        initialise();

        importAllClusters(transactionKey);

        return requestData;
    }

    public void importAllClusters(TransactionKey transactionKey) throws InvalidDataException, PersistencyException {
        for (ItemPath item: getItemsToImport(root)) {
            for (ClusterType type : importCluster.getClusters(item, transactionKey)) {
                switch (type) {
                    case PATH:       importPath(item, transactionKey);       break;
                    case PROPERTY:   importProperty(item, transactionKey);   break;
                    case LIFECYCLE:  importLifeCycle(item, transactionKey);  break;
                    case HISTORY:    importHistory(item, transactionKey);    break;
                    case VIEWPOINT:  importViewPoint(item, transactionKey);  break;
                    case OUTCOME:    importOutcome(item, transactionKey);    break;
                    case COLLECTION: importCollection(item, transactionKey); break;
                    case JOB:        importJob(item, transactionKey);        break;

                    default:
                        break;
                }
            }

            //importCluster.delete(item, "");
        }
    }

    private List<ItemPath> getItemsToImport(String root) throws InvalidDataException {
        List<ItemPath> items = new ArrayList<>();
        try {
            try (Stream<Path> files = Files.walk(Paths.get(root), 1)) {
                files.filter(Files::isDirectory)
                    .forEach(path -> {
                        //skip root directory
                        if (path.equals(Paths.get(root))) return;

                        String uuid = path.getFileName().toString();

                        log.info("getItemsToImport()- directory:{}", uuid);

                        try {
                            items.add(new ItemPath(uuid));
                        }
                        catch (InvalidItemPathException e) {
                            log.warn("getItemsToImport() - Unvalid UUID for import directory:{}", uuid);
                        }
                    });
            }

            return items;
        }
        catch (IOException e) {
            log.error("", e);
            throw new InvalidDataException(e.getMessage());
        }
    }

    public void importProperty(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, PROPERTY, transactionKey);

        for (String c : contents) {
            String path = PROPERTY+"/"+c;
            C2KLocalObject prop = importCluster.get(item, path, transactionKey);
            Gateway.getStorage().put(item, prop, transactionKey);

            //importCluster.delete(item, path);
        }
    }

    public void importViewPoint(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, VIEWPOINT, transactionKey);

        for (String c : contents) {
            String[] subContents = importCluster.getClusterContents(item, VIEWPOINT+"/"+c, transactionKey);

            for (String sc : subContents) {
                String path = VIEWPOINT+"/"+c+"/"+sc;
                C2KLocalObject view = importCluster.get(item, path, transactionKey);
                Gateway.getStorage().put(item, view, transactionKey);

                //importCluster.delete(item, path);
            }
        }
    }

    public void importLifeCycle(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, LIFECYCLE, transactionKey);

        for (String c : contents) {
            String path = LIFECYCLE+"/"+c;
            C2KLocalObject wf = importCluster.get(item, path, transactionKey);
            Gateway.getStorage().put(item, wf, transactionKey);

            //importCluster.delete(item, path);
        }
    }

    public void importHistory(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, HISTORY, transactionKey);

        for (String c : contents) {
            String path = HISTORY+"/"+c;
            C2KLocalObject obj = importCluster.get(item, path, transactionKey);
            Gateway.getStorage().put(item, obj, transactionKey);

            //importCluster.delete(item, path);
        }
    }

    public void importOutcome(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] schemas = importCluster.getClusterContents(item, OUTCOME, transactionKey);

        for (String schema : schemas) {
            String[] versions = importCluster.getClusterContents(item, OUTCOME+"/"+schema, transactionKey);

            for (String version : versions) {
                String[] events = importCluster.getClusterContents(item, OUTCOME+"/"+schema+"/"+version, transactionKey);

                for (String event : events) {
                    C2KLocalObject obj = importCluster.get(item, OUTCOME+"/"+schema+"/"+version+"/"+event, transactionKey);
                    Gateway.getStorage().put(item, obj, transactionKey);

                    //importCluster.delete(item, path.toString());
                }
            }
        }
    }

    public void importJob(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] stepNames = importCluster.getClusterContents(item, JOB, transactionKey);

        for (String step : stepNames) {
            String[] transitions = importCluster.getClusterContents(item, JOB+"/"+step, transactionKey);

            for (String trans : transitions) {
                C2KLocalObject job = importCluster.get(item, JOB+"/"+step+"/"+trans, transactionKey);
                Gateway.getStorage().put(item, job, transactionKey);

                //importCluster.delete(item, path);
            }
        }
    }

    public void importCollection(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] names = importCluster.getClusterContents(item, COLLECTION, transactionKey);

        for (String name : names) {
            String[] versions = importCluster.getClusterContents(item, COLLECTION+"/"+name, transactionKey);

            for (String version : versions) {
                C2KLocalObject coll = importCluster.get(item, COLLECTION+"/"+name+"/"+version, transactionKey);
                Gateway.getStorage().put(item, coll, transactionKey);

                //importCluster.delete(item, path.toString());
            }
        }
    }

    public void importDomainPath(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] domains = importCluster.getClusterContents(item, PATH+"/Domain", transactionKey);

        for (String name : domains) {
            try {
                Gateway.getLookupManager().add( (DomainPath)importCluster.get(item, PATH+"/Domain/"+name, transactionKey), transactionKey );
            }
            catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException e) {
                log.error("", e);
                throw new PersistencyException(e.getMessage());
            }

            //importCluster.delete(item, PATH+"/Domain/"+name);
        }
    }

    public void importRolePath(ItemPath item, AgentPath agentPath, TransactionKey transactionKey) throws PersistencyException {
        String[] roles = importCluster.getClusterContents(item, PATH+"/Role", transactionKey);

        for (String role : roles) {
            RolePath rolePath = (RolePath)importCluster.get(item, PATH+"/Role/"+role, transactionKey);

            if (!Gateway.getLookup().exists(rolePath, transactionKey)) {
                try {
                    Gateway.getLookupManager().add(rolePath, transactionKey);
                    if (agentPath != null) Gateway.getLookupManager().addRole(agentPath, rolePath, transactionKey);
                }
                catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException | ObjectNotFoundException e) {
                    log.error("", e);
                    throw new PersistencyException(e.getMessage());
                }
            }

            //importCluster.delete(item, PATH+"/Role/"+role);
        }
        
    }

    public ItemPath importItemPath(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        try {
            ItemPath itemPath = (ItemPath)importCluster.get(item, PATH+"/Item", transactionKey);
            Gateway.getLookupManager().add(itemPath, transactionKey);

            //importCluster.delete(item, PATH+"/Item");

            return itemPath;
        }
        catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException e) {
            log.error("", e);
            throw new PersistencyException(e.getMessage());
        }
    }

    public AgentPath importAgentPath(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        try {
            AgentPath agentPath = (AgentPath)importCluster.get(item, PATH+"/Item", transactionKey);
            Gateway.getLookupManager().add(agentPath, transactionKey);

            Gateway.getLookupManager().setAgentPassword(agentPath, "", true, transactionKey);

            //importCluster.delete(item, PATH+"/Item");

            return agentPath;
        }
        catch (ObjectCannotBeUpdated | ObjectAlreadyExistsException | CannotManageException | ObjectNotFoundException | NoSuchAlgorithmException e) {
            log.error("", e);
            throw new PersistencyException(e.getMessage());
        }
        
    }

    public void importPath(ItemPath item, TransactionKey transactionKey) throws PersistencyException {
        String[] contents = importCluster.getClusterContents(item, PATH, transactionKey);

        AgentPath agentPath = null;
        String entity = "";

        if (Arrays.asList(contents).contains("Item"))  entity = "Item";
        if (Arrays.asList(contents).contains("Agent")) entity = "Agent";


        if (StringUtils.isNotBlank(entity)) {
            if      (entity.equals("Item"))  importItemPath(item, transactionKey);
            else if (entity.equals("Agent")) agentPath = importAgentPath(item, transactionKey);
        }
        else log.warn("importPath() - WARNING: '"+item+"' has no Path.Item or Path.Agent files");;

        for (String c : contents) {
            if      (c.equals("Domain")) importDomainPath(item, transactionKey);
            else if (c.equals("Role"))   importRolePath(item, agentPath, transactionKey);
        }
    }
}
