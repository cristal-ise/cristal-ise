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
package org.cristalise.kernel.entity.agent;

import static org.cristalise.kernel.persistency.ClusterType.JOB;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.TransactionKey;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JobList extends C2KLocalObjectMap<Job> implements C2KLocalObject {

    public JobList(AgentPath agentPath) {
        super(agentPath, JOB);
    }

    public JobList(AgentPath agentPath, TransactionKey transactionKey) {
        super(agentPath, JOB, transactionKey);
    }

    public synchronized void addJob(Job job) {
        int jobId = getLastId() + 1;
        job.setId(jobId);
        put(String.valueOf(jobId), job);
    }

    public Job get(int id) {
        return get(String.valueOf(id));
    }

    @Override
    public void setName(String name) {
        //DO nothing
    }

    @Override
    public String getName() {
        return getClusterType().getName();
    }

    @Override
    public String getClusterPath() {
        return getClusterType().getName();
    }

    /**
     * Find the list of JobKeys for the given Item and its Step
     * 
     * @param otherJob use this Job's itemPath and stepPath for search
     * @return the current list of Job keys matching the inputs
     */
    public List<String> getKeysForStep(Job otherJob) {
        if (otherJob == null) return new ArrayList<String>();
        return getKeysForStep(otherJob.getItemPath(), otherJob.getStepName(), null);
    }

    /**
     * Find the list of JobKeys for the given Item and its Step
     * 
     * @param itemPath for search
     * @param stepPath for search, can be null
     * @param transitionId for search, can be null
     * @return the current list of Job keys matching the inputs
     */
    public synchronized List<String> getKeysForStep(ItemPath itemPath, String stepPath, Integer transitionId) {
        List<String> jobKeys = new ArrayList<String>();

        log.debug("getKeysForStep() - item:{} step:{}", itemPath, stepPath);

        for (String jid: keySet()) {
            Job currentJob = get(jid);
            boolean addJob = false;

            if (currentJob.getItemPath().equals(itemPath)) {
                if (StringUtils.isBlank(stepPath)) {
                    addJob = true;
                }
                else if(currentJob.getStepPath().equals(stepPath)) {
                    if (transitionId == null) {
                        addJob = true;
                    }
                    else if (currentJob.getTransition().getId() == transitionId) {
                        addJob = true;
                    }
                }
            }
            if (addJob) {
                log.trace("getKeysForStep() - adding job:{}", currentJob);
                jobKeys.add(jid);
            }
        }
        return jobKeys;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("{");
        for (String jid:  keySet()) sb.append(get(jid).toString());
        sb.append("}");
        return sb.toString();
    }
}