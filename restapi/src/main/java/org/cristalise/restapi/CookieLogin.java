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

import java.util.Base64;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONObject;
import org.json.XML;

@Path("login")
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
            AgentPath agentPath = Gateway.getSecurityManager().authenticate(user, pass, null).getPath();
            return getCookieResponse(agentPath, ItemUtils.produceJSON(headers.getAcceptableMediaTypes()));
        }
        catch (ObjectNotFoundException | InvalidDataException ex) {
            //NOTE: Enable this log for testing security problems only, but always remove it when merged
            //Logger.error(ex);
            Logger.msg(5, "CookieLogin.login() - Bad username/password");
            throw ItemUtils.createWebAppException("Bad username/password", Response.Status.UNAUTHORIZED);
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
        AuthData agentData = new AuthData(agentPath);
        try {
            NewCookie cookie;

            int cookieLife = Gateway.getProperties().getInt("REST.loginCookieLife", 0);

            if (cookieLife > 0)
                cookie = new NewCookie(COOKIENAME, encryptAuthData(agentData), "/", null, null, cookieLife, false);
            else
                cookie = new NewCookie(COOKIENAME, encryptAuthData(agentData));

            String result = "<Login result='Success' temporaryPassword='" + agentPath.isPasswordTemporary() + "' />";
            if (produceJSON) result = XML.toJSONObject(result).toString();

            // FIXME: Perhaps Angular 4 bug. Return string is a json, so HttpClient will be able to process the response
            return Response.ok(result).cookie(cookie).build();
        }
        catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error creating cookie");
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
            Logger.error(e);
            throw ItemUtils.createWebAppException("Problem logging in", Response.Status.BAD_REQUEST);
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
    }

}