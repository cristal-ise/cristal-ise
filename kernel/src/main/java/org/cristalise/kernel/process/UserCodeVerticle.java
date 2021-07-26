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
package org.cristalise.kernel.process;

import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.process.StandardClient.getRequiredStateMachine;
import static org.cristalise.kernel.process.UserCodeProcess.getAgentName;
import static org.cristalise.kernel.process.UserCodeProcess.getAgentPassword;
import static org.cristalise.kernel.process.UserCodeProcess.getRoleName;

import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;

/**
 * Provides a very basic automatic execution of Scripts associated with the Jobs (Activities).
 * It listens to the proxyMessage channel for cluster storage updates of Jobs of the UseCode Agent.
 * The processing of ProxyMessages assumes that a single message contains Jobs of the same Item.
 * <p>
 * Execution is based on the Default StateMachine, and it implements the following sequence:
 * <pre>
 * 1. assessStartConditions()
 * 2. start()
 * 3. complete()
 * 4. in case of error/exception during complete() execute error transition (e.g. Suspend for default StateMachine)
 */
@Slf4j
public class UserCodeVerticle extends AbstractVerticle {

    private AgentProxy userCode;

    private final int START;
    private final int COMPLETE;
    private final int ERROR;

    /**
     * Defines the name of the CRISTAL Property (value:{@value}) to override the default mapping for Start transition.
     * It is always prefixed like this: eg: UserCode.StateMachine.startTransition
     */
    public static final String STATE_MACHINE_START_TRANSITION = "StateMachine.startTransition";
    /**
     * Defines the name of the CRISTAL Property (value:{@value}) to override the default mapping for Complete transition.
     * It is always prefixed like this: eg: UserCode.StateMachine.completeTransition
     */
    public static final String STATE_MACHINE_COMPLETE_TRANSITION = "StateMachine.completeTransition";
    /**
     * Defines the name of the CRISTAL Property (value:{@value}) to override the default mapping for Error transition.
     * It is always prefixed like this: eg: UserCode.StateMachine.errorTransition
     */
    public static final String STATE_MACHINE_ERROR_TRANSITION = "StateMachine.errorTransition";
    /**
     * Defines the value (value:{@value}) to to be used in CRISTAL Property to ignore the Jobs of that Transition
     * eg: UserCode.StateMachine.resumeTransition = USERCODE_IGNORE
     */
    public static final String USERCODE_IGNORE = "USERCODE_IGNORE";

    /**
     * Constructor set up the user code
     * 
     * @throws InvalidDataException
     * @throws ObjectNotFoundException
     */
    public UserCodeVerticle() throws InvalidDataException, ObjectNotFoundException {

        StateMachine sm = getRequiredStateMachine(getRoleName(), null, "boot/SM/Default.xml");
        String propPrefix = getRoleName();

        //default values are valid for Transitions compatible with kernel provided Default StateMachine
        START    = getValidTransitionID(sm, propPrefix+"."+STATE_MACHINE_START_TRANSITION,    "Start");
        ERROR    = getValidTransitionID(sm, propPrefix+"."+STATE_MACHINE_ERROR_TRANSITION,    "Suspend");
        COMPLETE = getValidTransitionID(sm, propPrefix+"."+STATE_MACHINE_COMPLETE_TRANSITION, "Complete");
    }

    /**
     *
     * @param sm
     * @param propertyName
     * @param defaultValue
     * @return
     * @throws InvalidDataException
     */
    private int getValidTransitionID(StateMachine sm, String propertyName, String defaultValue) throws InvalidDataException {
        String propertyValue = Gateway.getProperties().getString(propertyName, defaultValue);

        if(USERCODE_IGNORE.equals(propertyValue)) return -1;
        else                                      return sm.getValidTransitionID(propertyValue);
    }

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        userCode = Gateway.getSecurityManager().authenticate(getAgentName(), getAgentPassword(), null);

        EventBus eb = vertx.eventBus();

        eb.localConsumer(userCode.getPath().getUUID() + "/" + JOB, (message) -> {
            String[] tokens = ((String) message.body()).split(":");
            String jobId = tokens[0];

            if (tokens[1].equals("DELETE")) return;

            try {
                Job aJob = userCode.getJob(jobId);
                vertx.executeBlocking((result) -> process(aJob));
            }
            catch (ObjectNotFoundException e) {
                log.error("handler()", e);
            }
        });

        startPromise.complete();
        log.info("start() - deployed '{}' consumer", ProxyMessage.ebAddress);
    }

    /**
     * 
     * @param thisJob
     * @param errorJob
     */
    protected void process(Job thisJob) {
        log.info("=======================================================================================");

        try {
            int transitionId = thisJob.getTransition().getId();

            if (transitionId == START)         start(thisJob);
            else if (transitionId == COMPLETE) complete(thisJob, null); //FIXME: ERROR Job needs to be retrieved
            else if (transitionId == ERROR)    log.trace("process() - skipping ERROR job:{}", thisJob); 
            else                               log.trace("process() - skipping job:{}", thisJob);
        }
        catch (InvalidTransitionException ex) {
            // must have already been done by someone else - ignore
            log.debug("process() - job was already executed - {}", thisJob);
        }
        catch (Exception ex) {
            log.error("Error executing job:{}", thisJob, ex);
        }
    }

    /**
     * Method called to handle the Start transition. Override this method to implement application specific action
     * for Jobs of Start Transition.
     *
     * @param thisJob the actual Job to be executed.
     */
    public void start(Job thisJob) throws CriseVertxException {
        log.debug("start() - job:"+thisJob);

        if (assessStartConditions(thisJob)) {
            log.trace("start() - Attempting to start");
            userCode.execute(thisJob);
        }
        else {
            log.debug("start() - Start conditions failed "+thisJob.getStepName()+" in "+thisJob.getItemPath());
        }
    }

    /**
     * Method called to handle the Complete transition. Override this method to implement application specific action
     * for Jobs of Complete Transition.
     *
     * @param thisJob the actual Job to be executed.
     * @param erroJob the error Job to be executed in case of error
     */
    public void complete(Job thisJob, Job erroJob) throws Exception {
        log.debug("complete() - job:"+thisJob);

        runUserCodeLogic(thisJob, erroJob);
    }

    /**
     * Override this method to implement application specific evaluation of start condition.
     * Default implementation - returns always true, i.e. there were no start conditions.
     *
     * @param job the actual Job to be executed.
     * @return true, if the start condition were met
     */
    public boolean assessStartConditions(Job job) {
        return true;
    }

    /**
     * Override this method to implement application specific (business) logic
     * Default implementation - the agent execute any scripts, query or both defined
     *
     * @param job the actual Job to be executed.
     * @param errorJob Job to be executed in case of an error
     */
    public void runUserCodeLogic(Job job, Job errorJob) throws CriseVertxException {
        if (errorJob == null) userCode.execute(job);
        else                  userCode.execute(job, errorJob);
    }
}
