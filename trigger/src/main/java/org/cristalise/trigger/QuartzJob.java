/**
 * This file is part of the CRISTAL-iSE Trigger module.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.trigger;

import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of Job handling
 */
@Setter
@DisallowConcurrentExecution
@Slf4j
public class QuartzJob implements org.quartz.Job {

    private Job cristalJob;
    private AgentProxy cristalAgent;

    /**
     *
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();

        log.debug("execute() - JobKey:{}", key);

        context.getMergedJobDataMap();

        try {
            cristalAgent.execute(cristalJob);
        }
        catch (Exception ex) {
            log.error("", ex);
            //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
            throw new JobExecutionException(ex);
        }
    }
}
