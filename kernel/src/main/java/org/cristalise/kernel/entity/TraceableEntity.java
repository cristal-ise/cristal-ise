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
package org.cristalise.kernel.entity;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.cristalise.kernel.SystemProperties.StateMachine_enableErrorHandling;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.ItemPredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.C2KLocalObjectMap;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.security.SecurityManager;

import io.vertx.core.Future;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;
import lombok.extern.slf4j.Slf4j;

/**
 * TraceableEntity is the implementation of the vertx service interface
 *
 * <pre>
 * Traditional Pinky/Brain ASCII art:
 * 
 *                                ,.   '\'\    ,---.
 *                               | \\  l\\l_ //    |
 *        _              _       |  \\/ `/  `.|    |
 *      /~\\   \        //~\     | Y |   |   ||  Y |
 *      |  \\   \      //  |     |  \|   |   |\ /  |
 *      [   ||        ||   ]     \   |  o|o  | >  /
 *     ] Y  ||        ||  Y [     \___\_--_ /_/__/
 *     |  \_|l,------.l|_/  |     /.-\(____) /--.\
 *     |   >'          `<   |     `--(______)----'
 *     \  (/~`--____--'~\)  /         u// u / \
 *      `-_>-__________-<_-'          / \  / /|
 *          /(_#(__)#_)\             ( .) / / ]
 *          \___/__\___/              `.`' /   [
 *           /__`--'__\                |`-'    |
 *        /\(__,>-~~ __)               |       |_
 *     /\//\\(  `--~~ )               _l       |-:.
 *     '\/  <^\      /^>             |  `   (  <  \\
 *          _\ >-__-< /_           ,-\  ,-~~->. \  `:._,/
 *        (___\    /___)         (____/    (____)   `-'
 *             Kovax            and, paradoxically, Kovax
 * </pre>
 */
@Slf4j
public class TraceableEntity implements Item {

    protected final ClusterStorageManager mStorage;

    public TraceableEntity() {
        this.mStorage = Gateway.getStorage();
    }

    protected PredefinedStepContainer getNewPredefStepContainer() {
        return new ItemPredefinedStepContainer();
    }

    @Override
    public Future<String> requestAction(
            String itemUuid,
            String agentUuid,
            String stepPath,
            int transitionID,
            String requestData,
            String fileName,
            List<Byte> attachment)
    {
        ItemProxy item;
        AgentProxy agent;
        TransactionKey transactionKey;

        try {
            item  = Gateway.getProxy(Gateway.getLookup().getItemPath(itemUuid));
            agent = Gateway.getAgentProxy(Gateway.getLookup().getAgentPath(agentUuid));
            transactionKey = new TransactionKey(item.getPath());
        }
        catch (Throwable t) {
            log.error("requestAction()", t);
            return Future.failedFuture(CriseVertxException.toServiceException(t));
        }

        try {
            SharedData sharedData = Gateway.getVertx().sharedData();
            Lock lock = Future.await(sharedData.getLockWithTimeout(itemUuid, 5000));

            try {
                mStorage.begin(transactionKey);
                String finalOutcome = requestAction(item, agent, stepPath, transitionID, requestData, fileName, attachment, transactionKey);
                mStorage.commit(transactionKey);

                return Future.succeededFuture(finalOutcome);
            }
            catch (Throwable originalEx) {
                log.error("requestAction() - item:{} by agent:{} executing {}", item, agent, stepPath, originalEx);
                try {
                    mStorage.abort(transactionKey);
                }
                catch (PersistencyException e) {
                    log.debug("requestAction() - Could not abort original transaction {}", transactionKey , e);
                }

                if (StateMachine_enableErrorHandling.getBoolean()) {
                    handleError(stepPath, item, agent, originalEx);
                }

                return Future.failedFuture(CriseVertxException.toServiceException(originalEx));
            }
            finally {
                lock.release();
            }
        }
        catch (Throwable t) {
            log.error("requestAction() - could not lock item:{}", itemUuid, t);
            return Future.failedFuture(CriseVertxException.toServiceException(t));
        }
    }

    /**
     * 
     * @param item
     * @param agent
     * @param stepPath
     * @param transitionID
     * @param requestData
     * @param fileName
     * @param attachment
     * @param transactionKey
     * @return
     * @throws Exception
     */
    private String requestAction(ItemProxy item, AgentProxy agent, String stepPath, int transitionID, String requestData, String fileName,
            List<Byte> attachment, TransactionKey transactionKey) throws Exception
    {
        log.info("=======================================================================================");
        log.info("requestAction({}) Transition {} on {} by agent {}", item, transitionID, stepPath, agent);

        Workflow lifeCycle = (Workflow) mStorage.get(item.getPath(), ClusterType.LIFECYCLE + "/workflow", transactionKey);

        SecurityManager secMan = Gateway.getSecurityManager();

        Activity act = (Activity) lifeCycle.search(stepPath);

        if (act != null) {
            if (secMan.isShiroEnabled() && !secMan.checkPermissions(agent.getPath(), act, item.getPath(), transactionKey)) {
                String errorMsg = "'" + agent.getName() + "' is NOT permitted to execute step:" + stepPath;
                if (log.isTraceEnabled()) {
                    log.error(errorMsg);
                    for (RolePath role : agent.getRoles()) log.error(role.dump());
                }
                throw new AccessRightsException(errorMsg);
            }
        }
        else {
            throw new InvalidDataException("Step '" + stepPath + "' is not available for item:" + item);
        }

        byte[] bytes = ArrayUtils.toPrimitive(attachment.toArray(new Byte[0]));
        String finalOutcome = lifeCycle.requestAction(agent.getPath(), stepPath, item.getPath(), transitionID, requestData, fileName, bytes, transactionKey);

        // store the workflow and the Jobs if we've changed the state of the domain workflow
        if ( ! stepPath.startsWith("workflow/predefined")) {
            mStorage.put(item.getPath(), lifeCycle, transactionKey);

            mStorage.removeCluster(item.getPath(), ClusterType.JOB, transactionKey);

            ArrayList<Job> newJobs = ((CompositeActivity)lifeCycle.search("workflow/domain")).calculateJobs(agent.getPath(), item.getPath(), true);
            for (Job newJob: newJobs) {
                mStorage.put(item.getPath(), newJob, transactionKey);
                if (isNotBlank(newJob.getRoleOverride())) newJob.sendToRoleChannel();
            }
        }

        // remove entity path if transaction was successful
        if (stepPath.equals("workflow/predefined/Erase")) {
            log.info("requestAction() - deleting ItemPath:{}", item);
            Gateway.getLookupManager().delete(item.getPath(), transactionKey);
        }

        return finalOutcome;
    }

    /**
     * 
     * @param stepPath
     * @param item
     * @param agent
     * @param cause
     */
    private void handleError(String stepPath, ItemProxy item, AgentProxy agent, Throwable cause) {
        TransactionKey errorTransactionKey = new TransactionKey(item.getPath());

        try {
            // Start a new transaction
            mStorage.begin(errorTransactionKey);

            String errorOutcome = requestErrorAction(item, agent, stepPath, cause, errorTransactionKey);

            if (isNotBlank(errorOutcome)) {
                // Error handling was defined and successful therefore commit transaction to store error outcome
                mStorage.commit(errorTransactionKey);
            }
            else {
                // Error handling was not defined or there was an exception therefore abort transaction
                mStorage.abort(errorTransactionKey);
            }
        }
        catch (PersistencyException pex) {
            try {
                mStorage.abort(errorTransactionKey);
            }
            catch (PersistencyException e) {
                log.debug("handleError() - Could not abort error transaction {}", errorTransactionKey , e);
            }

            log.error("handleError() - ", pex);
        }
    }

    /**
     * 
     * @param itemPath
     * @param agent
     * @param stepPath
     * @param cause
     * @param transactionKey
     * @return
     */
    private String requestErrorAction(ItemProxy item, AgentProxy agent, String stepPath, Throwable cause, TransactionKey transactionKey) {
        try {
            Workflow lifeCycle = (Workflow) mStorage.get(item.getPath(), ClusterType.LIFECYCLE + "/workflow", transactionKey);

            int errorTransitionId = ((Activity) lifeCycle.search(stepPath)).getErrorTransitionId();

            if (errorTransitionId == -1) {
                log.debug("requestErrorAction({}) - StateMachine does not define error transition for step:{}", item, stepPath);
                return null;
            }

            log.info("---------------------------------------------------------------------------------------");
            log.info("requestErrorAction({}) - transitionId {} on {} by {}", item, errorTransitionId, stepPath, agent);

            String errorOutcome = Gateway.getMarshaller().marshall(new ErrorInfo(cause));

            errorOutcome = lifeCycle.requestAction(agent.getPath(), stepPath, item.getPath(), errorTransitionId, errorOutcome, "", null, transactionKey);

            // store the workflow if we've changed the state of the domain wf
            mStorage.put(item.getPath(), lifeCycle, transactionKey);

            return errorOutcome;
        }
        catch (Exception e) {
            log.error("requestErrorAction()", e);
            return "";
        }
    }

    /**
     * 
     */
    @Override
    @Deprecated
    public Future<String> queryLifeCycle(String itemUuid, String agentUuid, boolean filter) {
        ItemProxy item;
        AgentProxy agent;

        try {
            item  = Gateway.getProxy(Gateway.getLookup().getItemPath(itemUuid));
            agent = Gateway.getAgentProxy((AgentPath) Gateway.getLookup().getItemPath(agentUuid));
        }
        catch (Throwable t) {
            log.error("queryLifeCycle({})", itemUuid, t);
            return Future.failedFuture(CriseVertxException.toServiceException(t));
        }

        log.info("=======================================================================================");
        log.info("queryLifeCycle({}) - agent:{}", item, agent);

        try {
            Workflow wf = (Workflow) mStorage.get(item.getPath(), ClusterType.LIFECYCLE + "/workflow", null);
            @SuppressWarnings("unchecked")
            C2KLocalObjectMap<Job> jobs = (C2KLocalObjectMap<Job>)mStorage.get(item.getPath(), ClusterType.JOB.getName(), null);

            SecurityManager secMan = Gateway.getSecurityManager();
            JobArrayList jobBag = new JobArrayList();

            if (secMan.isShiroEnabled()) {
                for (Job j : jobs.values()) {
                    Activity act = (Activity) wf.search(j.getStepPath());
                    if (secMan.checkPermissions(agent.getPath(), act, item.getPath(), null)) {
                        try {
                            j.getTransition().checkPerformingRole(act, agent.getPath());
                            jobBag.list.add(j);
                        }
                        catch (AccessRightsException e) {
                            // AccessRightsException is thrown if Job requires specific Role that agent does not have
                        }
                    }
                }
            }
            else {
                jobBag.list = (ArrayList<Job>) jobs.values();
            }

            log.info("queryLifeCycle({}) - Returning {} jobs.", item, jobBag.list.size());

            try {
                String result = Gateway.getMarshaller().marshall(jobBag);
                return Future.succeededFuture(result);
            }
            catch (Throwable t) {
                log.error("queryLifeCycle({})", item, t);
                return Future.failedFuture(CriseVertxException.toServiceException(t));
            }
        }
        catch (Exception t) {
            log.error("queryLifeCycle({}) - Unknown error", item, t);
            return Future.failedFuture(CriseVertxException.toServiceException(t));
        }
    }
}
