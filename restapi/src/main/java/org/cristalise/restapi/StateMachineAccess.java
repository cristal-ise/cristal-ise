package org.cristalise.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;

@Path("/stateMachine")
public class StateMachineAccess extends ResourceAccess {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAllStateMachines(@Context UriInfo uri) {
		return listAllResources("StateMachine", uri);
	}
	
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listStateMachineVersions(@PathParam("name") String name, @Context UriInfo uri) {
		return listResourceVersions("StateMachine", "StateMachine", "stateMachine", name, uri);
	}
	
	@GET
	@Path("{name}/{version}")
	@Produces(MediaType.TEXT_XML)
	public Response getStateMachineData(@PathParam("name") String name, @PathParam("version") Integer version) {
		return getResource("StateMachine", "StateMachine", name, version);
	}
}
