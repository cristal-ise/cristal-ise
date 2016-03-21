package org.cristalise.restapi;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("login")
public class CookieLogin extends RestHandler {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response login(@QueryParam("user") String user, @QueryParam("pass") String pass, @Context UriInfo uri) {
        try {
			if (!Gateway.getAuthenticator().authenticate(user, pass, null))
				throw ItemUtils.createWebAppException("Bad username/password", Response.Status.UNAUTHORIZED);
		} catch (InvalidDataException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Problem logging in");
		} catch (ObjectNotFoundException e1) {
			Logger.msg(5, "CookieLogin.login() - Bad username/password");
			throw ItemUtils.createWebAppException("Bad username/password", Response.Status.UNAUTHORIZED);
		}

        AgentPath agentPath;
		try {
			agentPath = Gateway.getLookup().getAgentPath(user);
		} catch (ObjectNotFoundException e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Agent '"+user+"' not found", Response.Status.NOT_FOUND);
		}
		
		// create and set cookie
        AuthData agentData = new AuthData(agentPath);
        try {
			NewCookie cookie;
			
			int cookieLife = Gateway.getProperties().getInt("REST.loginCookieLife", 0);
			if (cookieLife > 0) 
				cookie = new NewCookie(COOKIENAME, encryptAuthData(agentData), "/", null, null, cookieLife, false);
        	else
        		cookie = new NewCookie(COOKIENAME, encryptAuthData(agentData));
			return Response.ok().cookie(cookie).build();
		} catch (Exception e) {
			Logger.error(e);
			throw ItemUtils.createWebAppException("Error creating cookie");
		}
	}
}
