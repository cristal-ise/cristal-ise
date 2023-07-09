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

import static org.atteo.evo.inflector.English.plural
import static org.cristalise.kernel.lifecycle.instance.Activity.PREDEF_STEPS_ELEMENT

import org.cristalise.dev.dsl.DevXMLUtility
import org.cristalise.kernel.persistency.outcomebuilder.Field
import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep
import org.cristalise.kernel.lifecycle.instance.predefined.RemoveMembersFromCollection
import org.cristalise.kernel.lookup.DomainPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.TransactionKey
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.CastorHashMap
import org.json.JSONArray
import org.w3c.dom.NodeList

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Helper class on top of  {@link org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder}.
 * Contains methods to update Outcome with PredefinedStpes.
 */
@CompileStatic @Slf4j
class PredefinedStepsOutcomeBuilder {

    private ItemProxy      item
    private OutcomeBuilder builder
    
    private final TransactionKey transaction

    /**
     * Constructor to initialise the builder
     *  
     * @param anItem to be updated
     * @param outcome to be initialised - can be null
     * @param schema required to build the Outcome
     */
    public PredefinedStepsOutcomeBuilder(ItemProxy anItem, Outcome outcome = null, Schema schema, TransactionKey transKey = null) {
        assert schema, 'Cannot initialise wihtout a valid Schema'

        item = anItem
        transaction = transKey

        if (outcome) builder = new OutcomeBuilder(schema, outcome)
        else         builder = new OutcomeBuilder(schema)
    }

    /**
     * Convenience method to retrieve the Outcome. Outcome will not be validate.
     * 
     * @return the Outcome (not validated) 
     */
    public Outcome getOutcome() {
        return getOutcome(false)
    }

    /**
     * Convenience method to retrieve the Outcome. Outcome migth not be validated.
     * 
     * @param validate whether the Outcome shall be validated or not
     * @return the Outcome 
     */
    public Outcome getOutcome(boolean validate) {
        return builder.getOutcome(validate)
    }

    /**
     * Convenience method to retrieve the Schema
     * 
     * @return the Schema
     */
    public Schema getSchema() {
        return getOutcome().getSchema()
    }

    /**
     * Initialises the Outcome with the /${OutcomeRoot}/PredefeinedSteps/${PredefStepName} element 
     * 
     * @param predefStepClazz the class of the predefined step
     * @return the XPath to the initialised element
     */
    public String initOutcomePredefStepField(Class predefStepClazz) {
        String predefStepsPath = '/' + outcome.rootName + '/' + PREDEF_STEPS_ELEMENT

        if (! outcome.getNodeByXPath(predefStepsPath)) {
            builder.addField(PREDEF_STEPS_ELEMENT)
        }

        // only needed to count to number of nodes
        NodeList nodes = outcome.getNodesByXPath( '//' + predefStepClazz.getSimpleName())
        int index = nodes != null ? nodes.length + 1: 0

        builder.addField(predefStepsPath + '/' + predefStepClazz.getSimpleName())

        if (index == 0) return predefStepsPath + '/' + predefStepClazz.getSimpleName()
        else            return predefStepsPath + '/' + predefStepClazz.getSimpleName() + '[' + index + ']'
    }

    /**
     * Initialises the Outcome with the /${OutcomeRoot}/PredefeinedSteps/${PredefStepName} element 
     * and appends the given xml
     * 
     * @param predefStepClazz the class of the predefined step
     * @param xml the be added to the Outcome
     * @return the initialised Outcome
     */
    public Outcome initOutcomePredefStepField(Class predefStepClazz, String xml) {
        def predefStepXpath = initOutcomePredefStepField(predefStepClazz)
        builder.outcome.appendXmlFragment(predefStepXpath, xml)
        return builder.outcome
    }

    /**
     * Updates the Outcome with the data required to execute AddMembersToCollection 
     * automatically on the item server.
     * 
     * @param dependencyName name of the updated Dependency
     * @param members list of Item to be added
     */
    public void updateOutcomeWithAddMembersToCollection(
        String          dependencyName,
        List<ItemPath>  members
    ) {
        // checks if the dependency exists
        (Dependency)item.getCollection(dependencyName, transaction)

        def dep = new Dependency(dependencyName)

        for(def itemPath: members) {
            dep.addMember(itemPath, new CastorHashMap(), '', transaction)
        }

        def predefStepXpath = initOutcomePredefStepField(AddMembersToCollection.class)
        outcome.appendXmlFragment(predefStepXpath, Gateway.getMarshaller().marshall(dep))
    }

    /**
     * Updates the Outcome with the data required to execute AddMembersToCollection 
     * automatically on the item server
     * 
     * @param dependencyName name of the updated Dependency
     * @param memberPath the Item to be added
     * @param memberProps the member properties associated with the Item, can be null
     */
    public void updateOutcomeWithAddMembersToCollection(
        String          dependencyName, 
        ItemPath        memberPath, 
        CastorHashMap   memberProps = null
    ) {
        // checks if the dependency exists
        (Dependency)item.getCollection(dependencyName, transaction)

        def dep = new Dependency(dependencyName)
        dep.addMember(memberPath, memberProps ?: new CastorHashMap(), '', transaction)

        def predefStepXpath = initOutcomePredefStepField(AddMembersToCollection.class)
        outcome.appendXmlFragment(predefStepXpath, Gateway.getMarshaller().marshall(dep))
    }

    /**
     * Updates the Outcome with the data required to execute RemoveMembersFromCollection 
     * automatically on the item server
     * 
     * @param dependencyName name of the updated Dependency
     * @param memberSlotId if of the slot to be removed. Provide -1 to use memberPath instead
     * @param memberPath the Item to be removed. Can be null when memberSlotId is used
     * @param memberProps the member properties associated with the Item
     */
    public void updateOutcomeWithRemoveMembersFromCollection(
        String          dependencyName, 
        int             memberSlotId, 
        ItemPath        memberPath
    ) {
        // checks if the dependency exists
        def currDep = (Dependency)item.getCollection(dependencyName, transaction)
        def dep = new Dependency(dependencyName)

        if (memberSlotId == -1) dep.addMember(currDep.getMember(memberPath))
        else                    dep.addMember(currDep.getMember(memberSlotId))

        def predefStepXpath = initOutcomePredefStepField(RemoveMembersFromCollection.class)
        outcome.appendXmlFragment(predefStepXpath, Gateway.getMarshaller().marshall(dep))
    }

    /**
     * Updates the Outcome with the data required to execute ChangeName 
     * automatically on the item server
     * 
     * @param currentName of the Item
     * @param newName of the Item
     */
    public void updateOutcomeWithChangeName(String currentName, String newName) {
        def predefStepXpath = initOutcomePredefStepField(ChangeName.class)
        outcome.appendXmlFragment(predefStepXpath, PredefinedStep.bundleData(currentName, newName))
    }

    /**
     * Checks if the given field is referencing Item(s) use appInfo meta data in Schema to 
     * find the existing Collection of the Item. The order of checks:
     * <pre>
     * 1. check using collectionName from appInfo['reference'] if it was defined in appInfo
     * 2. check using referencedItemType from appInfo['reference']
     * 3. check using the gven fieldName
     * 4. check using the plural form of referencedItemType
     * </pre>
     * 
     * @param fieldName the actual field of the Outcome to be analysed
     * @param transaction key, can be null
     * @return the name of the existing Collection otherwise returns null if referencedItemType was not specified
     * @throws InvalidDataException if referencedItemType was specified but no Collection can be found
     */
    public String getReferencedDependencyName(String fieldName, TransactionKey transaction = null) {
        def field = (Field)builder.findChildStructure(fieldName)

        String referencedItemType       = field?.getAppInfoNodeElementValue('reference', 'itemType')
        String referencedCollectionName = field?.getAppInfoNodeElementValue('reference', 'collectionName')

        if (referencedItemType) {
            if (referencedCollectionName) {
                if (item.checkCollection(referencedCollectionName, transaction)) return referencedCollectionName
                throw new InvalidDataException("'$item' has no Collection:'$referencedCollectionName'")
            }
            else {
                def possibleCollNames = [referencedItemType, fieldName, plural(referencedItemType)]
                
                possibleCollNames.each { collName ->
                    if (item.checkCollection(collName, transaction)) return collName
                }

                throw new InvalidDataException("'$item' has none of these Collections:${possibleCollNames}")
            }
        }

        return null
    }

    public String convertItemNamesToUuids(String fieldName, String fieldValue, String moduleNs) {
        def field = (Field)builder.findChildStructure((String)fieldName)

        String referencedItemType = field?.getAppInfoNodeElementValue('reference', 'itemType')

        // there is nothing can be done
        if (!referencedItemType) return fieldValue

        def typeFolder = plural(referencedItemType)
        Boolean isMultiple = field.getAppInfoNodeElementValue('dynamicForms', 'multiple') as Boolean

        def convertedValue = ''

        // use JSONArray format
        if (isMultiple) {
            def jsonarray = new JSONArray()

            fieldValue.toString().split(',').each { value ->
                if (ItemPath.isUUID(value)) {
                    jsonarray.put(value)
                }
                else {
                    def domainPath = new DomainPath("$moduleNs/${typeFolder}/$value")
                    jsonarray.put(domainPath.getItemPath(transaction).name)
                }
            }

            convertedValue =  jsonarray.toString()
        }
        else {
            if (! ItemPath.isUUID(fieldValue as String)) {
                def domainPath = new DomainPath("$moduleNs/${typeFolder}/$fieldValue")
                convertedValue = domainPath.getItemPath(transaction).name
            }
        }

        log.debug('convertItemNamesToUuids() - field:{} replacing value {} with {}', fieldName, fieldValue, convertedValue)

        return convertedValue
    }
    
    /**
     * Loop the record for fields containing Item names and convert them to UUID string.
     *
     * @param record the list of fields to be processed
     */
    public void convertItemNamesToUuids(Map<String, Object> record, String moduleNs) {
        record.each { fieldName, fieldValue ->
            record[fieldName] = convertItemNamesToUuids(fieldName, fieldValue as String, moduleNs)
        }
    }

    /**
     * 
     * @param record
     */
    public void updateOutcomeWithAddMembersToCollection(Map<String, Object> record, String moduleNs) {
        record.each { fieldName, fieldValue ->
            def dependencyName = getReferencedDependencyName(fieldName, transaction)

            if (dependencyName) {
                def convertedValue = convertItemNamesToUuids(fieldName, fieldValue as String, moduleNs)

                outcome.setField(fieldName, convertedValue)

                def field = (Field)builder.findChildStructure((String)fieldName)
                Boolean isMultiple = field?.getAppInfoNodeElementValue('dynamicForms', 'multiple') as Boolean
                List<ItemPath> memberPathes = []

                if (isMultiple) {
                    def jsonarray = new JSONArray(convertedValue)

                    for (int i = 0; i < jsonarray.length(); i++) {
                        def uuid = jsonarray.getString(i)
                        memberPathes << new ItemPath(uuid)
                    }
                }
                else {
                    memberPathes << new ItemPath(convertedValue)
                }

                for (def memberPath: memberPathes) {
                    updateOutcomeWithAddMembersToCollection(dependencyName, memberPath)
                }
            }
        }
    }

}
