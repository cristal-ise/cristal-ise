package org.cristalise.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/schema")
public class SchemaAccess extends ResourceAccess {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response listAllSchemas(@Context UriInfo uri) {
		return listAllResources("OutcomeDesc", uri);
	}
	
	@GET
	@Path("{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response listSchemaVersions(@PathParam("name") String name, @Context UriInfo uri) {
		return listResourceVersions("OutcomeDesc", "Schema", "schema", name, uri);
	}
	
	@GET
	@Path("{name}/{version}")
	@Produces(MediaType.TEXT_XML)
	public Response getSchema(@PathParam("name") String name, @PathParam("version") Integer version) {
		return getResource("OutcomeDesc", "Schema", name, version);
	}
}
