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

import static org.cristalise.kernel.SystemProperties.$UserCodeRole_StateMachine_completeTransition;
import static org.cristalise.kernel.SystemProperties.$UserCodeRole_StateMachine_errorTransition;
import static org.cristalise.kernel.SystemProperties.$UserCodeRole_StateMachine_startTransition;
import static org.cristalise.kernel.persistency.ClusterType.JOB;
import static org.cristalise.kernel.process.StandardClient.getRequiredStateMachine;
import static org.cristalise.kernel.process.UserCodeProcess.getAgentName;
import static org.cristalise.kernel.process.UserCodeProcess.getAgentPassword;
import static org.cristalise.kernel.process.UserCodeProcess.getRoleName;

import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ProxyMessage;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
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
public class UserCodeVerticle extends VerticleBase {

    private AgentProxy userCode;
    private MessageConsumer<String> jobConsumer;

    private final int START;
    private final int COMPLETE;
    private final int ERROR;

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
    public UserCodeVerticle() throws InvalidDataException {
        StateMachine sm = getRequiredStateMachine(getRoleName(), null, "boot/SM/Default.xml");

        //default values are valid for Transitions compatible with kernel provided Default StateMachine
        START    = getValidTransitionID(sm, $UserCodeRole_StateMachine_startTransition.getString(null, getRoleName()));
        ERROR    = getValidTransitionID(sm, $UserCodeRole_StateMachine_errorTransition.getString(null, getRoleName()));
        COMPLETE = getValidTransitionID(sm, $UserCodeRole_StateMachine_completeTransition.getString(null, getRoleName()));
    }

    private int getValidTransitionID(StateMachine sm, String propertyValue) throws InvalidDataException {
        if(USERCODE_IGNORE.equals(propertyValue)) return -1;
        else                                      return sm.getValidTransitionID(propertyValue);
    }

    @Override
    public Future<?> start() throws Exception {
        userCode = Gateway.getSecurityManager().authenticate(getAgentName(), getAgentPassword(), null);

        EventBus eb = vertx.eventBus();

        jobConsumer = eb.localConsumer(userCode.getPath().getUUID() + "/" + JOB, (message) -> {
            String[] tokens = ((String) message.body()).split(":");
            String jobId = tokens[0];

            if (tokens[1].equals("DELETE")) return;

            try {
                Job aJob = userCode.getJob(jobId);
                vertx.executeBlocking(() -> {
                    process(aJob);
                    return null;
                });
            }
            catch (ObjectNotFoundException e) {
                log.error("handler()", e);
            }
        });

        log.info("start() - deployed '{}' consumer", ProxyMessage.ebAddress);
        return jobConsumer.completion();
    }

    @Override
    public Future<?> stop() {
        log.info("start() - undeployed '{}' consumer", ProxyMessage.ebAddress);

        if (jobConsumer != null) return jobConsumer.unregister();
        else                     return Future.succeededFuture();
    }

    protected void process(Job thisJob) {
        log.info("=======================================================================================");

        try {
            int transitionId = thisJob.getTransition().getId();

            if (transitionId == START)         startJob(thisJob);
            else if (transitionId == COMPLETE) completeJob(thisJob, null); //FIXME: ERROR Job needs to be retrieved
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
    public void startJob(Job thisJob) throws CriseVertxException {
        log.debug("startJob() - job:{}", thisJob);

        if (assessStartConditions(thisJob)) {
            log.trace("startJob() - Attempting to start");
            userCode.execute(thisJob);
        }
        else {
            log.debug("startJob() - Start conditions failed {} in {}", thisJob.getStepName(), thisJob.getItemPath());
        }
    }

    /**
     * Method called to handle the Complete transition. Override this method to implement application-specific action
     * for Jobs of Complete Transition.
     *
     * @param thisJob the actual Job to be executed.
     * @param erroJob the error Job to be executed in case of error
     */
    public void completeJob(Job thisJob, Job erroJob) throws Exception {
        log.debug("completeJob() - job:{}", thisJob);

        runUserCodeLogic(thisJob, erroJob);
    }

    /**
     * Override this method to implement application specific evaluation of start condition.
     * Default implementation - always returns true, i.e. there were no start conditions.
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
