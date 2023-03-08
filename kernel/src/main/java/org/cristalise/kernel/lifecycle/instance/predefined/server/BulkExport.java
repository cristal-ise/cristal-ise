package org.cristalise.kernel.lifecycle.instance.predefined.server;

import static org.cristalise.kernel.SystemProperties.BulkExport_fileExtension;
import static org.cristalise.kernel.SystemProperties.BulkExport_rootDirectory;
import static org.cristalise.kernel.SystemProperties.BulkExport_useDirectories;
import static org.cristalise.kernel.persistency.ClusterType.ATTACHMENT;
import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;
import static org.cristalise.kernel.persistency.ClusterType.HISTORY;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.persistency.ClusterType.LIFECYCLE;
import static org.cristalise.kernel.persistency.ClusterType.OUTCOME;
import static org.cristalise.kernel.persistency.ClusterType.PATH;
import static org.cristalise.kernel.persistency.ClusterType.PROPERTY;
import static org.cristalise.kernel.persistency.ClusterType.VIEWPOINT;

import org.cristalise.kernel.common.AccessRightsException;
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
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.storage.XMLClusterStorage;

public class BulkExport extends PredefinedStep {

    private String root;
    private String ext;
    private Boolean useDir;

    XMLClusterStorage exportCluster;

    public BulkExport() {
        super();

        root = BulkExport_rootDirectory.getString();
        ext = BulkExport_fileExtension.getString();
        useDir = BulkExport_useDirectories.getBoolean();
    }


    public void initialise() throws InvalidDataException {
        if (exportCluster == null) {
            if (root == null) throw new InvalidDataException("Root path not given in config file.");

            exportCluster = new XMLClusterStorage(root, ext, useDir);
        }
    }

    public void exportAllClusters(ItemPath item) throws InvalidDataException, PersistencyException, ObjectNotFoundException {
        TransactionKey transKey = new TransactionKey(item);

        String[] clusterContents = Gateway.getStorage().getClusterContents(item, "");
        for (String str : clusterContents) {
            if (str.equals("AuditTrail")) {
                str = "HISTORY";
            }
            ClusterType type = ClusterType.valueOf(str.toUpperCase());
            switch (type) {
                case PATH:
                    exportPath(item, transKey);
                    break;
                case PROPERTY:
                    exportProperty(item, transKey);
                    break;
                case LIFECYCLE:
                    exportLifeCycle(item, transKey);
                    break;
                case HISTORY:
                    exportHistory(item, transKey);
                    break;
                case VIEWPOINT:
                    exportViewPoint(item, transKey);
                    break;
                case OUTCOME:
                    exportOutcome(item, transKey);
                    break;
                case COLLECTION:
                    exportCollection(item, transKey);
                    break;
                case JOB:
                    exportJob(item, transKey);
                    break;
                case ATTACHMENT:
                    exportAttachment(item, transKey);
                    break;
                default:
                    break;
            }
        }
        Gateway.getStorage().commit(transKey);
    }

    public void exportPath(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, PATH + "/", transKey);
        for (String obj : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, PATH + "/" + obj, null);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    public void exportProperty(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, PROPERTY + "/", null);
        for (String prop : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, PROPERTY + "/" + prop, transKey);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    public void exportLifeCycle(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, LIFECYCLE + "/", transKey);
        for (String obj : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, LIFECYCLE + "/" + obj, null);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    public void exportHistory(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, HISTORY + "/", transKey);
        for (String obj : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, HISTORY + "/" + obj, null);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    public void exportOutcome(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] schemas = Gateway.getStorage().getClusterContents(item, OUTCOME + "/", null);
        for (String schema : schemas) {
            String[] versions = Gateway.getStorage().getClusterContents(item, OUTCOME + "/" + schema, null);

            for (String version : versions) {
                String[] events = Gateway.getStorage().getClusterContents(item, OUTCOME + "/" + schema + "/" + version, null);

                for (String event : events) {
                    C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, OUTCOME + "/" + schema + "/" + version + "/" + event, null);
                    exportCluster.put(item, c2KLocalObj, transKey);
                }
            }
        }
    }


    public void exportViewPoint(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, VIEWPOINT + "/", null);
        for (String obj : objList) {
            String[] subList = Gateway.getStorage().getClusterContents(item, VIEWPOINT + "/" + obj, null);
            for (String subStr : subList) {
                C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, VIEWPOINT + "/" + obj + "/" + subStr, null);
                exportCluster.put(item, c2KLocalObj, transKey);
            }
        }
    }

    public void exportCollection(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, COLLECTION + "/", null);
        for (String obj : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, COLLECTION + "/" + obj, null);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    public void exportJob(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, JOB + "/", null);
        for (String obj : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, JOB + "/" + obj, null);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    public void exportAttachment(ItemPath item, TransactionKey transKey) throws PersistencyException, ObjectNotFoundException {
        String[] objList = Gateway.getStorage().getClusterContents(item, ATTACHMENT + "/", null);
        for (String obj : objList) {
            C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, ATTACHMENT + "/" + obj, null);
            exportCluster.put(item, c2KLocalObj, transKey);
        }
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transKey)
            throws InvalidDataException, InvalidCollectionModification,
            ObjectAlreadyExistsException, ObjectCannotBeUpdated, ObjectNotFoundException,
            PersistencyException, CannotManageException, AccessRightsException
    {
        initialise();

        exportAllClusters(itemPath);

        return requestData;
    }
}
