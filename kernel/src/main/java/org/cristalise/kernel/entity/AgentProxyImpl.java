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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*;

import java.util.Date;
import java.util.Map;

import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.InvalidTransitionException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.agent.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.graph.model.BuiltInVertexProperties;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.ErrorInfo;
import org.cristalise.kernel.scripting.Parameter;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptErrorException;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.CorbaExceptionUtility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AgentProxyImpl extends AgentProxy
{
    public AgentProxyImpl(AgentPath agentPath, Object locker) throws ObjectNotFoundException
    {
        super(null, agentPath);
        this.locker = locker;
    }

    @Override
    public String execute(Job job)
            throws AccessRightsException,
                InvalidDataException,
                InvalidTransitionException,
                ObjectNotFoundException,
                PersistencyException,
                ObjectAlreadyExistsException,
                ScriptErrorException,
                InvalidCollectionModification
    {
        ItemProxy item = Gateway.getProxyManager().getProxy(job.getItemPath());
        Date startTime = new Date();

        log.info("execute(job) - act:" + job.getStepPath() + " agent:" + mAgentPath.getAgentName());

        if (job.hasScript()) {
            log.info("execute(job) - executing script");
            try {
                // load script
                ErrorInfo scriptErrors = callScript(item, job);
                String errorString = scriptErrors.toString();
                if (scriptErrors.getFatal()) {
                    log.error("execute(job) - fatal script errors:{}", scriptErrors);
                    throw new ScriptErrorException(scriptErrors);
                }

                if (errorString.length() > 0) {
                    log.warn("Script errors: {}", errorString);
                }
            }
            catch (ScriptingEngineException ex) {
                Throwable cause = ex.getCause();

                if (cause == null) {
                    cause = ex;
                }

                log.error("", ex);

                throw new InvalidDataException(CorbaExceptionUtility.unpackMessage(cause));
            }
        }
        else if (job.hasQuery() &&  !"Query".equals(job.getActProp(BuiltInVertexProperties.OUTCOME_INIT))) {
            log.info("execute(job) - executing query (OutcomeInit != Query)");

            job.setOutcome(item.executeQuery(job.getQuery()));
        }

        // #196: Outcome is validated after script execution, becuase client(e.g. webui)
        // can submit an incomplete outcome which is made complete by the script
        if (job.hasOutcome() && job.isOutcomeSet()) {
            job.getOutcome().validateAndCheck();
        }

        job.setAgentPath(mAgentPath);

        if ((boolean)job.getActProp(SIMPLE_ELECTRONIC_SIGNATURE, false)) {
            executeSimpleElectonicSignature(job);
        }

        log.info("execute(job) - submitting job to item proxy");
        String result = item.requestAction(job);

        if (log.isDebugEnabled()) {
            Date timeNow = new Date();
            long secsNow = (timeNow.getTime() - startTime.getTime()) / 1000;
            log.debug("execute(job) - execution DONE in " + secsNow + " seconds");
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    private  ErrorInfo callScript(ItemProxy item, Job job) throws ScriptingEngineException, InvalidDataException, ObjectNotFoundException {
        Script script = job.getScript();

        CastorHashMap params = new CastorHashMap();
        params.put(Script.PARAMETER_ITEM,  item);
        params.put(Script.PARAMETER_AGENT, this);
        params.put(Script.PARAMETER_JOB,   job);

        Object returnVal = script.evaluate(item.getPath(), params, job.getStepPath(), true, locker);

        // At least one output parameter has to be ErrorInfo,
        // it is either a single unnamed parameter or a parameter named 'errors'
        if (returnVal instanceof Map) {
            return (ErrorInfo) ((Map)returnVal).get(getErrorInfoParameterName(script));
        }
        else {
            if (returnVal instanceof ErrorInfo) {
                return (ErrorInfo) returnVal;
            }
            else {
                throw new InvalidDataException("Script "+script.getName()+" return value must be of org.cristalise.kernel.scripting.ErrorInfo");
            }
        }
    }

    private String getErrorInfoParameterName(Script script) throws InvalidDataException {
        Parameter p;

        if (script.getOutputParams().size() == 1) {
            p = script.getOutputParams().values().iterator().next();
        }
        else {
            p = script.getOutputParams().get("errors");
        }

        if (p.getType() != ErrorInfo.class ) {
            throw new InvalidDataException("Script "+script.getName()+" must have at least one output of org.cristalise.kernel.scripting.ErrorInfo");
        }

        return p.getName();
    }

}
