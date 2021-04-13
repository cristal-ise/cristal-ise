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

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.CriseVertxException;
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
        ItemPath item = null;
        AgentPath agent = null;
        TransactionKey transactionKey = null;
        Workflow lifeCycle = null;

        log.info("=======================================================================================");
        
        try {
            item  = Gateway.getLookup().getItemPath(itemUuid);
            agent = (AgentPath) Gateway.getLookup().getItemPath(agentUuid);

            transactionKey = new TransactionKey(item);
        }
        catch (InvalidItemPathException | ObjectNotFoundException e) {
            log.error("requestAction()", e);
            returnHandler.handle(e.fail());
            return;
        }

        try {
            log.info("request(" + item + ") Transition " + transitionID + " on " + stepPath + " by " + agent);

            // TODO: check if delegate is allowed valid for agent
            lifeCycle = (Workflow) mStorage.get(item, ClusterType.LIFECYCLE + "/workflow", null);

            mStorage.begin(transactionKey);

            SecurityManager secMan = Gateway.getSecurityManager();

            Activity act = (Activity) lifeCycle.search(stepPath);

            if (act != null) {
                if (secMan.isShiroEnabled() && !secMan.checkPermissions(agent, act, item, transactionKey)) {
                    if (log.isTraceEnabled()) for (RolePath role : agent.getRoles())
                        log.error(role.dump());
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

            mStorage.commit(transactionKey);

            returnHandler.handle(Future.succeededFuture(finalOutcome));
        }
        catch (Exception ex) {
            log.error("Unknown Error: requestAction on " + item + " by " + agent + " executing " + stepPath, ex);

            String errorOutcome = handleError(item, agent, stepPath, lifeCycle, ex, transactionKey);

            try {
                if (isBlank(errorOutcome)) {
                    mStorage.abort(transactionKey);

                    if (ex instanceof CriseVertxException) {
                        CriseVertxException e = (CriseVertxException) ex;
                        returnHandler.handle(e.fail());
                    }
                    else {
                        returnHandler.handle(Future.failedFuture(
                                new ServiceException(999, ex.getMessage(), CriseVertxException.convertToDebugInfo(ex))));
                    }
                }
                else {
                    mStorage.commit(transactionKey);
                    returnHandler.handle(Future.succeededFuture(errorOutcome));
                }
            }
            catch (Exception e) {
                log.error("", e);
                returnHandler.handle(Future.failedFuture(new ServiceException(999, e.getMessage())));
            }
        }
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
    private String handleError(ItemPath itemPath, AgentPath agent, String stepPath, Workflow lifeCycle, Exception cause, TransactionKey transactionKey) {
        if (!Gateway.getProperties().getBoolean("StateMachine.enableErrorHandling", false)) return null;

        int errorTransId = ((Activity) lifeCycle.search(stepPath)).getErrorTransitionId();
        if (errorTransId == -1) return null;

        try {
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
        log.info("queryLifeCycle(" + itemUuid + ") - agent: " + agentUuid);
        ItemPath item = null;
        AgentPath agent = null;

        log.info("=======================================================================================");

        try {
            item  = Gateway.getLookup().getItemPath(itemUuid);
            agent = (AgentPath) Gateway.getLookup().getItemPath(agentUuid);
        }
        catch (InvalidItemPathException | ObjectNotFoundException e) {
            log.error("requestAction()", e);
            returnHandler.handle(e.fail());
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
                return;
            }
            catch (Exception ex) {
                log.error("queryLifeCycle(" + item + ")", ex);
                if (ex instanceof CriseVertxException) {
                    CriseVertxException e = (CriseVertxException) ex;
                    returnHandler.handle(e.fail());
                }
                else {
                    returnHandler.handle(Future.failedFuture(
                            new ServiceException(999, ex.getMessage(), CriseVertxException.convertToDebugInfo(ex))));
                }
            }
        }
        catch (Exception ex) {
            log.error("queryLifeCycle(" + item + ") - Unknown error", ex);
            if (ex instanceof CriseVertxException) {
                CriseVertxException e = (CriseVertxException) ex;
                returnHandler.handle(e.fail());
            }
            else {
                returnHandler.handle(Future.failedFuture(
                        new ServiceException(999, ex.getMessage(), CriseVertxException.convertToDebugInfo(ex))));
            }
        }
    }
}
