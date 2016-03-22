package org.cristalise.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

@Path("auth")
public class TokenLogin extends RestHandler {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response login(@QueryParam("user") final String user,
			@QueryParam("pass") final String pass, @Context final UriInfo uri) {
		try {
			if (!Gateway.getAuthenticator().authenticate(user, pass, null)) {
				throw new WebApplicationException("Bad username/password", 401);
			}
		} catch (InvalidDataException e) {
			Logger.error(e);
			throw new WebApplicationException("Problem logging in");
		} catch (ObjectNotFoundException e1) {
			throw new WebApplicationException("Bad username/password", 401);
		}

		AgentPath agentPath;
		try {
			agentPath = Gateway.getLookup().getAgentPath(user);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw new WebApplicationException("Agent '" + user + "' not found",
					404);
		}

		// create and set token
		AuthData agentData = new AuthData(agentPath);
		try {

			String token = encryptAuthData(agentData);
			return Response.ok(token).build();
		} catch (Exception e) {
			Logger.error(e);
			throw new WebApplicationException("Error creating token");
		}
	}

}
