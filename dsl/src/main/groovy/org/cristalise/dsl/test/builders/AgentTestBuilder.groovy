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
package org.cristalise.dsl.test.builders

import groovy.transform.CompileStatic

import java.util.ArrayList

import org.cristalise.dsl.entity.AgentBuilder;
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.agent.JobList
import org.cristalise.kernel.entity.imports.ImportAgent;
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy
import org.cristalise.kernel.entity.proxy.MemberSubscription
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.ClusterStorage
import org.cristalise.kernel.persistency.ClusterType
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.Gateway


/**
 *
 */
@CompileStatic
class AgentTestBuilder extends AgentBuilder {
    AgentPath builderAgent

    ImportAgent newAgent
    AgentPath   newAgentPath
    AgentProxy  newAgentProxy

    JobList jobList = null

    public AgentTestBuilder() {}

    public AgentTestBuilder(ImportAgent agent) {
        newAgent = agent
        initBuilderAgent()
    }

    public AgentTestBuilder(AgentPath path) {
        newAgentPath = path
//        initBuilderAgent()
        setupJobList()
    }

    public void initBuilderAgent() {
        builderAgent = new AgentPath(new ItemPath(), "AgentTestBuilder")
        if(!Gateway.getLookupManager().exists(builderAgent)) Gateway.getLookupManager().add(builderAgent)
    }

    public void setupJobList() {
        if(!newAgentProxy) newAgentProxy = Gateway.proxyManager.getAgentProxy(newAgentPath)

        jobList = (JobList)newAgentProxy.getObject(ClusterType.JOB)
        jobList.activate()
    }

    public static AgentTestBuilder create(Map<String, Object> attrs, Closure cl) {
        def atb = new AgentTestBuilder(AgentBuilder.build(attrs, cl))

        atb.newAgentPath = atb.create(atb.builderAgent, atb.newAgent)
        atb.setupJobList()

        return atb
    }

    public static AgentTestBuilder build(Map<String, Object> attrs, Closure cl) {
        return new AgentTestBuilder(AgentBuilder.build(attrs, cl))
    }

    public void checkJobList(List<Map<String, Object>> expectedJobs) {
        jobList.dump(0);

        //check if both list are empty or null
        if(!expectedJobs && !jobList) return 

        assert expectedJobs && jobList && jobList.size() == expectedJobs.size()

        expectedJobs.each { Map jobMap -> 
            assert jobMap && jobMap.stepName && jobMap.agentRole && jobMap.transitionName
//            assert jobMap && jobMap.stepName && jobMap.agentRole != null && jobMap.transitionName
            
            assert jobList.values().find {
                ((Job) it).stepName == jobMap.stepName && 
                ((Job) it).agentRole == jobMap.agentRole &&
                ((Job) it).transition.name == jobMap.transitionName
            }, "Cannot find Job: ${jobMap.stepName} , ${jobMap.agentRole} , ${jobMap.transitionName}"
        }
    }

    public String executeJob(ItemPath itemPath, String actName, String transName, Outcome outcome = null) {
        return executeJob(Gateway.getProxyManager().getProxy(itemPath), actName, transName, outcome)
    }

    public String executeJob(ItemProxy itemProxy, String actName, String transName, Outcome outcome = null) {
        if(!newAgentProxy) newAgentProxy = Gateway.getProxyManager().getAgentProxy(newAgentPath)

        Job j = itemProxy.getJobByTransitionName(actName, transName, newAgentPath)
        if (outcome) j.setOutcome(outcome)
        return newAgentProxy.execute(j)
    }

    public ArrayList<Job> getJobs(ItemPath itemPath) {
        return Gateway.getProxyManager().getProxy(itemPath).getJobList(newAgentProxy)
    }
}
