package org.cristalise.restapi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

@Path("login")
public class CookieLogin extends RestHandler {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response login(@QueryParam("user") String user, @QueryParam("pass") String pass, @Context UriInfo uri) {
        try {
			if (!Gateway.getAuthenticator().authenticate(user, pass, null))
				throw new WebApplicationException("Bad username/password", 401);
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
			throw new WebApplicationException("Agent '"+user+"' not found", 404);
		}
		
		// create and set cookie
        AuthData agentData = new AuthData(agentPath);
        try {
			NewCookie cookie;
			
			int cookieLife = Gateway.getProperties().getInt("REST.loginCookieLife", 0);
			if (cookieLife > 0) 
				cookie = new NewCookie(COOKIENAME, makeCookie(agentData), "/", null, null, cookieLife, false);
        	else
        		cookie = new NewCookie(COOKIENAME, makeCookie(agentData));
			return Response.ok().cookie(cookie).build();
		} catch (Exception e) {
			Logger.error(e);
			throw new WebApplicationException("Error creating cookie");
		}
	}
}
