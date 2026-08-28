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

import static org.cristalise.kernel.lifecycle.instance.predefined.CreateItemFromDescription.instantiateWorkflow;
import static org.cristalise.kernel.lifecycle.instance.predefined.ReplaceDomainWorkflow.replaceDomainWorkflow;

import java.util.List;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.process.Gateway;


/**
 * {@value #description}
 * <pre>
 * 1. read current job and consolidate it with field StepPathToEnable to know which activity needs to be enabled
 * 2. loop through new workflow and set state for each Activity 
 * 
 * x. Recalculate jobs and store them
 * </pre>
 * 
 */
@Slf4j
public class UpdateWorkflowFromDescription extends PredefinedStep {

    public static final String description = "Updates the Workflow of the Item from its description.";

//    private List<String> actsToEnable = null;

    public UpdateWorkflowFromDescription() {
        super("WorkflowMigrationData", description);
    }

    /**
     * 
     */
    @Override
    protected String runActivityLogic(AgentPath agentP, ItemPath itemP, int transitionID, String requestData, TransactionKey transactionKey)
            throws  InvalidDataException,
                    InvalidCollectionModification,
                    ObjectAlreadyExistsException,
                    ObjectCannotBeUpdated,
                    ObjectNotFoundException,
                    PersistencyException,
                    CannotManageException,
                    AccessRightsException
    {
        Outcome wfMigrationData = new Outcome(requestData);
        
        String[] nameAndVersion = wfMigrationData.getField("DescItemUrn").split(":");
        ItemProxy descItem = Gateway.getProxy(ItemPath.getItemPath(nameAndVersion[0], transactionKey), transactionKey);

//      String descType = Gateway.getProxy(itemP).getType();
//      actsToEnable = Arrays.asList(Gateway.getProperties().getString(descType+".Activity", "").split(","));

        CompositeActivity newCompAct = instantiateWorkflow(descItem, nameAndVersion[1], transactionKey);

        migrateCompositeActivity(newCompAct, transactionKey);
        replaceDomainWorkflow(agentP, itemP, getWf(), newCompAct, transactionKey);

        return requestData;
    }

    private void migrateActivityState(Activity newAct, TransactionKey transactionKey) throws InvalidDataException {
        log.info("migrateActivityState() - {}", newAct.getPath());
        Activity currentAct = (Activity) getWf().search(newAct.getPath());

        if (currentAct != null) {
            newAct.setState(currentAct.getState());
            newAct.active = currentAct.active;
        }
        else {
            
        }
    }

    private void migrateCompositeActivity(CompositeActivity newCompAct, TransactionKey transactionKey) throws InvalidDataException {
        log.info("migrateCompositeActivity() - {}", newCompAct.getPath());

        for (GraphableVertex newChildV : newCompAct.getChildren()) {
            if (newChildV instanceof CompositeActivity) {
                CompositeActivity newChildCompAct = (CompositeActivity)newChildV;

                migrateCompositeActivity(newChildCompAct, transactionKey);
                migrateActivityState(newChildCompAct, transactionKey);
            }
            else if (newChildV instanceof Activity) {
                migrateActivityState((Activity)newChildV, transactionKey);
            }
            else {
                log.debug("migrateCompositeActivity() - SKIPPING {}  {}", newChildV.getPath(), newChildV.getClass().getSimpleName());
            }
        }
    }
}
