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

import java.util.ArrayList;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.graph.model.GraphPoint;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReplaceDomainWorkflow extends PredefinedStep {

    public static final String description = "Replaces the domain CA with the supplied one.";

    public ReplaceDomainWorkflow() {
        super("WorkflowReplaceData", description);
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey) 
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        Outcome workflowReplaceData = new Outcome(requestData);

        log.debug("Called by {} on {}", agent.getAgentName(transactionKey), item);

        Workflow currentWf = getWf();

        String oldDomainCAXml = Gateway.getMarshaller().marshall(currentWf.search("workflow/domain"));

        try {
            String xml = Outcome.serialize(workflowReplaceData.getNodeByXPath("//NewWorkflowXml/CompositeActivity"), false);
            CompositeActivity newDomainCA = (CompositeActivity) Gateway.getMarshaller().unmarshall(xml);

            replaceDomainWorkflow(agent, item, currentWf, newDomainCA, transactionKey);

            workflowReplaceData.appendXmlFragment("//OldWorkflowXml", oldDomainCAXml);
        }
        catch (XPathExpressionException e) {
            throw new InvalidDataException(e);
        }

        return workflowReplaceData.getData(true);
    }

    public static void replaceDomainWorkflow(AgentPath agent, ItemPath item, Workflow currentWf, CompositeActivity newDomainCA, TransactionKey transactionKey)
            throws InvalidDataException, PersistencyException, ObjectNotFoundException
    {
        newDomainCA.setName("domain");
        
        // replace 'workflow/domain' in currentWf with newDomainCA
        currentWf.getChildrenGraphModel().removeVertex(currentWf.search("workflow/domain"));
        currentWf.initChild(newDomainCA, true, new GraphPoint(150, 100));

        // if new workflow, activate it, otherwise refresh the jobs
        if (!newDomainCA.active) currentWf.run(agent, item, transactionKey);

        // store new wf
        Gateway.getStorage().put(item, currentWf, transactionKey);

        // replace Jobs with the new ones
        Gateway.getStorage().removeCluster(item, ClusterType.JOB, transactionKey);

        ArrayList<Job> newJobs = ((CompositeActivity)currentWf.search("workflow/domain")).calculateJobs(agent, item, true);
        for (Job newJob: newJobs) {
            Gateway.getStorage().put(item, newJob, transactionKey);

            if (StringUtils.isNotBlank(newJob.getRoleOverride())) newJob.sendToRoleChannel();
        }
    }
}
