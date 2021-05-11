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
import static org.cristalise.kernel.common.CriseVertxException.FailureCodes.InternalServerError;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.CriseVertxException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.agent.JobArrayList;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.lifecycle.instance.predefined.PredefinedStepContainer;
import org.cristalise.kernel.lifecycle.instance.predefined.item.ItemPredefinedStepContainer;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.persistency.ClusterStorageManager;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.security.SecurityManager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;
import io.vertx.serviceproxy.ServiceException;
import lombok.extern.slf4j.Slf4j;

/**************************************************************************
 * TraceableEntity is the implementation of the vertx service interface
 * 
 * Traditional Pinky/Brain ASCII art:
 * 
 * <pre>
*                                ,.   '\'\    ,---.
*                            .  | \\  l\\l_ //    |
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
 ***************************************************************************/
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
    public void requestAction(String itemUuid, String agentUuid, String stepPath, int transitionID, String requestData, String fileName,
            List<Byte> attachment, Handler<AsyncResult<String>> returnHandler)
    {
        log.info("=======================================================================================");
        log.info("requestAction("+itemUuid+") Transition " + transitionID + " on " + stepPath + " by agent " + agentUuid);

        ItemPath item;
        AgentPath agent;
        TransactionKey transactionKey;

        try {
            item  = Gateway.getLookup().getItemPath(itemUuid);
            agent = (AgentPath) Gateway.getLookup().getItemPath(agentUuid);
            transactionKey = new TransactionKey(item);
        }
        catch (InvalidItemPathException | ObjectNotFoundException e) {
            log.error("requestAction()", e);
            returnHandler.handle(e.failService());
            return;
        }

        SharedData sharedData = Gateway.getVertx().sharedData();

        sharedData.getLockWithTimeout(itemUuid, 5000,  lockResult -> {
        if (lockResult.succeeded()) {
            // Got the lock!
            Lock lock = lockResult.result();

            try {
                mStorage.begin(transactionKey);
                String finalOutcome = requestAction(item, agent, stepPath, transitionID, requestData, fileName, attachment, transactionKey);
                mStorage.commit(transactionKey);
                returnHandler.handle(Future.succeededFuture(finalOutcome));
            }
            catch (Exception ex) {
                log.error("requestAction() - " + item + " by " + agent + " executing " + stepPath, ex);
                try {
                    mStorage.abort(transactionKey);
                }
                catch (PersistencyException e) {}

                TransactionKey errorTransactionKey = new TransactionKey(item);

                try {
                    // Start a new transaction
                    mStorage.begin(errorTransactionKey);

                    String errorOutcome = handleError(item, agent, stepPath, ex, transactionKey);

                    if (isNotBlank(errorOutcome)) {
                        // Error handling was defined and successful therefore commit transaction to store error outcome
                        mStorage.commit(errorTransactionKey);
                    }
                    else{
                        // Error handling was not defined or there was an exception therefore abort transaction
                        mStorage.abort(errorTransactionKey);
                    }

                    // Now throw the original exception
                    if (ex instanceof CriseVertxException) {
                        CriseVertxException criseEx = (CriseVertxException) ex;
                        returnHandler.handle(criseEx.failService());
                    }
                    else {
                        returnHandler.handle(CriseVertxException.failService(ex));
                    }
                }
                catch (PersistencyException pex) {
                    try {
                        mStorage.abort(errorTransactionKey);
                    }
                    catch (PersistencyException e) {}

                    log.error("requestAction()", pex);
                    returnHandler.handle(pex.failService());
                }
            }

            lock.release();
        }
        else {
            Throwable cause = lockResult.cause();
            log.error("requestAction()", cause);
            JsonObject debugInfo = CriseVertxException.convertToDebugInfo(cause);
            returnHandler.handle(CriseVertxException.failService(cause));
        }
        });
    }

    private String requestAction(ItemPath item, AgentPath agent, String stepPath, int transitionID, String requestData, String fileName,
            List<Byte> attachment, TransactionKey transactionKey)
            throws PersistencyException, ObjectNotFoundException, AccessRightsException, InvalidDataException, InvalidTransitionException,
            ObjectAlreadyExistsException, ObjectCannotBeUpdated, CannotManageException, InvalidCollectionModification
    {
        Workflow lifeCycle = (Workflow) mStorage.get(item, ClusterType.LIFECYCLE + "/workflow", transactionKey);

        SecurityManager secMan = Gateway.getSecurityManager();

        Activity act = (Activity) lifeCycle.search(stepPath);

        if (act != null) {
            if (secMan.isShiroEnabled() && !secMan.checkPermissions(agent, act, item, transactionKey)) {
                if (log.isTraceEnabled()) {
                    for (RolePath role : agent.getRoles()) log.error(role.dump());
                }
                throw new AccessRightsException("'" + agent.getAgentName() + "' is NOT permitted to execute step:" + stepPath);
            }
        }
        else {
            throw new InvalidDataException("Step '" + stepPath + "' is not available for item:" + item);
        }

        byte[] bytes = ArrayUtils.toPrimitive(attachment.toArray(new Byte[0]));
        String finalOutcome = lifeCycle.requestAction(agent, stepPath, item, transitionID, requestData, fileName, bytes, transactionKey);

        // store the workflow if we've changed the state of the domain wf
        if (!(stepPath.startsWith("workflow/predefined"))) mStorage.put(item, lifeCycle, transactionKey);

        // remove entity path if transaction was successful
        if (stepPath.equals("workflow/predefined/Erase")) {
            log.info("requestAction() - deleting ItemPath:{}", item);
            Gateway.getLookupManager().delete(item, transactionKey);
        }

        return finalOutcome;
    }

    /**
     * 
     * @param itemPath
     * @param agent
     * @param stepPath
     * @param lifeCycle
     * @param cause
     * @param transactionKey
     * @return
     */
    private String handleError(ItemPath itemPath, AgentPath agent, String stepPath, Exception cause, TransactionKey transactionKey) {
        if (!Gateway.getProperties().getBoolean("StateMachine.enableErrorHandling", false)) return null;

        try {
            Workflow lifeCycle = (Workflow) mStorage.get(itemPath, ClusterType.LIFECYCLE + "/workflow", transactionKey);

            int errorTransId = ((Activity) lifeCycle.search(stepPath)).getErrorTransitionId();
            if (errorTransId == -1) return null;

            log.info("handleError({}) - errorTransId " + errorTransId + " on " + stepPath + " by " + agent, itemPath);

            String errorOutcome = Gateway.getMarshaller().marshall(new ErrorInfo(cause));

            lifeCycle.requestAction(agent, stepPath, itemPath, errorTransId, errorOutcome, "", null, transactionKey);

            // store the workflow if we've changed the state of the domain wf
            if (!(stepPath.startsWith("workflow/predefined"))) mStorage.put(itemPath, lifeCycle, transactionKey);

            return errorOutcome;
        }
        catch (Exception e) {
            log.error("handleError()", e);
            return "";
        }
    }

    /**
     *
     */
    @Override
    public void queryLifeCycle(String itemUuid, String agentUuid, boolean filter, Handler<AsyncResult<String>> returnHandler) {
        log.info("=======================================================================================");
        log.info("queryLifeCycle(" + itemUuid + ") - agent: " + agentUuid);

        ItemPath item = null;
        AgentPath agent = null;

        try {
            item  = Gateway.getLookup().getItemPath(itemUuid);
            agent = (AgentPath) Gateway.getLookup().getItemPath(agentUuid);
        }
        catch (InvalidItemPathException | ObjectNotFoundException e) {
            log.error("queryLifeCycle("+item+")", e);
            returnHandler.handle(e.failService());
            return;
        }

        try {
            Workflow wf = (Workflow) mStorage.get(item, ClusterType.LIFECYCLE + "/workflow", null);

            JobArrayList jobBag = new JobArrayList();
            CompositeActivity domainWf = (CompositeActivity) wf.search("workflow/domain");
            ArrayList<Job> jobs = filter ? 
                    domainWf.calculateJobs(agent, item, true) : domainWf.calculateAllJobs(agent, item, true);

            SecurityManager secMan = Gateway.getSecurityManager();

            if (secMan.isShiroEnabled()) {
                for (Job j : jobs) {
                    Activity act = (Activity) wf.search(j.getStepPath());
                    if (secMan.checkPermissions(agent, act, item, null)) {
                        try {
                            j.getTransition().getPerformingRole(act, agent);
                            jobBag.list.add(j);
                        }
                        catch (AccessRightsException e) {
                            // AccessRightsException is thrown if Job requires specific Role that agent does not have
                        }
                    }
                }
            }
            else {
                jobBag.list = jobs;
            }

            log.info("queryLifeCycle(" + item + ") - Returning " + jobBag.list.size() + " jobs.");

            try {
                String result = Gateway.getMarshaller().marshall(jobBag);
                returnHandler.handle(Future.succeededFuture(result));
            }
            catch (Exception ex) {
                log.error("queryLifeCycle(" + item + ")", ex);
                if (ex instanceof CriseVertxException) {
                    CriseVertxException e = (CriseVertxException) ex;
                    returnHandler.handle(e.failService());
                }
                else {
                    returnHandler.handle(CriseVertxException.failService(ex));
                }
            }
        }
        catch (Exception ex) {
            log.error("queryLifeCycle(" + item + ") - Unknown error", ex);
            if (ex instanceof CriseVertxException) {
                CriseVertxException e = (CriseVertxException) ex;
                returnHandler.handle(e.failService());
            }
            else {
                returnHandler.handle(CriseVertxException.failService(ex));
            }
        }
    }
}
