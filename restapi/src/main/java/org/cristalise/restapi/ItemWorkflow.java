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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.graph.model.Vertex;
import org.cristalise.kernel.lifecycle.instance.Activity;
import org.cristalise.kernel.lifecycle.instance.CompositeActivity;
import org.cristalise.kernel.lifecycle.instance.Next;
import org.cristalise.kernel.lifecycle.instance.Workflow;
import org.cristalise.kernel.process.Gateway;
import org.json.XML;

import lombok.extern.slf4j.Slf4j;

@Path("/item/{uuid}/workflow") @Slf4j
public class ItemWorkflow extends ItemUtils {
    
    private Map<String, Object> getGanttTask(String uuid, String parent, Activity act) {
        LinkedHashMap<String, Object> aTask = new LinkedHashMap<String, Object>();

        if (parent == null) {
            aTask.put("id",    uuid);
            aTask.put("type", "project");
            aTask.put("text",  getProxy(uuid).getName());
            aTask.put("open",  true);

            aTask.put("duration", "");
            aTask.put("start_date", "");
        }
        else {
            aTask.put("id",       uuid + "/" + act.getID());
            aTask.put("parent",   parent);
            aTask.put("type",     "task");
            aTask.put("text",     act.getTypeName());
            
            Integer duration = (Integer)act.getProperties().get("Duration");
            aTask.put("duration", BigDecimal.valueOf(duration/3600.00));

            aTask.put("start_date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
        }

        return aTask;
    }

    private Map<String, Object> getGanttLink(String uuid, Next edge) {
        Map<String, Object> aLink = new LinkedHashMap<String, Object>();

        aLink.put("id",     uuid + "/" + edge.getID());
        aLink.put("source", uuid + "/" + edge.getOriginVertexId());
        aLink.put("target", uuid + "/" + edge.getTerminusVertexId());
        aLink.put("type",   "0");

        return aLink;
    }

    //FIXME: use Script or injected implementation (each gantt utility can have different json representation)
    private Map<String, Object> getGanttObject(Workflow wf) throws Exception {
        CompositeActivity domain = (CompositeActivity) wf.search("workflow/domain");

        LinkedHashMap<String, Object> ganttObject = new LinkedHashMap<String, Object>();

        ArrayList<Map<String, Object>> tasks = new ArrayList<>();
        ArrayList<Map<String, Object>> links = new ArrayList<>();

        Map<String, Object> parentTask = getGanttTask(wf.getItemUUID(), null, domain);
        tasks.add(parentTask);

        Vertex vertex = domain.getChildrenGraphModel().getStartVertex();

        do {
            if (vertex instanceof Activity) {
                Activity act = (Activity) vertex;

                tasks.add(getGanttTask(wf.getItemUUID(), wf.getItemUUID(), act));

                int outEdgeIds[] = act.getOutEdgeIds();

                if (outEdgeIds != null && outEdgeIds.length == 1) {
                    Next edge = (Next)domain.getChildrenGraphModel().resolveEdge(outEdgeIds[0]);

                    links.add(getGanttLink(wf.getItemUUID(), edge));

                    vertex = domain.getChildrenGraphModel().getVertexById(edge.getTerminusVertexId());
                }
                else
                    vertex = null;
            }
            else {
                log.warn("getGanttJSON() - Cannot handle vertex type:{}", vertex.getClass().getSimpleName());
            }
        }
        while (vertex != null);

        ganttObject.put("data",  tasks);
        ganttObject.put("links", links);

        return ganttObject;
    }

    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response getWorkflow(
            @Context                 HttpHeaders headers,
            @PathParam("uuid")       String  uuid,
            @QueryParam("gantt")     String  gantt,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);

        try {
            Workflow wf = getProxy(uuid).getWorkflow();

            if (produceJSON(headers.getAcceptableMediaTypes())) {
                if (gantt == null) return Response.ok(XML.toJSONObject(Gateway.getMarshaller().marshall(wf), true)).build();
                else               return toJSON(getGanttObject(wf));
            }
            else {
                if (gantt == null) return Response.ok(Gateway.getMarshaller().marshall(wf)).build();
                else               throw ItemUtils.createWebAppException("Cannot product Gantt in XML format", Response.Status.BAD_REQUEST);
            }
        }
        catch (Exception e) {
            throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }
}
