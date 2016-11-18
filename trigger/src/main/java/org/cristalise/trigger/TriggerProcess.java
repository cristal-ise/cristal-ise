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

/**
 * 
 */
public class TriggerProcess extends StandardClient implements ProxyObserver<Job> {

    private final ArrayList<String> transitions = new ArrayList<String>();

    private Scheduler quartzScheduler = null;

    /**
     * 
     * @throws MarshalException
     * @throws ValidationException
     * @throws ObjectNotFoundException
     * @throws IOException
     * @throws MappingException
     */
    public TriggerProcess() throws MarshalException, ValidationException, ObjectNotFoundException, IOException, MappingException
    {
        String stateMachineNS   = Gateway.getProperties().getString("Trigger.StateMachine.namespace", "trigger");
        String stateMachinePath = Gateway.getProperties().getString("Trigger.StateMachine.bootfile",  "boot/SM/Trigger.xml");

        StateMachine sm = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(stateMachineNS, stateMachinePath));

        String[] transNames = Gateway.getProperties().getString("Trigger.StateMachine.transitions", "Warning,Timeout").split(",");

        for(String transName: transNames) {
            int transID = sm.getTransitionID(transName);

            if(transID == -1) throw new ObjectNotFoundException("StateMachine '" + sm.getName() + "' does NOT have '"+transName+"' transition");

            transitions.add(transName);
        }

        Logger.msg(5, "TriggerProcess() - StateMachine:" + sm.getName() + " transitions:" + Arrays.toString(transNames));
    }

    /**
     * 
     * @throws SchedulerException
     * @throws AccessRightsException
     * @throws ObjectNotFoundException
     * @throws PersistencyException
     */
    public void initialise() throws SchedulerException, AccessRightsException, ObjectNotFoundException, PersistencyException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        quartzScheduler = schedFact.getScheduler();
        quartzScheduler.start();

        Logger.msg(5, "TriggerProcess.startScheduler() - Retrieving initial list of Jobs.");

        //Subscribe to changes and fetch exiting Jobs from JobList of Agent
        agent.subscribe(new MemberSubscription<Job>(this, ClusterStorage.JOB, true));
    }

    /**
     * 
     * @throws SchedulerException
     */
    public void shutdownScheduler() throws SchedulerException {
        quartzScheduler.shutdown();
    }

    /**
     * 
     * @param currentJob
     * @param jobID
     * @return
     */
    protected JobDetail buildJobDetail(Job currentJob, String jobID) {
        JobDataMap jdm = new JobDataMap();

        jdm.put("CristalAgent", agent);
        jdm.put("CristalJob",   currentJob);

        return newJob(QuartzJob.class)
                .withIdentity(jobID)
                .usingJobData(jdm)
                .build();
    }

    /**
     * 
     * 
     * @param currentJob
     * @param jobID
     */
    protected void buildTriggersAndScehduleJob(Job currentJob, String jobID) {
        String transName = currentJob.getTransition().getName();

        Integer duration = (Integer)currentJob.getActProp(transName+"Duration");
        String  unit     = (String) currentJob.getActProp(transName+"Unit");

        Logger.msg(5, "TriggerProcess.buildTriggersAndScehduleJob() - Scheduling job:"+jobID+" trans:"+transName+" Duration:"+duration+"["+unit+"]");

        try {
            SimpleTrigger trigger = (SimpleTrigger) newTrigger()
                    .withIdentity(jobID)
                    .startAt(DateBuilder.futureDate(duration, IntervalUnit.valueOf(unit.toUpperCase())))
                    .forJob(jobID) 
                    .build();

            JobDetail jobDetail = buildJobDetail(currentJob, jobID);

            quartzScheduler.scheduleJob(jobDetail, trigger);

            Logger.msg(7, "TriggerProcess.buildTriggersAndScehduleJob() - Scheduled job:"+jobID+" trans:"+transName+" Duration:"+duration+"["+unit+"]");
        }
        catch (Exception ex) {
            Logger.error(ex);
            //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
        }
    }

    /**
     * Receives Job from the AgentProxy. Reactivates thread if sleeping.
     */
    @Override
	public void add(Job currentJob) {
        String transName = currentJob.getTransition().getName();
        String jobID = Integer.toString(currentJob.getId());

        Boolean enabled      = Gateway.getProperties().getBoolean("Trigger.enabled", true);
        Boolean transitionOn = (Boolean)currentJob.getActProp(transName+"On", true);

        synchronized(quartzScheduler) {
            if (transitions.contains(transName)) {
                if(enabled && transitionOn) {
                    buildTriggersAndScehduleJob(currentJob, jobID);
                }
                else {
                    Logger.msg(7, "TriggerProcess.add() - disabled trans:"+transName+" job:"+jobID); 
                    //TODO: Execute activity in the Workflow of the Agent to store this error and remove Job from list
                }
            }
            else {
                Logger.warning("TriggerProcess.add() - UKNOWN trans:"+transName+" job:"+jobID); 
                //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
            }
        }
    }

    /**
     * Job control messages, could be errors as well
     */
    @Override
    public void control(String control, String msg) {
        if (MemberSubscription.ERROR.equals(control)) { 
            Logger.error("Error in job subscription: "+msg);
            //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
        }
    }

    /**
    * Job removal notification from the AgentProxy.
    */
    @Override
	public void remove(String id) {
        synchronized(quartzScheduler) {
            Logger.msg(7, "TriggerProcess.remove() - id:"+id);
            try {
                quartzScheduler.deleteJob(new JobKey(id));
            }
            catch (Exception e) {
                Logger.error(e);
                //TODO: Execute activity in the Workflow of the Agent to report this error
            }
        }
    }

    static public void main(String[] args) {
        try {
            Gateway.init(readC2KArgs(args));
            TriggerProcess proc = new TriggerProcess();

            proc.login( Gateway.getProperties().getString("Trigger.agent", "triggerAgent"),
                        Gateway.getProperties().getString("Trigger.password"),
                        Gateway.getProperties().getString("AuthResource", "Cristal"));

            proc.initialise();

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
