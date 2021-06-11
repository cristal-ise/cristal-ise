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
import org.cristalise.kernel.process.Gateway;
import org.cristalise.storage.XMLClusterStorage;

public class BulkExport extends PredefinedStep{
    
    /**
     * 
     */
    public static final String BULK_EXPORT_ROOT_DIRECTORY = "BulkExport.rootDirectory";
    /**
     * 
     */
    public static final String BULK_EXPORT_USE_DIRECTORIES = "BulkExport.useDirectories";
    /**
     * 
     */
    public static final String BULK_EXPORT_FILE_EXTENSION = "BulkExport.fileExtension";
    
    private String  root;
    private String  ext;
    private Boolean  useDir;

    XMLClusterStorage exportCluster;
    
    public BulkExport() {
        super();

        root   = Gateway.getProperties().getString( BULK_EXPORT_ROOT_DIRECTORY);
        ext    = Gateway.getProperties().getString( BULK_EXPORT_FILE_EXTENSION, "");
        useDir = Gateway.getProperties().getBoolean(BULK_EXPORT_USE_DIRECTORIES, false);
    }
    
    
    public void initialise() throws InvalidDataException {
        if (exportCluster == null) {
            if (root == null)
                throw new InvalidDataException("BulkExport.runActivityLogic() - Root path not given in config file.");

            exportCluster = new XMLClusterStorage(root, ext, useDir);
        }
    }
    

    public void exportAllClusters(ItemPath item) throws InvalidDataException, PersistencyException, ObjectNotFoundException {
       
            Object sublocker = new Object();
            
            String [] clusterContents =  Gateway.getStorage().getClusterContents(item, "");
            for(String str : clusterContents){
                if(str.equals("AuditTrail")){
                    str = "HISTORY";
                }
                ClusterType type = ClusterType.valueOf(str.toUpperCase());
                switch (type) {
                    case PATH:       exportPath(item, sublocker);       break;
                    case PROPERTY:   exportProperty(item, sublocker);   break;
                    case LIFECYCLE:  exportLifeCycle(item, sublocker);  break;
                    case HISTORY:    exportHistory(item, sublocker);    break;
                    case VIEWPOINT:  exportViewPoint(item, sublocker);  break;
                    case OUTCOME:    exportOutcome(item, sublocker);    break;
                    case COLLECTION: exportCollection(item, sublocker); break;
                    case JOB:        exportJob(item, sublocker);        break;
                    case ATTACHMENT: exportAttachment(item, sublocker); break;
                    default:
                        break;
                }
            }
            Gateway.getStorage().commit(sublocker);
     }
    
    public void exportPath(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
        String[] objList  = Gateway.getStorage().getClusterContents(item, PATH + "/", locker);
         for(String obj : objList){
             C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, PATH + "/" +obj,  null);
             exportCluster.put(item,c2KLocalObj);
         }
     } 
     
     public void exportProperty(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
         String[] objList  = Gateway.getStorage().getClusterContents(item, PROPERTY+"/", null);
         for(String prop : objList){
             C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, PROPERTY+"/"+prop, locker);
             exportCluster.put(item,c2KLocalObj);
         }
      } 
      
     
     public void exportLifeCycle(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
         String[] objList  = Gateway.getStorage().getClusterContents(item, LIFECYCLE+"/", locker);
         for(String obj : objList){
             C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, LIFECYCLE+ "/" +obj,  null);
             exportCluster.put(item,c2KLocalObj);
         }
         
      } 
      
     
     public void exportHistory(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
         String[] objList  = Gateway.getStorage().getClusterContents(item, HISTORY + "/", locker);
         for(String obj : objList){
             C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, HISTORY+ "/" +obj,  null);
             exportCluster.put(item,c2KLocalObj);
         }
      } 
      
     
     
     public void exportOutcome(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
        String[] schemas  = Gateway.getStorage().getClusterContents(item, OUTCOME + "/", null);
         for(String schema : schemas){
             String[] versions  = Gateway.getStorage().getClusterContents(item, OUTCOME +"/"+schema,  null);
             
             for(String version : versions){
                 String[] events  = Gateway.getStorage().getClusterContents(item, OUTCOME +"/"+schema+"/"+version,  null);
                 
                 for (String event : events) {
                     C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, OUTCOME+"/"+schema+"/"+version+"/"+event,  null);
                     exportCluster.put(item,c2KLocalObj);
                 }
             }
             
         }
      } 
      
     
     public void exportViewPoint(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
         String[] objList  = Gateway.getStorage().getClusterContents(item, VIEWPOINT + "/", null);
          for(String obj : objList){
              String [] subList =  Gateway.getStorage().getClusterContents(item, VIEWPOINT + "/" +obj,  null);
              for (String subStr : subList){
                  C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, VIEWPOINT + "/" +obj + "/" +subStr,  null);
                  exportCluster.put(item,c2KLocalObj); 
              }
          }
       } 
     
     public void exportCollection(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
         String[] objList  = Gateway.getStorage().getClusterContents(item, COLLECTION + "/", null);
         for(String obj : objList){
             C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, COLLECTION + "/" +obj,  null);
             exportCluster.put(item,c2KLocalObj);
         }
      } 
      
     
     
     public void exportJob(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
        String[] objList  = Gateway.getStorage().getClusterContents(item, JOB + "/", null);
         for(String obj : objList){
             C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, JOB + "/" +obj,  null);
             exportCluster.put(item,c2KLocalObj);
         }
      }
     
     public void exportAttachment(ItemPath item, Object locker) throws PersistencyException, ObjectNotFoundException {
         String[] objList  = Gateway.getStorage().getClusterContents(item, ATTACHMENT + "/", null);
          for(String obj : objList){
              C2KLocalObject c2KLocalObj = Gateway.getStorage().get(item, ATTACHMENT + "/" +obj,  null);
              exportCluster.put(item,c2KLocalObj);
          }
       }
     
    
    
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, Object locker)
            throws InvalidDataException, InvalidCollectionModification, ObjectAlreadyExistsException, ObjectCannotBeUpdated,
            ObjectNotFoundException, PersistencyException, CannotManageException, AccessRightsException {
        // TODO Auto-generated method stub
        initialise();
        
        exportAllClusters(itemPath);
        
        return requestData;
    }

}
