/**
 * This file is part of the CRISTAL-iSE REST API.
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
package org.cristalise.restapi;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.entity.Job;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.outcomebuilder.GeneratedFormType;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.utils.CastorHashMap;
import org.json.JSONArray;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.ArrayList;
import java.util.List;

import static org.cristalise.kernel.persistency.outcomebuilder.GeneratedFormType.*;

@Path("/item/{uuid}/job") @Slf4j
public class ItemJob extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobs(
            @PathParam("uuid")            String  uuid,
            @QueryParam("agent")          String  agentName,
            @QueryParam("activityName")   String  activityName,
            @QueryParam("transitionName") String  transitionName,
            @CookieParam(COOKIENAME)      Cookie  authCookie,
            @Context                      UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        List<Job> jobList = null;
        Job job = null;
        try {
            AgentProxy agent = getAgent(agentName, authCookie);
            if (StringUtils.isNotBlank(activityName)) {
                if (StringUtils.isNotBlank(transitionName)) job = item.getJobByTransitionName(activityName, transitionName, agent);
                else                                        job = item.getJobByName(activityName, agent);
            }
            else
                jobList = item.getJobs(agent);
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }

        if (jobList != null) {
            ArrayList<Object> jobListData = new ArrayList<Object>();

            for (Job j : jobList) jobListData.add(makeJobData(j, item, uri));

            return toJSON(jobListData, cookie).build();
        }
        else if (job != null) {
            return toJSON(makeJobData(job, item, uri), cookie).build();
        }
        else {
            throw new WebAppExceptionBuilder().message("No job found for actName:" + activityName + " transName:" + transitionName)
                        .status(Response.Status.NOT_FOUND).newCookie(cookie).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("formModel/{activityPath: .*}")
    public Response getJobFormModel(
            @Context                    HttpHeaders headers,
            @PathParam("uuid")          String      uuid,
            @PathParam("activityPath")  String      actPath,
            @QueryParam("transition")   String      transition,
            @CookieParam(COOKIENAME)    Cookie      authCookie,
            @Context                    UriInfo     uri)
    {
        return getJobForm(uuid, actPath, transition, authCookie, uri, NgDynamicFormModel);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("formLayout/{activityPath: .*}")
    public Response getJobFormLayout(
            @Context                    HttpHeaders headers,
            @PathParam("uuid")          String      uuid,
            @PathParam("activityPath")  String      actPath,
            @QueryParam("transition")   String      transition,
            @CookieParam(COOKIENAME)    Cookie      authCookie,
            @Context                    UriInfo     uri)
    {
        return getJobForm(uuid, actPath, transition, authCookie, uri, NgDynamicFormLayout);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("formTemplate/{activityPath: .*}")
    public Response getJobFormTemplate(
            @Context                    HttpHeaders headers,
            @PathParam("uuid")          String      uuid,
            @PathParam("activityPath")  String      actPath,
            @QueryParam("transition")   String      transition,
            @CookieParam(COOKIENAME)    Cookie      authCookie,
            @Context                    UriInfo     uri) 
    {
        return getJobForm(uuid, actPath, transition, authCookie, uri, NgDynamicFormTemplate);
    }

    /**
     * 
     */
    private Response getJobForm(
            String uuid, 
            String actPath, 
            String transition, 
            Cookie authCookie, 
            UriInfo uri, 
            GeneratedFormType formType)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        if (actPath == null) {
            throw new WebAppExceptionBuilder().message("Must specify activity path")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }
        if (actPath.startsWith(PREDEFINED_PATH)) {
            throw new WebAppExceptionBuilder().message("getJobFormTemplate() is unimplemented for PredefinedSteps")
                    .status(Response.Status.BAD_REQUEST).newCookie(cookie).build();
        }

        log.debug("getJobForm() - item:{} activityPath:{} formType:{}", item, actPath, formType);

        try {
            transition = extractAndCheckTransitionName(transition, uri);
            AgentProxy agent = Gateway.getAgentProxy(getAgentPath(authCookie));

            Job thisJob = item.getJobByTransitionName(actPath, transition, agent);

            if (thisJob == null) {
                throw new WebAppExceptionBuilder().message("Job not found for actPath:"+actPath+" transition:"+transition)
                        .status(Response.Status.NOT_FOUND).newCookie(cookie).build();
            }
            
            CastorHashMap inputs = (CastorHashMap) thisJob.getActProps().clone();

            inputs.put(Script.PARAMETER_AGENT, agent);
            inputs.put(Script.PARAMETER_ITEM, item);
            inputs.put(Script.PARAMETER_JOB, thisJob);

            // set outcome if required
            if (thisJob.hasOutcome()) {
                JSONArray formTemplate = new OutcomeBuilder(thisJob.getSchema(), false).generateNgDynamicFormsJson(inputs, formType);
                return Response.ok(formTemplate.toString()).cookie(cookie).build();
            }
            else {
                log.debug("getJobForm() - no outcome needed for job:{}", thisJob);
                return Response.noContent().cookie(cookie).build();
            }
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }
}
