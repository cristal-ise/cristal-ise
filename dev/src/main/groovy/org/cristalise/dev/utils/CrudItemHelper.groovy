
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

import org.cristalise.kernel.collection.Dependency
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.lifecycle.instance.predefined.AddMembersToCollection
import org.cristalise.kernel.lifecycle.instance.predefined.RemoveMembersFromCollection
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.TransactionKey
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.CastorHashMap

import groovy.transform.CompileStatic

@CompileStatic
class CrudItemHelper {

    private CrudItemHelper() {}

    /**
     * Initialises the Outcome with the /PrdefeinedSteps/${PredefStepName} element 
     * 
     * @param outcome to be initialised
     * @param schema required to build the Outcome
     * @param predefStepClazz the class of the predefined step
     * @return the XPath to the initialised element
     */
    public static String initOutcomePredefStepField(Outcome outcome, Schema schema, Class predefStepClazz) {
        String predefStepXpath = '/' + outcome.rootName + '/PredefinedSteps/' + predefStepClazz.getSimpleName()
        OutcomeBuilder builder = new OutcomeBuilder(schema, outcome)

        if (! outcome.getNodeByXPath('//PredefinedSteps')) {
            builder.addField('PredefinedSteps', '')
        }
        builder.addField(predefStepXpath, '')

        return predefStepXpath
    }

    /**
     * Updates the Outcome with the data required to execute AddMembersToCollection 
     * automatically on the item server
     * 
     * @param item to be updated
     * @param outcome to be updated
     * @param schema required to build the Outcome
     * @param dependencyName name of the updated Dependency
     * @param members list of Item to be added
     * @param transactionKey key of the transaction, can be null
     */
    public static void updateOutcomeWithAddMembersToCollection(
        ItemProxy       item,
        Outcome         outcome,
        Schema          schema,
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

        def predefStepXpath = initOutcomePredefStepField(outcome, schema, AddMembersToCollection.class)
        outcome.appendXmlFragment(predefStepXpath, Gateway.getMarshaller().marshall(dep))
    }

    /**
     * Updates the Outcome with the data required to execute AddMembersToCollection 
     * automatically on the item server
     * 
     * @param item to be updated
     * @param outcome to be updated
     * @param schema required to build the Outcome
     * @param dependencyName name of the updated Dependency
     * @param memberPath the Item to be added
     * @param memberProps the member properties associated with the Item
     * @param transactionKey key of the transaction, can be null
     */
    public static void updateOutcomeWithAddMembersToCollection(
        ItemProxy       item,
        Outcome         outcome,
        Schema          schema,
        String          dependencyName, 
        ItemPath        memberPath, 
        CastorHashMap   memberProps = null, 
        TransactionKey  transactionKey = null
    ) {
        // checks if the dependency exists
        (Dependency)item.getCollection(dependencyName, transactionKey)

        def dep = new Dependency(dependencyName)
        dep.addMember(memberPath, memberProps ?: new CastorHashMap(), '', transactionKey)

        def predefStepXpath = initOutcomePredefStepField(outcome, schema, AddMembersToCollection.class)
        outcome.appendXmlFragment(predefStepXpath, Gateway.getMarshaller().marshall(dep))
    }

    /**
     * Updates the Outcome with the data required to execute AddMembersToCollection 
     * automatically on the item server
     * 
     * @param item to be updated
     * @param outcome to be updated
     * @param schema required to build the Outcome
     * @param dependencyName name of the updated Dependency
     * @param memberSlotId if of the slot to be removed. Provide -1 to use memberPath instead
     * @param memberPath the Item to be removed. Can be null when memberSlotId is used
     * @param memberProps the member properties associated with the Item
     * @param transactionKey key of the transaction, can be null
     */
    public static void updateOutcomeWithRemoveMembersFromCollection(
        ItemProxy       item,
        Outcome         outcome,
        Schema          schema,
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

        def predefStepXpath = initOutcomePredefStepField(outcome, schema, RemoveMembersFromCollection.class)
        outcome.appendXmlFragment(predefStepXpath, Gateway.getMarshaller().marshall(dep))
    }
}
