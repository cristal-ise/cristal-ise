/**
 * This file is part of the CRISTAL-iSE REST API.
 * Copyright (c) 2001-2016 The CRISTAL Consortium. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 *
 * http://www.fsf.org/licensing/licenses/lgpl.html
 */
package org.cristalise.restapi;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.ForcedLogout;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.LoginTimeout;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.Logout;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Path("logout") @Slf4j
public class CookieLogout extends RestHandler   {

    @GET
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response logout(
            @Context                 HttpHeaders headers,
            @QueryParam("reason")    String      reason,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        AgentPath ap = checkAuthCookie(authCookie);

        log.debug("agent:'{}' reason:'{}'", ap.getAgentName(), reason);

        try {
            AgentProxy agent = Gateway.getProxyManager().getAgentProxy(ap);

            if      ("timeout".equals(reason))     agent.execute(agent, LoginTimeout.class);
            else if ("windowClose".equals(reason)) agent.execute(agent, ForcedLogout.class);
            else                                   agent.execute(agent, Logout.class);
        }
        catch (Exception e) {
            log.error("Problem logging out", e);
            throw ItemUtils.createWebAppException("Problem logging out", Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.ok().build();
    }
}