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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cristalise.kernel.collection.Collection;
import org.cristalise.kernel.collection.CollectionMemberList;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.collection.DependencyDescription;
import org.cristalise.kernel.collection.DependencyMember;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescription;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;

import lombok.extern.slf4j.Slf4j;

/**
 * {@value #description}
 */
@Slf4j
public class UpdateCollectionsFromDescription extends PredefinedStep {

    public static final String description = "Updates the Collections of the Item from its description";

    public UpdateCollectionsFromDescription() {
        super(description);
    }

    /**
     * 
     */
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
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

        if (inputs.length != 2 && inputs.length != 3) throw new InvalidDataException("Invalid nunber of inputs:" + Arrays.toString(inputs));

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)inputs);

        ItemProxy descItem = Gateway.getProxy(new DomainPath(inputs[0]));
        String descVer  = inputs[1];
        //use this 3rd parameter to update the members that cannot be calculate from the description
        CollectionMemberList<DependencyMember> newMembers = 
                (CollectionMemberList<DependencyMember>)Gateway.getMarshaller().unmarshall(inputs[2]);

        PropertyArrayList newItemProps = new PropertyArrayList();
        List<String> currentCollNames = new ArrayList<>(Arrays.asList(Gateway.getStorage().getClusterContents(item, COLLECTION, transactionKey)));

        //Loop through collection desc names and create new ones

        for (String collName: descItem.getContents(COLLECTION, transactionKey)) {
            if (! currentCollNames.contains(collName)) {
                Collection<?> newColl = CreateItemFromDescription.instantiateCollection(collName, descItem.getPath(), descVer, newItemProps, transactionKey);

                if (newColl != null) Gateway.getStorage().put(item, newColl, transactionKey);
            }
            else {
                currentCollNames.remove(collName);

                //FIXME: Check if current collection is a Dependency, properties are only available in Dependency and DependencyDescription
                Dependency itemColl = updateDependencyProperties(item, descItem.getPath(), descVer, collName, transactionKey);

                updateDependencyMembers(itemColl, newMembers);

                Gateway.getStorage().put(item, itemColl, transactionKey);
            }
        }

        //instantiating Dependency of Factory creates new Item Property
        for (Property p: newItemProps.list) {
            PropertyUtility.writeProperty(item, p.getName(), p.getValue(), transactionKey);
        }

        //remove remaining collection from current list
        for (String collName: currentCollNames) {
            Gateway.getStorage().remove(item, COLLECTION + "/" + collName, transactionKey);
        }

        return requestData;
    }

    /**
     * 
     * @param newMembers
     * @param itemColl
     * @throws ObjectNotFoundException
     * @throws InvalidCollectionModification
     */
    private static void updateDependencyMembers(Dependency itemColl, CollectionMemberList<DependencyMember> newMembers)
            throws ObjectNotFoundException, InvalidCollectionModification
    {
        for (DependencyMember currentMember: itemColl.getMembers().list) {
            DependencyMember newMember = null;

            if(newMembers != null) {
                newMember = newMembers.list.stream()
                    .filter(aMember -> currentMember.getItemPath().equals(aMember.getItemPath()))
                    .findAny()
                    .orElse(null);
            }

            currentMember.updatePropertieFromDescription(itemColl.getProperties(), newMember);
        }
    }

    /**
     * 
     * @param item
     * @param descItemPath
     * @param descVer
     * @param collName
     * @param transactionKey
     * @return
     * @throws PersistencyException
     * @throws ObjectNotFoundException
     */
    private static Dependency updateDependencyProperties(ItemPath item, ItemPath descItemPath, String descVer, String collName, TransactionKey transactionKey)
            throws PersistencyException, ObjectNotFoundException
    {
        Map<String, Object> newCollProps = new HashMap<>(); // place holder for all properties from the factory

        DependencyDescription collOfDesc = (DependencyDescription)
                Gateway.getStorage().get(descItemPath, COLLECTION + "/" + collName + "/" + descVer, transactionKey);

        newCollProps.putAll(collOfDesc.getProperties());

        // DependencyDescription shall have one member only
        PropertyDescriptionList itemPropertyList = PropertyUtility.getPropertyDescriptionOutcome(
                collOfDesc.getMembers().list.get(0).getItemPath(), descVer, transactionKey);

        for (PropertyDescription prop: itemPropertyList.list) {
            if(prop.getIsClassIdentifier() || prop.isTransitive()){
                newCollProps.put(prop.getName(), prop.getDefaultValue());
            }
        }

        Dependency itemColl = (Dependency) Gateway.getStorage().get(item, COLLECTION + "/" + collName + "/" + descVer, transactionKey);

        Iterator<String> iterator =  itemColl.getProperties().keySet().iterator();

        while (iterator.hasNext()) {
            String keyStr = iterator.next();
            if(! newCollProps.containsKey(keyStr)) {
                iterator.remove();
            }
        }

        // Iterate over newCollProps to add or update properties
        for (Map.Entry<String, Object> propDef : newCollProps.entrySet()) {
            itemColl.getProperties().put(propDef.getKey(), propDef.getValue());
        }

        // updates classProps for Dependency if there is any
        if(itemPropertyList != null) {
            itemColl.setClassProps(itemPropertyList.getClassProps());
        }

        return itemColl;
    }
}
