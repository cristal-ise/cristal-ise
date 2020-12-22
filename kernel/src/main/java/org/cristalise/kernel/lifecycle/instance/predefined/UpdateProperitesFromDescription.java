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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;
import org.cristalise.kernel.property.PropertyArrayList;
import org.cristalise.kernel.property.PropertyDescriptionList;
import org.cristalise.kernel.property.PropertyUtility;

import lombok.extern.slf4j.Slf4j;


/**
 * {@value #description}
 */
@Slf4j
public class UpdateProperitesFromDescription extends PredefinedStep {

    public static final String description = "Updates the Properties of the Item from its description";

    public UpdateProperitesFromDescription() {
        super();
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

        String descPath = inputs[0]; //i.e. domainPath of FactoryItem
        String descVer  = inputs[1];
        PropertyArrayList initProps = inputs.length == 3 && StringUtils.isNotBlank(inputs[2]) ? unmarshallInitProperties(inputs[2]) : new PropertyArrayList();

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, (Object)inputs);

        PropertyDescriptionList newPropDesc = getPropertyDesc(descPath, descVer, transactionKey);

        //Delete or update existing Properties
        for (String existingPropName: Gateway.getStorage().getClusterContents(item, ClusterType.PROPERTY)) {
            if (newPropDesc.definesProperty(existingPropName)) {
                Property existingProp = PropertyUtility.getProperty(item, existingPropName, transactionKey);

                //TODO: cover the cases when the mutable flag was changed
                if (existingProp.isMutable()) {
                    //Update existing mutable Property if initial Property list contains it
                    Property initProp = initProps.get(existingPropName);

                    if (initProp != null) {
                        existingProp.setValue(initProp.getValue());
                        Gateway.getStorage().put(item, existingProp, transactionKey);
                    }
                }
                else {
                    //update existing immutable Property if its default value was changed
                    String defaultValue = PropertyUtility.getDefaultValue(newPropDesc.list, existingPropName);

                    if (StringUtils.isNotBlank(defaultValue) && !defaultValue.equals(existingProp.getValue())) {
                        existingProp.setValue(defaultValue);
                        Gateway.getStorage().put(item, existingProp, transactionKey);
                    }
                }
            }
            else  {
                //Delete Property as it does not exist in definition
                Gateway.getStorage().remove(item, ClusterType.PROPERTY + "/" + existingPropName, transactionKey);
            }
        }

        //Add new properties
        for (Property newProp: newPropDesc.instantiate(initProps).list) {
            if (!PropertyUtility.propertyExists(item, newProp.getName(), transactionKey)) {
                Gateway.getStorage().put(item, newProp, transactionKey);
            }
        }

        return requestData;
    }

    /**
     * 
     * @param descPath
     * @param descVer
     * @param transactionKey
     * @return
     * @throws ObjectNotFoundException
     * @throws InvalidDataException
     */
    private PropertyDescriptionList getPropertyDesc(String descPath, String descVer, TransactionKey transactionKey) throws ObjectNotFoundException, InvalidDataException {
        ItemPath descItemPath;

        try {
            descItemPath = Gateway.getLookup().resolvePath(new DomainPath(descPath), transactionKey);
        }
        catch (InvalidItemPathException e) {
            log.error("", e);
            throw new InvalidDataException(e.getMessage());
        }

        return PropertyUtility.getPropertyDescriptionOutcome(descItemPath, descVer, transactionKey);
    }

    /**
     * Unmarshalls initial Properties
     *
     * @param initPropString
     * @return unmarshalled initial PropertyArrayList
     * @throws InvalidDataException
     */
    protected PropertyArrayList unmarshallInitProperties(String initPropString) throws InvalidDataException {
        try {
            return (PropertyArrayList) Gateway.getMarshaller().unmarshall(initPropString);
        }
        catch (Exception e) {
            log.error("Initial property parameter was not a marshalled PropertyArrayList", e);
            throw new InvalidDataException("Initial property parameter was not a marshalled PropertyArrayList: " + initPropString);
        }
    }
}
