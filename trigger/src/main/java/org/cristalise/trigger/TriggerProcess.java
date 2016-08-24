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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.C2KLocalObject;
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
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

public class TriggerProcess extends StandardClient implements ProxyObserver<Job> {

    private final ArrayList<Integer> transitions = new ArrayList<Integer>();
    private final HashMap<String, C2KLocalObject> jobs = new HashMap<String, C2KLocalObject>();
    
    private Scheduler quartzScheduler = null;

//    private static boolean active = true;



    public TriggerProcess() 
            throws MarshalException, ValidationException, ObjectNotFoundException, IOException, MappingException
    {
        String stateMachinePath = Gateway.getProperties().getString("Trigger.StateMachine", "boot/SM/Trigger.xml");
        StateMachine sm = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, stateMachinePath));

        String[] transNames = Gateway.getProperties().getString("Trigger.Transitions", "Timeout").split(",");

        for(String transName: transNames) {
            int transID = sm.getTransitionID(transName);

            if(transID == -1) new ObjectNotFoundException("StateMachine '" + sm.getName() + "' does NOT have '"+transName+"' transition");

            transitions.add(transID);
        }

        Logger.msg(5, "TriggerProcess() - StateMachine:" + sm.getName() + "transitions:" + Arrays.toString(transNames));
//        Logger.msg(0, "TriggerProcess() - " + getDesc() + " initialised for " + agentName);
    }

    public void executeActions(Job job) throws Exception {
        // default implementation - the agent will execute any scripts defined when we execute
        agent.execute(job);
    }


    public void startScheduler() throws SchedulerException {
        SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();

        quartzScheduler = schedFact.getScheduler();
        quartzScheduler.start();
    }
    
    public void shutdownScheduler() throws SchedulerException {
        quartzScheduler.shutdown();
    }

    /**
     * Gets the next possible Job based on the Transitions
     * 
     * @return the actual Job
     */
    protected Job getActualJob() {
        Job thisJob = null;

        synchronized (jobs) {
            if (jobs.size() > 0) {
                
                for(int transID: transitions) {
                    thisJob = getJob(jobs, transID);

                    if (thisJob != null) break;
                }

                if (thisJob == null) {
                    Logger.warning("No supported jobs, but joblist is not empty! Discarding remaining jobs");
                    jobs.clear();
                }
                else
                    jobs.remove(ClusterStorage.getPath(thisJob));
            }
        }
        return thisJob;
    }

	private static Job getJob(HashMap<String, C2KLocalObject> jobs, int transition) {
        for (C2KLocalObject c2kLocalObject : jobs.values()) {
            Job thisJob = (Job)c2kLocalObject;
            if (thisJob.getTransition().getId() == transition) {
                Logger.msg(1,"=================================================================");
                Logger.msg(1, "Got "+thisJob.getTransition().getName()+" job for "+thisJob.getStepName()+" in "+thisJob.getItemPath());
                return thisJob;
            }
        }
        return null;
    }

    /**
     * Receives job from the AgentProxy. Reactivates thread if sleeping.
    */
    @Override
	public void add(Job contents) {
            synchronized(jobs) {
                Logger.msg(7, "TriggerProcess.add() - id:"+ClusterStorage.getPath(contents));

                jobs.put(ClusterStorage.getPath(contents), contents);
                jobs.notify();
            }
    }

    @Override
    public void control(String control, String msg) {
        if (MemberSubscription.ERROR.equals(control))
            Logger.error("Error in job subscription: "+msg);
    }

    /**
    * Job removal notification from the AgentProxy.
    */
    @Override
	public void remove(String id) {
        synchronized(jobs) {
            Logger.msg(7, "TriggerProcess.remove() - id:"+id);
            jobs.remove(id);
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
