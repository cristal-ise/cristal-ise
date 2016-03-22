package org.cristalise.restapi;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Iterator;
import java.util.LinkedHashMap;

@Path("/role")
public class RoleAccess extends RestHandler {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listRoles(@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuthCookie(authCookie);
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
		checkAuthCookie(authCookie);
		RolePath role;
		try {
			role = Gateway.getLookup().getRolePath(roleName);
		} catch (ObjectNotFoundException e1) {
			throw ItemUtils.createWebAppException(e1.getMessage(), Response.Status.NOT_FOUND);
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
			throw ItemUtils.createWebAppException(e.getMessage(), Response.Status.NOT_FOUND);
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
