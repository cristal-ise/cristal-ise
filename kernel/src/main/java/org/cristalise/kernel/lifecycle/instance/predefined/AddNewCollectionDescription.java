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
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.AggregationDescription;
import org.cristalise.kernel.collection.CollectionDescription;
import org.cristalise.kernel.collection.DependencyDescription;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.CastorHashMap;
import lombok.extern.slf4j.Slf4j;


/**
 * Generates a new empty collection description. Collection instances should
 * be added by an Admin, who can do so using AddC2KObject.
 */
@Slf4j
public class AddNewCollectionDescription extends PredefinedStep {

    public AddNewCollectionDescription() {
        super();
    }

    /**
     * <pre>
     * Params:
     * 0 - collection name
     * 1 - collection type (Aggregation | Dependency)
     * 2 - properties - CastorHasMap marshaled
     * 3 - Member DomainPath to specify the Type of the member Item
     * </pre>
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectAlreadyExistsException, PersistencyException
    {
        // extract parameters
        String[] params = getDataList(requestData);

        log.debug("Called by {} on {} with parameters {}", agent.getAgentName(transactionKey), item, Arrays.toString(params));

        if (params.length < 2 || params.length > 4) {
            throw new InvalidDataException("Invalid parameters " + Arrays.toString(params));
        }

        String collName = params[0];
        String collType = params[1];

        if (StringUtils.isBlank(collName) || StringUtils.isBlank(collType)) {
            throw new InvalidDataException("Collection Name and Type cannot be blank - params:" + Arrays.toString(params));
        }

        // check if collection already exists
        if (Gateway.getStorage().getClusterContents(item, COLLECTION + "/" + collName).length != 0) {
            throw new ObjectAlreadyExistsException("Collection '" + collName + "' already exists");
        }

        CollectionDescription<?> newCollDesc;

        if (collType.equalsIgnoreCase("Aggregation")) {
            newCollDesc = createAggregationDescription(params, collName, transactionKey);
        }
        else if (collType.equalsIgnoreCase("Dependency")) {
            newCollDesc = createDependencyDescription(params, collName, transactionKey);
        }
        else {
            throw new InvalidDataException("Collection type must be either Aggregation or Dependency - value:" + collType);
        }

        Gateway.getStorage().put(item, newCollDesc, transactionKey);

        return requestData;
    }

    /**
     * 
     * @param params
     * @param collName
     * @param transactionKey
     * @return
     * @throws InvalidDataException
     */
    protected CollectionDescription<?> createAggregationDescription(String[] params, String collName, TransactionKey transactionKey)
            throws InvalidDataException
    {
        return new AggregationDescription(collName);
    }

    /**
     * 
     * @param params
     * @param collName
     * @param transactionKey
     * @return
     * @throws InvalidDataException
     */
    protected CollectionDescription<?> createDependencyDescription(String[] params, String collName, TransactionKey transactionKey)
            throws InvalidDataException
    {
        try {
            DependencyDescription newDepDesc = new DependencyDescription(collName);

            if (params.length > 2) {
                CastorHashMap props = (CastorHashMap) Gateway.getMarshaller().unmarshall(params[2]);
                newDepDesc.setProperties(props);
            }

            if (params.length > 3) {
                ItemPath memberPath = Gateway.getLookup().resolvePath(new DomainPath(params[3]), transactionKey);
                newDepDesc.addMember(memberPath, transactionKey);
            }
            return newDepDesc;
        }
        catch (Exception e) {
            throw new InvalidDataException(e);
        }
    }
}
