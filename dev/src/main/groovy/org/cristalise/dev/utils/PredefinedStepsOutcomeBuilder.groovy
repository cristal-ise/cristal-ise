
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

import static org.cristalise.kernel.lifecycle.instance.Activity.PREDEF_STEPS_ELEMENT

import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.ChangeName
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep
import org.cristalise.kernel.lifecycle.instance.predefined.RemoveMembersFromCollection
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.TransactionKey
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.CastorHashMap
import org.w3c.dom.NodeList

import groovy.transform.CompileStatic

/**
 * Helper class on top of  {@link org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder}.
 * Contains methods to update Outcome with PredefinedStpes.
 */
@CompileStatic
class PredefinedStepsOutcomeBuilder {

    private ItemProxy      item
    private OutcomeBuilder builder;

    /**
     * Constructor to initialise the builder
     *  
     * @param anItem to be updated
     * @param outcome to be initialised
     * @param schema required to build the Outcome
     */
    public PredefinedStepsOutcomeBuilder(ItemProxy anItem, Outcome outcome, Schema schema) {
        item = anItem
        assert schema, 'Cannot initialise wihtout a valid Schema'

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
     * @param transactionKey key of the transaction, can be null
     */
    public void updateOutcomeWithAddMembersToCollection(
        String          dependencyName,
        List<ItemPath>  members,
        TransactionKey  transactionKey = null
    ) {
        // checks if the dependency exists
        (Dependency)item.getCollection(dependencyName, transactionKey)

        def dep = new Dependency(dependencyName)

        for(def itemPath: members) {
            dep.addMember(itemPath, new CastorHashMap(), '', transactionKey)
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
     * @param transactionKey key of the transaction, can be null
     */
    public void updateOutcomeWithAddMembersToCollection(
        String          dependencyName, 
        ItemPath        memberPath, 
        CastorHashMap   memberProps = null, 
        TransactionKey  transactionKey = null
    ) {
        // checks if the dependency exists
        (Dependency)item.getCollection(dependencyName, transactionKey)

        def dep = new Dependency(dependencyName)
        dep.addMember(memberPath, memberProps ?: new CastorHashMap(), '', transactionKey)

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
     * @param transactionKey key of the transaction, can be null
     */
    public void updateOutcomeWithRemoveMembersFromCollection(
        String          dependencyName, 
        int             memberSlotId, 
        ItemPath        memberPath, 
        TransactionKey  transactionKey = null
    ) {
        // checks if the dependency exists
        def currDep = (Dependency)item.getCollection(dependencyName, transactionKey)
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
     * @param transactionKey key of the transaction, can be null
     */
    public void updateOutcomeWithChangeName(String currentName, String newName, TransactionKey  transactionKey = null) {
        def predefStepXpath = initOutcomePredefStepField(ChangeName.class)
        outcome.appendXmlFragment(predefStepXpath, PredefinedStep.bundleData(currentName, newName))
    }
}
