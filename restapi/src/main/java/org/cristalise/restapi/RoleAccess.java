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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;

@Path("/role")
public class RoleAccess extends PathUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRoles(
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri) 
    {
        return listRoles("", authCookie, uri);
    }

    @GET
    @Path("{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRoles(
            @PathParam("path")       String path,
            @CookieParam(COOKIENAME) Cookie  authCookie,
            @Context                 UriInfo uri) 
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ArrayList<Map<String, Object>> childRolesData = new ArrayList<>();

        RolePath startRole = new RolePath(path);

        Iterator<org.cristalise.kernel.lookup.Path> children = Gateway.getLookup().getChildren(startRole);

        while (children.hasNext()) {
            childRolesData.add(makeLookupData(path, children.next(), uri));
        }

        childRolesData.addAll(getRoleAgentsData(path, startRole, uri, cookie));

        return toJSON(childRolesData, cookie).build();
    }

    @GET
    @Path("{role}/summary")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoleSummary(
            @PathParam("role")       String roleName, 
            @CookieParam(COOKIENAME) Cookie authCookie, 
            @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        RolePath role;
        try {
            role = Gateway.getLookup().getRolePath(roleName);
        }
        catch (ObjectNotFoundException e1) {
            throw new WebAppExceptionBuilder().exception(e1).newCookie(cookie).build();
        }

        LinkedHashMap<String, Object> roleData = new LinkedHashMap<>();

        roleData.put("name", roleName);
        roleData.put("hasJobList", role.hasJobList());
        Iterator<org.cristalise.kernel.lookup.Path> childRoles = Gateway.getLookup().getChildren(role);

        if (childRoles.hasNext()) {
            LinkedHashMap<String, Object> childRoleData = new LinkedHashMap<>();
            while (childRoles.hasNext()) {
                RolePath childRole = (RolePath) childRoles.next();
                childRoleData.put(childRole.getName(), uri.getBaseUriBuilder().path("role").path(childRole.getName()).build());
            }
            roleData.put("subroles", childRoleData);
        }
        AgentPath[] agents;

        try {
            agents = Gateway.getLookup().getAgents(role);

            if (agents.length > 0) {
                LinkedHashMap<String, Object> agentData = new LinkedHashMap<String, Object>();
                for (AgentPath agent : agents) {
                    agentData.put(agent.getAgentName(), uri.getBaseUriBuilder().path("item").path(agent.getUUID().toString()).build());
                }
                roleData.put("agents", agentData);
            }

            return toJSON(roleData, cookie).build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    protected List<Map<String, Object>> getRoleAgentsData(String path, RolePath role, UriInfo uri, NewCookie cookie) {
        ArrayList<Map<String, Object>> agentsData = new ArrayList<>();

        
        try {
            for (AgentPath agent : Gateway.getLookup().getAgents(role)) {
                agentsData.add(makeLookupData(path, agent, uri));
            }
    
            return agentsData;
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }
}
