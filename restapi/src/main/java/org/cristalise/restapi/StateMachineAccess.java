package org.cristalise.restapi;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/stateMachine")
public class StateMachineAccess extends ResourceAccess {
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAllStateMachines(@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		return listAllResources("StateMachine", uri);
	}
	
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listStateMachineVersions(@PathParam("name") String name, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		return listResourceVersions("StateMachine", "StateMachine", "stateMachine", name, uri);
	}
	
	@GET
	@Path("{name}/{version}")
	@Produces(MediaType.TEXT_XML)
	public Response getStateMachineData(@PathParam("name") String name, @PathParam("version") Integer version,
			@CookieParam(COOKIENAME) Cookie authCookie) {
		checkAuth(authCookie);
		return getResource("StateMachine", "StateMachine", name, version);
	}
}
