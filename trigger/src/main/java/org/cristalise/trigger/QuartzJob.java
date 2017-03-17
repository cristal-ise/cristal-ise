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

import javax.xml.xpath.XPathExpressionException;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.utils.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import lombok.Setter;

/**
 * Default implementation of Job handling
 */
@Setter
public class QuartzJob implements org.quartz.Job {

    private Job cristalJob;
    private AgentProxy cristalAgent;

    /**
     * 
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();

        Logger.msg(5, "==================================================================");
        Logger.msg(5, "QuartzJob.execute() - JobKey:"+key);

        context.getMergedJobDataMap();

        try {
            setOutcome();
            cristalAgent.execute(cristalJob);
        }
        catch (Exception ex) {
            Logger.error(ex);
            //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
            throw new JobExecutionException(ex);
        }
    }

    /**
     * 
     * @param key
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     * @throws XPathExpressionException
     */
    public void setOutcome() throws InvalidDataException, ObjectNotFoundException, XPathExpressionException {
        Outcome o = cristalJob.getOutcome();
        String transName = cristalJob.getTransition().getName();

        if(transName.equals("Timeout")) {
            o.setFieldByXPath("/Timeout/Actions", "Executing key:"+cristalJob.getId());
        }
        else if(transName.equals("Warning")) {
            o.setFieldByXPath("/Warning/Actions", "Executing key:"+cristalJob.getId());
        }

        cristalJob.setOutcome(o);
    }
}
