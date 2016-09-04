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
package org.cristalise.trigger;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.agent.JobList;
import org.cristalise.kernel.entity.proxy.MemberSubscription;
import org.cristalise.kernel.entity.proxy.ProxyObserver;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.persistency.ClusterStorage;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.StandardClient;
import org.cristalise.kernel.utils.Logger;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;

public class TriggerProcess extends StandardClient implements ProxyObserver<Job> {

    private final ArrayList<Integer> transitions = new ArrayList<Integer>();

    private Scheduler quartzScheduler = null;


    public TriggerProcess() throws MarshalException, ValidationException, ObjectNotFoundException, IOException, MappingException
    {
        String stateMachinePath = Gateway.getProperties().getString("Trigger.StateMachine", "boot/SM/Trigger.xml");
        StateMachine sm = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, stateMachinePath));

        String[] transNames = Gateway.getProperties().getString("Trigger.Transitions", "Timeout").split(",");

        for(String transName: transNames) {
            int transID = sm.getTransitionID(transName);

            if(transID == -1) new ObjectNotFoundException("StateMachine '" + sm.getName() + "' does NOT have '"+transName+"' transition");

            transitions.add(transID);
        }

        agent.subscribe(new MemberSubscription<Job>(this, ClusterStorage.JOB, true));

        Logger.msg(5, "TriggerProcess() - StateMachine:" + sm.getName() + "transitions:" + Arrays.toString(transNames));
    }

    public void executeActions(Job job) throws Exception {
        // default implementation - the agent will execute any scripts defined when we execute
        agent.execute(job);
    }


    public void startScheduler() throws SchedulerException, AccessRightsException, ObjectNotFoundException, PersistencyException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        quartzScheduler = schedFact.getScheduler();
        quartzScheduler.start();
        
        JobList joblist = (JobList)agent.getObject(ClusterStorage.JOB);

        for(String jobID: joblist.keySet()) add(joblist.get(jobID));
    }
    
    public void shutdownScheduler() throws SchedulerException {
        quartzScheduler.shutdown();
    }

    /**
     * Receives job from the AgentProxy. Reactivates thread if sleeping.
    */
    @Override
	public void add(Job currentJob) {
        synchronized(quartzScheduler) {
            Logger.msg(7, "TriggerProcess.add() - id:"+ClusterStorage.getPath(currentJob));

            if (transitions.contains(currentJob.getTransition().getId())) {
                JobDataMap jdm = new JobDataMap();
                jdm.put("CristalAgent", agent);
                jdm.put("CristalJob",   currentJob);

                String jobID = currentJob.getItemUUID()+"/"+currentJob.getId();

                JobDetail jobDetail = newJob(QuartzJob.class)
                        .withIdentity(jobID)
                        .usingJobData(jdm)
                        .build();

                buildTriggersAndScehduleJob(currentJob, jobID, jobDetail);
            }
            else {
                Logger.warning("TriggerProcess.add() - SKIPPING job name:"+currentJob.getName()+" trans:"+currentJob.getTransition().getName()); 
            }
        }
    }

    /**
     * 
     * 
     * @param currentJob
     * @param quartzJobId
     * @param jobDetail
     */
    protected void buildTriggersAndScehduleJob(Job currentJob, String quartzJobId, JobDetail jobDetail) {
        Integer duration = (Integer)currentJob.getActProp(currentJob.getTransition().getName()+"Duration");

        SimpleTrigger trigger = (SimpleTrigger) newTrigger()
                .withIdentity(quartzJobId)
                .startAt(DateBuilder.futureDate(duration, IntervalUnit.SECOND))
                .forJob(quartzJobId) 
                .build();
        
        try {
            quartzScheduler.scheduleJob(jobDetail, trigger);
        }
        catch (SchedulerException ex) {
            Logger.error(ex);
        }
    }

    @Override
    public void control(String control, String msg) {
        if (MemberSubscription.ERROR.equals(control)) Logger.error("Error in job subscription: "+msg);
    }

    /**
    * Job removal notification from the AgentProxy.
    */
    @Override
	public void remove(String id) {
        synchronized(quartzScheduler) {
            Logger.msg(7, "TriggerProcess.remove() - id:"+id);
            try {
                //JobKey key = new JobKey(currentJob.getItemUUID()+"/"+currentJob.getId());
                quartzScheduler.deleteJob(new JobKey(id));
            }
            catch (SchedulerException e) {
                Logger.error(e);
            }
        }
    }

    static public void main(String[] args) {
        try {
        	Gateway.init(readC2KArgs(args));
            TriggerProcess proc = new TriggerProcess();

            proc.login( "trigger",/*InetAddress.getLocalHost().getHostName()*/ 
                        "password", 
                        Gateway.getProperties().getString("AuthResource", "Cristal"));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    AbstractMain.shutdown(0);
                    //TODO: call quartzScheduler.shutdown() as well
                }
            });
        }
        catch( Exception ex ) {
            Logger.error(ex);

            try {
            	Gateway.close();
            }
            catch(Exception ex1) {
                Logger.error(ex1);
            }

            System.exit(1);
        }
    }

    public String getDesc() {
        return("Trigger Process");
    }

    public static void shutdown() {
//        active = false;
    }
}
