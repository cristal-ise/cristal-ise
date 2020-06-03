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

import java.nio.charset.Charset;
import java.util.Base64;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lifecycle.instance.predefined.agent.Login;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.security.SecurityManager;
import org.json.JSONObject;
import org.json.XML;

import lombok.extern.slf4j.Slf4j;

@Path("login") @Slf4j
public class CookieLogin extends RestHandler {

    @GET
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response login(
            @Context HttpHeaders headers,
            @QueryParam("user") String user,
            @QueryParam("pass") String pass)
    {
        return processLogin(user, pass, headers);
    }

    /**
     * Validates the provided user and pass and execute authentication.
     * 
     * @param user
     * @param pass
     * @param headers
     * @return
     */
    private Response processLogin(String user, String pass, HttpHeaders headers) {
        try {
            log.debug("agent:'{}'", user);

            AgentProxy agent = Gateway.getSecurityManager().authenticate(user, pass, null);
            agent.execute(agent, Login.class);

            return getCookieResponse(agent.getPath(), ItemUtils.produceJSON(headers.getAcceptableMediaTypes()));
        }
        catch (Exception ex) {
            //NOTE: Enable this log for testing security problems only, but always remove it when merged
            //log.error("", ex);
            String msg = SecurityManager.decodePublicSecurityMessage(ex);

            if (StringUtils.isBlank(msg)) msg = "Bad username/password";

            log.debug("error:{}", msg);

            throw new WebAppExceptionBuilder()
                    .message( msg )
                    .status( Response.Status.UNAUTHORIZED ).build();
        }
    }

    /**
     * Builds the appropriate response object based on the result of the user and pass validation and authentication.
     * 
     * @param agentPath
     * @param produceJSON
     * @return
     */
    private synchronized Response getCookieResponse(AgentPath agentPath, boolean produceJSON) {
        // create and set cookie
        try {
            NewCookie cookie = createNewCookie( agentPath );
            
            //Issue #143: Read 'password temporary flag' from Lookup, because agentPath is taken from AgentProxy which could be cached
            boolean tempPwd = Gateway.getLookup().getAgentPath(agentPath.getAgentName()).isPasswordTemporary();

            String result = "<Login result='Success' temporaryPassword='" + tempPwd + "' uuid='" + agentPath.getUUID() + "' />";
            if (produceJSON) result = XML.toJSONObject(result, true).toString();

            // FIXME: Perhaps Angular 4 bug. Return string is a json, so HttpClient will be able to process the response
            return Response.ok(result).cookie(cookie).build();
        }
        catch (Exception e) {
            log.error("Error creating cookie", e);
            throw new WebAppExceptionBuilder()
                    .message( "Error creating cookie" )
                    .status( Response.Status.INTERNAL_SERVER_ERROR ).build();
        }
    }

    /**
     * Login using the encoded credentials and {@link POST} method.
     * 
     * @param postData
     * @param headers
     * @return
     */
    @POST
    @Consumes({ MediaType.TEXT_PLAIN, MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON })
    public Response postLogin(String postData, @Context HttpHeaders headers) {
        String user;
        String pass;

        try {
            if (StringUtils.isEmpty(postData)) {
                throw new Exception("Authentication data is null or empty");
            }

            JSONObject authData = new JSONObject(postData);
            user = decode(authData.getString(USERNAME));
            pass = decode(authData.getString(PASSWORD));

            if (StringUtils.isEmpty(user) || StringUtils.isEmpty(pass)) {
                throw new Exception("Invalid username or password");
            }
        }
        catch (Exception e) {
            log.error("", e);
            throw new WebAppExceptionBuilder()
                    .message( "Problem logging in" )
                    .status( Response.Status.BAD_REQUEST ).build();
        }

        return processLogin(user, pass, headers);
    }

    /**
     * Decodes the base64 encoded string.
     * 
     * @param encodedStr
     * @return
     */
    private String decode(String encodedStr) {
        return new String(Base64.getDecoder().decode(encodedStr));
        //return new String(Base64.getDecoder().decode(encodedStr.getBytes(Charset.defaultCharset())));
    }

}