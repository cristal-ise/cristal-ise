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

import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.process.AbstractMain;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.StandardClient;
import org.quartz.DateBuilder;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class TriggerProcess extends StandardClient {

    private final ArrayList<String> transitions = new ArrayList<String>();

    private Scheduler quartzScheduler = null;

    /**
     *
     * @throws InvalidDataException Invalid data
     */
    public TriggerProcess() throws InvalidDataException {
        StateMachine sm = getRequiredStateMachine("Trigger", "trigger", "boot/SM/Trigger.xml");

        String[] transNames = Gateway.getProperties().getString("Trigger.StateMachine.transitions", "Warning,Timeout").split(",");

        for(String transName: transNames) {
            sm.getValidTransitionID(transName); //checks if the trans name is correct or not
            transitions.add(transName);
        }

        log.debug("StateMachine:{} transitions:{}", sm.getName(), (Object)transNames);
    }

    /**
     *
     * @throws SchedulerException Scheduler error
     * @throws ObjectNotFoundException 
     */
    public void initialise() throws SchedulerException, ObjectNotFoundException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        quartzScheduler = schedFact.getScheduler();
        quartzScheduler.start();

        //Subscribe to changes and fetch exiting Jobs from JobList of Agent
        Vertx vertx = Gateway.getVertx();
        vertx.eventBus().localConsumer(agent.getPath().getUUID() + "/" + JOB, message -> {
            String[] tokens = ((String) message.body()).split(":");
            String jobId = tokens[0];

            vertx.executeBlocking(promise -> {
                try {
                    if (tokens[1].equals("ADD")) add(agent.getJob(jobId));
                    else                         remove(jobId);
                }
                catch (ObjectNotFoundException e) {
                    log.error("", e);
                }
                promise.complete();
            }, res -> {
                //
            });
        });

        Set<String> jobIds = new HashSet<>();//agent.getJobList().keySet();
        log.debug("initialise() - Retrieving #{} of Jobs.", jobIds.size());
        for (String id: jobIds) add(agent.getJob(id));
    }

    /**
     *
     * @throws SchedulerException Scheduler error
     */
    public void shutdownScheduler() throws SchedulerException {
        quartzScheduler.shutdown();
    }

    /**
     *
     * @param currentJob
     * @param jobID
     * @return JobDetail
     */
    protected JobDetail buildJobDetail(Job currentJob, String jobID) {
        JobDataMap jdm = new JobDataMap();

        jdm.put("CristalAgent", agent);
        jdm.put("CristalJob",   currentJob);

        return newJob(QuartzJob.class).withIdentity(jobID).usingJobData(jdm).build();
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

        log.debug("buildTriggersAndScehduleJob() - Scheduling job:"+jobID+" trans:"+transName+" Duration:"+duration+"["+unit+"]");

        try {
            SimpleTrigger trigger = (SimpleTrigger) newTrigger()
                    .withIdentity(jobID)
                    .startAt(DateBuilder.futureDate(duration, IntervalUnit.valueOf(unit.toUpperCase())))
                    .forJob(jobID)
                    .build();

            JobDetail jobDetail = buildJobDetail(currentJob, jobID);

            quartzScheduler.scheduleJob(jobDetail, trigger);

            log.debug("buildTriggersAndScehduleJob() - Scheduled job:"+jobID+" trans:"+transName+" Duration:"+duration+"["+unit+"]");
        }
        catch (Exception ex) {
            log.error("", ex);
            //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
        }
    }

    /**
     * Receives Job from the AgentProxy. Reactivates thread if sleeping.
     */
    public void add(Job currentJob) {
        String transName = currentJob.getTransition().getName();
        String jobID = "";//Integer.toString(currentJob.getId());

        Boolean enabled      = Gateway.getProperties().getBoolean("Trigger.enabled", true);
        Boolean transitionOn = (Boolean)currentJob.getActProp(transName+"On", true);

        synchronized(quartzScheduler) {
            if (transitions.contains(transName)) {
                if(enabled && transitionOn) {
                    buildTriggersAndScehduleJob(currentJob, jobID);
                }
                else {
                    log.debug("add() - disabled trans:"+transName+" job:"+jobID);
                    //TODO: Execute activity in the Workflow of the Agent to store this error and remove Job from list
                }
            }
            else {
                log.warn("add() - UKNOWN trans:"+transName+" job:"+jobID);
                //TODO: Execute activity in the Workflow of the Agent to store this error and probably remove Job from list
            }
        }
    }

    /**
     * Job removal notification from the AgentProxy.
     */
    public void remove(String id) {
        synchronized(quartzScheduler) {
            log.debug("remove() - id:"+id);
            try {
                quartzScheduler.deleteJob(new JobKey(id));
            }
            catch (Exception e) {
                log.error("", e);
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

            StandardClient.createClientVerticles();

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
            log.error("", ex);

            try {
                Gateway.close();
            }
            catch(Exception ex1) {
                log.error("", ex1);
            }

            System.exit(1);
        }
    }
}
