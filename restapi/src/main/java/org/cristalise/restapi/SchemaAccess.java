package org.cristalise.restapi;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("/schema")
public class SchemaAccess extends ResourceAccess {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAllSchemas(@CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		return listAllResources("OutcomeDesc", uri);
	}
	
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listSchemaVersions(@PathParam("name") String name, @CookieParam(COOKIENAME) Cookie authCookie,
			@Context UriInfo uri) {
		checkAuth(authCookie);
		return listResourceVersions("OutcomeDesc", "Schema", "schema", name, uri);
	}
	
	@GET
	@Path("{name}/{version}")
	@Produces(MediaType.TEXT_XML)
	public Response getSchema(@PathParam("name") String name, @PathParam("version") Integer version, 
			@CookieParam(COOKIENAME) Cookie authCookie) {
		checkAuth(authCookie);
		return getResource("OutcomeDesc", "Schema", name, version);
	}
}
