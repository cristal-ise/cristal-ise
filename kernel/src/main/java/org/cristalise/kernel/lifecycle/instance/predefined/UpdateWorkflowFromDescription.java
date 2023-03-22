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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.GraphableVertex;
import org.cristalise.kernel.lifecycle.CompositeActivityDef;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.WfVertex;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;

import lombok.val;
import lombok.extern.slf4j.Slf4j;


/**
 * {@value #description}
 * <pre>
 * 1. read current job and consolidate it with field StepPathToEnable to know which activity needs to be enabled
 * 2. scan ne workflow and set state for each Activity 
 * 
 * x. Recalculate jobs and store them
 * </pre>
 * 
 */
@Slf4j
public class UpdateWorkflowFromDescription extends PredefinedStep {

    public static final String description = "Updates the Workflow of the Item from its description.";

    private List<String> actsToEnable = null;

    public UpdateWorkflowFromDescription() {
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "WorkflowMigrationData");
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

//        String descType = Gateway.getProxy(itemP).getType();
//        actsToEnable = Arrays.asList(Gateway.getProperties().getString(descType+".Activity", "").split(","));

        migrateWorkflow(itemP, getWf(), (CompositeActivityDef)null/*newWfDef*/, transactionKey);

        return requestData;
    }

    private WfVertex migrateWorkflow(ItemPath ip, Workflow oldWf, CompositeActivityDef newWfDef, TransactionKey transactionKey) 
            throws InvalidDataException, ObjectNotFoundException
    {
        CompositeActivity newDomain = (CompositeActivity)newWfDef.instantiate(newWfDef.getName(), transactionKey);
        CompositeActivity oldDomain = (CompositeActivity)oldWf.search("workflow/domain");

        newDomain.setState(oldDomain.getState());
        newDomain.active = oldDomain.active;

        return migrateWorkflow(ip, newDomain, oldDomain, transactionKey);
    }

    private void migrateActivityState(Activity newAct) throws InvalidDataException {
        log.info("migrateActivityState() - {}", newAct.getPath());
        Activity currentAct = (Activity) getWf().search(newAct.getPath());
        
        if (currentAct != null) {
            newAct.setState(currentAct.getState());
            newAct.active = currentAct.active;
        }
    }

    private void migrateCompositeActivity(CompositeActivity newCompAct) throws InvalidDataException {
        log.info("migrateCompositeActivity() - {}", newCompAct.getPath());

        for (GraphableVertex newChildV : newCompAct.getChildren()) {
            if (newChildV instanceof CompositeActivity) {
                CompositeActivity newChildCompAct = (CompositeActivity)newChildV;

                migrateCompositeActivity(newChildCompAct);
                migrateActivityState(newChildCompAct);
            }
            else if (newChildV instanceof Activity) {
                migrateActivityState((Activity)newChildV);
            }
            else {
                log.debug("migrateCompositeActivity() - SKIPPING {}  {}", newChildV.getPath(), newChildV.getClass().getSimpleName());
            }
        }
    }

    private WfVertex migrateWorkflow(ItemPath ip, CompositeActivity newDomain, CompositeActivity oldDomain, TransactionKey transactionKey) throws InvalidDataException {
        for (GraphableVertex newChildV : newDomain.getChildren()) {
            if (newChildV instanceof CompositeActivity) {
                CompositeActivity newCompAct = (CompositeActivity)newChildV;
                Activity searchedAct = (Activity) oldDomain.search(newCompAct.getName());

                if (searchedAct != null && searchedAct instanceof CompositeActivity) {
                    CompositeActivity oldCompAct = (CompositeActivity) searchedAct;

                    newCompAct.setState(oldCompAct.getState());
                    newCompAct.active = oldCompAct.active;

                    migrateWorkflow(ip, newCompAct, oldCompAct, transactionKey);
                }
                else {
                    if (actsToEnable.contains(newCompAct.getPath())) {
                        newCompAct.setState(1);
                        newCompAct.active = true;
                    }
                    else {
                        newCompAct.setState(0);
                        newCompAct.active = false;
                    }

                    migrateWorkflow(ip, newCompAct, null, transactionKey);
                }
            }
            else if (newChildV instanceof Activity) {
                updateNewAct(oldDomain, (Activity)newChildV);
            }
        }

        return newDomain;
    }

    private void updateNewAct(CompositeActivity oldDomain, Activity newAct) {
        Activity searchedAct = (Activity)oldDomain.search(newAct.getName());
//        val searchedAct = oldDomain != null && oldDomain.getCh ? oldDomain.children.find { it.name == newAct.name } : null;

        if (searchedAct != null && searchedAct instanceof Activity) {
            val oldAct = ( Activity ) searchedAct;
//            newAct.state  = oldAct.stateMachine.name == "Default" &&
//                    newAct.stateMachine.name == "HmwsSM" ? (oldAct.state == 2 ? 1 : 0) : oldAct.state; // This convert the old state to new state depending on the stateMachine
            newAct.active = oldAct.active;
        }
        else {
            newAct.setState(0);
            newAct.active = actsToEnable.contains(newAct.getPath());
        }
    }

    private List<String> getAvailableActNames(ItemProxy item, TransactionKey transactionKey) {
        C2KLocalObjectMap<Job> jobList = item.getJobs(transactionKey);
        List<String> result = new ArrayList<>();

          for (Job job : jobList.values()) {
              if (job.getTransition().getId() == 0) result.add(job.getStepName());
          }
          Collections.sort(result);
          return result;
    }

/*
*/
}
