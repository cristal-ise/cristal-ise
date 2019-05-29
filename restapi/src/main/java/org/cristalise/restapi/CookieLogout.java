package org.cristalise.restapi;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.Logout;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

@Path("logout")
public class CookieLogout extends RestHandler   {

    @GET
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response logout(
            @Context                 HttpHeaders headers, 
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        AgentPath ap = checkAuthCookie(authCookie);

        try {
            AgentProxy agent = Gateway.getProxyManager().getAgentProxy(ap);
            agent.execute(agent, Logout.class, "");
        }
        catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Problem logging out", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }
}