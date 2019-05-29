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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.CollectionMember;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;
import org.cristalise.kernel.utils.Logger;

/**
 * {@value #description}
 */
public class UpdateCollectionsFromDescription extends PredefinedStep {

    public static final String description = "Updates the Collections of the Item from its description";

    public UpdateCollectionsFromDescription() {
        super();
    }

    /**
     * 
     */
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, Object locker)
            throws  InvalidDataException,
                    InvalidCollectionModification,
                    ObjectAlreadyExistsException,
                    ObjectCannotBeUpdated,
                    ObjectNotFoundException,
                    PersistencyException,
                    CannotManageException,
                    AccessRightsException
    {
        String[] inputs = getDataList(requestData);

        //implement checking of valid inputs
        
        String descPath = inputs[0]; //i.e. domainPath of FactoryItem
        String descVer  = inputs[1];

        ItemPath descItemPath; // employee factory

        try {
            descItemPath = Gateway.getLookup().resolvePath(new DomainPath(descPath));
        }
        catch (InvalidItemPathException e) {
            Logger.error(e);
            throw new InvalidDataException(e.getMessage());
        }

        PropertyArrayList newItemProps = new PropertyArrayList();
        List<String> currentCollNames = new ArrayList<>(Arrays.asList(Gateway.getStorage().getClusterContents(item, COLLECTION)));

        //Loop through collection desc names and create new ones
        for (String collName :  Gateway.getStorage().getClusterContents(descItemPath, COLLECTION, locker)) {
            if (! currentCollNames.contains(collName)) {
                Collection<?> newColl = CreateItemFromDescription.instantiateCollection(collName, descItemPath, descVer, newItemProps, locker);

                if (newColl != null) Gateway.getStorage().put(item, newColl, locker);
            }
            else {
                currentCollNames.remove(collName);
                
                //update collection properties if needed
                Map<String, Object> itemCollProps = new HashMap<>();
               
                @SuppressWarnings("unchecked")
                Collection<? extends CollectionMember> collOfDesc = (Collection<? extends CollectionMember>)
                        Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, locker);
                
                itemCollProps.putAll(((Dependency) collOfDesc).getProperties());
   
                PropertyDescriptionList itemPropertyList = PropertyUtility.getPropertyDescriptionOutcome(collOfDesc.getMembers().list.get(0).getItemPath(), descVer, locker);
                
                if(!itemPropertyList.list.isEmpty()){
                    itemPropertyList.list.forEach(props -> {
                        if(props.getIsClassIdentifier()){
                            itemCollProps.put(props.getName(), props.getDefaultValue());
                        }
                        if(props.isTransitive()){
                            itemCollProps.put(props.getName(), props.getDefaultValue());    
                        }
                    });
                }
                
                @SuppressWarnings("unchecked")
                Collection<? extends CollectionMember> itemColl = (Collection<? extends CollectionMember>)
                        Gateway.getStorage().get(item, COLLECTION + "/" + collName + "/" + descVer, locker);
 
                for(Map.Entry<String, Object> props : ((Dependency) itemColl).getProperties().entrySet()){
                    if(!itemCollProps.containsKey(props.getKey())){
                        ((Dependency) itemColl).getProperties().remove(props.getKey());
                    } else {
                        // do update the values if needed
                    }
                }
                Gateway.getStorage().put(item, itemColl, locker);
            }
        }

        //instantiating Dependency of Factory creates new Item Property
        for (Property p: newItemProps.list) {
            PropertyUtility.writeProperty(item, p.getName(), p.getValue(), locker);
        }

        //remove remaining collection from current list
        for (String collName: currentCollNames) {
            Gateway.getStorage().remove(item, COLLECTION + "/" + collName, locker);
        }

        return requestData;
    }
}
