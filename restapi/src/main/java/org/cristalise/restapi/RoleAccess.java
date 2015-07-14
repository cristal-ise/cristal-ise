package org.cristalise.restapi;

import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;

@Path("/role")
public class RoleAccess extends RestHandler {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listRoles(@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		LinkedHashMap<String, Object> roles = new LinkedHashMap<>();
		Iterator<org.cristalise.kernel.lookup.Path> iter = Gateway.getLookup().search(new RolePath(), "*");
		while (iter.hasNext()) {
			RolePath role = (RolePath)iter.next();
			roles.put(role.getName(), uri.getAbsolutePathBuilder().path(role.getName()));
		}
		return toJSON(roles);
	}
	
	@GET
	@Path("{role}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRole(@PathParam("role") String roleName, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		RolePath role;
		try {
			role = Gateway.getLookup().getRolePath(roleName);
		} catch (ObjectNotFoundException e1) {
			throw new WebApplicationException(404);
		}
		LinkedHashMap<String, Object> roleData = new LinkedHashMap<>();
		roleData.put("name", roleName);
		roleData.put("hasJobList", role.hasJobList());
		Iterator<org.cristalise.kernel.lookup.Path> childRoles = Gateway.getLookup().getChildren(role);
		if (childRoles.hasNext()) {
			LinkedHashMap<String, Object> childRoleData = new LinkedHashMap<>();
			while (childRoles.hasNext()) {
				RolePath childRole = (RolePath)childRoles.next();
				childRoleData.put(childRole.getName(), uri.getBaseUriBuilder().path("role").path(childRole.getName()).build());
			}
			roleData.put("subroles", childRoleData);
		}
		AgentPath[] agents;
		try {
			agents = Gateway.getLookup().getAgents(role);
		} catch (ObjectNotFoundException e) {
			throw new WebApplicationException(404);
		}
		if (agents.length > 0) {
			LinkedHashMap<String, Object> agentData = new LinkedHashMap<String, Object>();
			for (AgentPath agent : agents) {
				agentData.put(agent.getAgentName(), uri.getBaseUriBuilder().path("item").path(agent.getUUID().toString()).build());
			}
			roleData.put("agents", agentData);
		}
		return toJSON(roleData);
	}
	
}
