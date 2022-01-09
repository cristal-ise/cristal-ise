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
package org.cristalise.kernel.lifecycle.instance.predefined.agent;

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import java.io.IOException;
import java.util.List;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.agent.JobArrayList;
import org.cristalise.kernel.entity.agent.JobList;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStep;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Supplies Agent with the new set of jobs for the given Item and Activity. The Agent replaces all existing jobs 
 * for that activity with the given set.  Only activities that are assigned to a Role that is flagged 
 * to push Jobs do this
 * 
 * {@value #description}
 */
@Slf4j
public class RefreshJobListOfAgent extends PredefinedStep {

    public static final String description = "Updates the Agent's list of Jobs referencing one Activity of a single Item.";

    public RefreshJobListOfAgent() {
        super();
        this.setBuiltInProperty(SCHEMA_NAME, "JobArrayList");
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath itemPath, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException
    {
        log.debug("Called by {} on {}", agent.getAgentName(transactionKey), itemPath);

        AgentProxy item = (AgentProxy) Gateway.getProxy(itemPath, transactionKey);

        try {
            JobArrayList newJobs = (JobArrayList)Gateway.getMarshaller().unmarshall(requestData);

            JobList currentJobList = item.getJobList(transactionKey);

            //newJobs.list.get(0) can be used becuase the newJobs contains only the jobs for one Activity of a single Item
            List<String> idsToRemove = currentJobList.getJobIdsForStep(newJobs.list.get(0));

            // merge new jobs in first, so the RemoteMap.getLastId() used during addJob() returns the next unique id
            for (Job newJob : newJobs.list) {
                // FIXME: id = -999 is a hack to get the itemPath and stepPath to cleanup the actual list
//                if (newJob.getId() != -999) {
//                    log.trace("Adding job:{}", newJob);
//                    currentJobList.addJob(newJob);
//                }
            }

            log.trace("Removing old jobs:{}", idsToRemove);

            // remove old jobs for this item
            for(String key: idsToRemove) currentJobList.remove(key);

            return requestData;
        }
        catch (MarshalException | ValidationException | IOException | MappingException e) {
            log.error("Error marshalling Jobs", e);
            throw new InvalidDataException("Error marshalling Jobs", e);
        }
    }
}
