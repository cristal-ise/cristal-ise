
/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.dev.utils

import static org.cristalise.kernel.collection.BuiltInCollections.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import static org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription.FACTORY_GENERATED_NAME
import static org.cristalise.kernel.property.BuiltInItemProperties.*;

import org.apache.commons.lang3.StringUtils
import org.cristalise.kernel.collection.BuiltInCollections
import org.cristalise.kernel.collection.DependencyMember
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.CreateAgentFromDescription
import org.cristalise.kernel.lifecycle.instance.predefined.item.CreateItemFromDescription
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.TransactionKey
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.property.PropertyArrayList
import org.cristalise.kernel.utils.LocalObjectLoader
import org.w3c.dom.Node

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class CrudFactoryHelper {
    /**
     * 
     * @param factoryItem to create new Agents/Items
     * @param job
     * @param newItemName
     * @return
     */
    private static String getInitaliseOutcomeXML(ItemProxy factoryItem, Outcome outcome, String newItemName, TransactionKey transKey) {
        if (! factoryItem.checkCollection(SCHEMA_INITIALISE)) {
            log.debug('getInitaliseOutcomeXML() - CrudFactory:{} does not have collection:{}', factoryItem, SCHEMA_INITIALISE)
            return null
        }

        String outcomeRoot = outcome.getRootName()

        def initSchemaCollection = factoryItem.getCollection(SCHEMA_INITIALISE, transKey)
        DependencyMember member = (DependencyMember)initSchemaCollection.getMembers().list[0]

        def updateSchemaUUID = member.getChildUUID()
        def updateSchemaVersion = member.getProperties().getBuiltInProperty(VERSION)
        if (updateSchemaVersion instanceof String) updateSchemaVersion = Integer.parseInt(updateSchemaVersion)

        def updateSchema = LocalObjectLoader.getSchema(updateSchemaUUID, (Integer)updateSchemaVersion, transKey).getName()

        outcome.setFieldByXPath("$outcomeRoot/SchemaInitialise/$updateSchema/Name", newItemName)
        def initialiseNode = outcome.getNodeByXPath("/$outcomeRoot/SchemaInitialise/$updateSchema")

        if (initialiseNode) {
            String initOutcomeXml = Outcome.serialize(initialiseNode, true)

            log.debug('getInitaliseOutcomeXML() - factory:{} returning xml:{}', factoryItem, initOutcomeXml);

            return initOutcomeXml
        }
        else {
            throw new InvalidDataException("CrudFactory:$factoryItem - invalid path:/$outcomeRoot/SchemaInitialise/$updateSchema")
        }
    }

    /**
     *
     * @param factoryItem
     * @param outcome
     * @param transKey
     * @return
     */
    public static String getItemName(ItemProxy factoryItem, Outcome outcome) {
        return getItemName(factoryItem, outcome, factoryItem.getTransactionKey())
    }

    /**
     * 
     * @param factoryItem
     * @param outcome
     * @param transKey
     * @return
     */
    public static String getItemName(ItemProxy factoryItem, Outcome outcome, TransactionKey transKey) {
        String itemName = null

        if (factoryItem.checkProperty(ID_PREFIX, transKey)) {
            itemName = FACTORY_GENERATED_NAME
            String outcomeRoot = outcome.getRootName()
            outcome.setFieldByXPath("$outcomeRoot/Name", itemName);
        }
        else {
            //Name was provided by the user/agent
            itemName = outcome.getField('Name')

            if (StringUtils.isBlank(itemName) || itemName == 'string' || itemName == 'null') {
                throw new InvalidDataException("CrudFactory:$factoryItem - Name must be provided")
            }
        }

        log.debug('getItemName() - factory:{} newItemName:{}', factoryItem, itemName);

        return itemName
    }

    /**
     * 
     * @param factoryItem
     * @param job
     * @return
     */
    public static String getDomainRoot(ItemProxy factoryItem, Job job) {
        return getDomainRoot(factoryItem, job, factoryItem.getTransactionKey())
    }

    /**
     * 
     * @param factoryItem
     * @param job
     * @param transKey
     * @return
     */
    public static String getDomainRoot(ItemProxy factoryItem, Job job, TransactionKey transKey) {
        String root = factoryItem.getProperty('Root', transKey)
        if (!root) root = job.getActPropString('Root')

        if (!root) throw new InvalidDataException("CrudFactory:$factoryItem - Define property:'Root' for either Activity or for Item")

        String subFolder = job.outcome.getField('SubFolder')
        String domainRoot = (subFolder && subFolder != 'string') ? "${root}/${subFolder}" : root

        log.debug('getDomainRoot() - factory:{} domainRoot:{}', factoryItem, domainRoot);

        return domainRoot
    }

    /**
     * If the factory Item creates Agents or not
     * 
     * @param factoryItem to create new Items
     * @return true if the factory Item creates Agents, or returns false if creates Items
     */
    public static boolean isCreateAgent(ItemProxy factoryItem) {
        return isCreateAgent(factoryItem, factoryItem.getTransactionKey())
    }

    /**
     * If the factory Item creates Agents or not
     * 
     * @param factoryItem to create new Items
     * @param transKey
     * @return true if the factory Item creates Agents, or returns false if creates Items
     */
    public static boolean isCreateAgent(ItemProxy factoryItem, TransactionKey transKey) {
        return new Boolean(factoryItem.getProperty('CreateAgent', 'false', transKey))
    }

    /**
     * Calculate the predefined step, i.e. CreateAgentFromDescription or CreateItemFromDescription
     * 
     * @param item the factory Item
     * @return CreateAgentFromDescription for factories creating Agents or CreateItemFromDescription
     */
    public static Class<?> getPredefStep(ItemProxy factoryItem) {
        return getPredefStep(factoryItem, factoryItem.getTransactionKey())
    }

    /**
     * Calculate the predefined step, i.e. CreateAgentFromDescription or CreateItemFromDescription
     *
     * @param item the factory Item
     * @param transKey
     * @return CreateAgentFromDescription for factories creating Agents or CreateItemFromDescription
     */
    public static Class<?> getPredefStep(ItemProxy factoryItem, TransactionKey transKey) {
        return isCreateAgent(factoryItem, transKey) ? CreateAgentFromDescription.class : CreateItemFromDescription.class
    }

    /**
     * 
     * @param factoryItem
     * @param agent
     * @param job
     * @param newItemName
     * @return
     */
    public static String[] getParams(ItemProxy factoryItem, AgentProxy agent, Job job, String newItemName) {
        return getParams(factoryItem, agent, job, newItemName, factoryItem.getTransactionKey())
    }

    /**
    /**
     * Reads the optional PropertyList XMl fragment from the Outcome usually created by a CRUD Factory
     * 
     * The 'Empty' OutcomeInitiator creates invalid empty PropertyList, which has one Property element 
     * with the name attribute empty. This is recognised and ignored!
     * 
     * @param factoryItem
     * @param outcome optionally containing the PropertyList
     * @return the valid XML fragment extracted from the Outcome or null
     */
    public static String getInitialProperties(ItemProxy factoryItem, Outcome outcome) {
        def nodes = outcome.getNodesByXPath('//PropertyList/Property')

        if (nodes) {
            Node validNode = null;

            if (nodes.getLength() == 1) {
                if (outcome.getFieldByXPath('//PropertyList/Property/@name')) {
                    validNode = outcome.getNodeByXPath('//PropertyList')
                }
                else {
                    log.trace('getInitialProperties() - factory:{} IGNORING invalid PropertyList in Outcome:{}', factoryItem, outcome)
                }
            }
            else {
                validNode = outcome.getNodeByXPath('//PropertyList')
            }

            if (validNode) {
                def outcomeString = outcome.serialize(validNode, false)
                log.debug('getInitialProperties() - factory:{} outcomeString: {}', factoryItem, outcomeString)

                return outcomeString
            }
        }

        return null
    }

    /**
     * 
     * @param factoryItem
     * @param agent
     * @param job
     * @param newItemName
     * @param transKey
     * @return
     */
    public static String[] getParams(ItemProxy factoryItem, AgentProxy agent, Job job, String newItemName, TransactionKey transKey) {
        String[] params = null
        String domainRoot = getDomainRoot(factoryItem, job, transKey)
        String initialProps = getInitialProperties(factoryItem, job.outcome)
        String initaliseOutcomeXML = getInitaliseOutcomeXML(factoryItem, job.getOutcome(), newItemName, transKey)

        if (isCreateAgent(factoryItem)) {
            if      (initaliseOutcomeXML) params = new String[7]
            else if (initialProps)        params = new String[6]
            else                          params = new String[4]

            params[0] = newItemName
            params[1] = domainRoot
            params[2] = factoryItem.getProperty('DefaultRoles', 'Admin')
            params[3] = factoryItem.getProperty('DefaultPassword', 'password')
        }
        else {
            if      (initaliseOutcomeXML) params = new String[5]
            else if (initialProps)        params = new String[4]
            else                          params = new String[2]

            params[0] = newItemName
            params[1] = domainRoot
        }

        if (initaliseOutcomeXML) {
            params[params.length-3] = 'last'
            params[params.length-2] = initialProps ?: agent.marshall(new PropertyArrayList())
            params[params.length-1] = initaliseOutcomeXML
        }
        else if (initialProps) {
            params[params.length-2] = 'last'
            params[params.length-1] = initialProps
        }

        log.debug('getParams() - factory:{} params:{}', factoryItem, params);

        return params
    }
}
