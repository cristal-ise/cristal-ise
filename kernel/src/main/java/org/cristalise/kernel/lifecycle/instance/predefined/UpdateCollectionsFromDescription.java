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
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyDescription;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescription;
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

        //FIXME: check if inputs are valid
        String descPath = inputs[0]; //i.e. domainPath of FactoryItem
        String descVer  = inputs[1];

        ItemPath descItemPath; // very likely the factory item

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

                //FIXME: Check if current collection is a Dependency, properties are only available in Dependency and DependencyDescription
                Dependency itemColl = updateDependencyCollectionProperties(item, descItemPath, descVer, collName, locker);

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

    private Dependency updateDependencyCollectionProperties(ItemPath item, ItemPath descItemPath, String descVer, String collName, Object locker)
            throws PersistencyException, ObjectNotFoundException
    {
        Map<String, Object> itemCollProps = new HashMap<>(); // place holder for all properties from the factory

        DependencyDescription collOfDesc = (DependencyDescription)
                Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, locker);

        itemCollProps.putAll(collOfDesc.getProperties());

        //DependencyDescription shall have one member only
        PropertyDescriptionList itemPropertyList = PropertyUtility.getPropertyDescriptionOutcome(collOfDesc.getMembers().list.get(0).getItemPath(), descVer, locker);

        for (PropertyDescription prop: itemPropertyList.list) {
            if(prop.getIsClassIdentifier() || prop.isTransitive()){
                itemCollProps.put(prop.getName(), prop.getDefaultValue());
            }
        }

        Dependency itemColl = (Dependency) Gateway.getStorage().get(item, COLLECTION + "/" + collName + "/" + descVer, locker);
 
        for (Map.Entry<String, Object> props : itemColl.getProperties().entrySet()) {
            if(! itemCollProps.containsKey(props.getKey())) {
                // remove property if not exist from the current list
                itemColl.getProperties().remove(props.getKey()); 
            } else {
                // if exists, update the default values if needed
            }
        }

        // Iterate over itemCollProps to add new properties to collection or update member collection props
        for (Map.Entry<String, Object> props : itemCollProps.entrySet()) {
            if(! itemColl.getProperties().containsKey(props.getKey())) {
                // add property if not exist from the current list
                itemColl.getProperties().put(props.getKey(),props.getValue()); 
            } else {
                // update property
                if(props.getValue()!=null && !itemColl.getProperties().get(props.getKey()).toString().equals(props.getValue().toString())){
                    itemColl.getProperties().remove(props.getKey());
                    itemColl.getProperties().put(props.getKey(), props.getValue());
                }
            }
        }

        return itemColl;
    }
}
