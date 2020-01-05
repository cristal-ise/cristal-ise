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

import static org.cristalise.kernel.property.BuiltInItemProperties.NAME;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.AesCipherService;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.Property;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
abstract public class RestHandler {

    private ObjectMapper mapper;
    private boolean requireLogin = true;

    private static Key cookieKey;
    private static AesCipherService aesCipherService;

    public static final String COOKIENAME = "cauth";
    public static final String USERNAME   = "username";
    public static final String PASSWORD   = "password";

    static {
        int keySize = Gateway.getProperties().getBoolean("REST.allowWeakKey", false) ? 128 : 256;

        aesCipherService = new AesCipherService();
        cookieKey = aesCipherService.generateNewKey(keySize);
    }

    public RestHandler() {
        mapper = new ObjectMapper();
        requireLogin = Gateway.getProperties().getBoolean("REST.requireLoginCookie", true);
    }

    /**
     * 
     * @param authData
     * @return
     * @throws InvalidAgentPathException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidDataException
     */
    protected synchronized AuthData decryptAuthData(String authData)
            throws InvalidAgentPathException, IllegalBlockSizeException, BadPaddingException, InvalidDataException
    {
        byte[] bytes = DatatypeConverter.parseBase64Binary(authData);

        for (int cntRetries = 1; ; cntRetries++) {
            try {
                return new AuthData(aesCipherService.decrypt(bytes, cookieKey.getEncoded()).getBytes());
            }
            catch (final Exception e) {
                log.trace("Exception caught in decryptAuthData: #{}", cntRetries , e);

                // try to decrypt 5 times before throwing an exception
                if (cntRetries == 5) 
                    log.error("Faild to decrypting AuthData ({}th try)", cntRetries , e);
                    throw e;
            }
        }
    }

    /**
     * 
     * @param auth
     * @return
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    protected synchronized String encryptAuthData(AuthData auth)
            throws IllegalBlockSizeException, BadPaddingException
    {
        byte[] bytes = aesCipherService.encrypt(auth.getBytes(), cookieKey.getEncoded()).getBytes();
        return DatatypeConverter.printBase64Binary(bytes);
    }

    public Response.ResponseBuilder toJSON(Object data, NewCookie cookie) {
        try {
            String json = mapper.writeValueAsString(data);
            log.debug("JSON response:{}", json);
            if (cookie != null) return Response.ok(json).cookie(cookie);
            else                return Response.ok(json);
        }
        catch (IOException e) {
            throw new WebAppExceptionBuilder("Problem building response JSON", e, Response.Status.INTERNAL_SERVER_ERROR, cookie).build();
        }
    }

    /**
     * This method will check if authentication is 30seconds old, if true then it will return NewCookie.
     * Return null if not.
     *
     * @param authData
     * @return NewCookie
     */
    public NewCookie checkAndCreateNewCookie(AuthData authData) {
        if ( authData != null ) {
            boolean userNoTimeout = isUserNoTimeout( authData.agent );
            if ( !userNoTimeout && ((new Date().getTime() - authData.timestamp.getTime()) / 1000 > 30) ) {
                authData.timestamp = new Date();
                return createNewCookie(authData);
            }
        }

        return null;
    }

    /**
     * This method will check if authentication is 30seconds old, if true then it will return NewCookie.
     * Return null if not.
     *
     * @param authCookie
     * @return NewCookie
     */
    public NewCookie checkAndCreateNewCookie(Cookie authCookie) {
        return checkAndCreateNewCookie( checkAuthCookie( authCookie) );
    }

    public NewCookie createNewCookie(AgentPath agentPath) {
        AuthData agentData = new AuthData(agentPath);
        return createNewCookie( agentData );
    }

    public NewCookie createNewCookie(AuthData authData) {
        try {
            NewCookie cookie = new NewCookie(COOKIENAME, encryptAuthData(authData), "/", null, null, -1, false);
            return  cookie;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            log.error("Problem building response JSON", e);
            throw new WebAppExceptionBuilder("Problem building response JSON: ", e, Response.Status.INTERNAL_SERVER_ERROR, null).build();
        }
    }

    /**
     * AgentPath is decrypted from the cookie
     *
     * @param authCookie the cookie sent by the client
     * @return AgentPath decrypted from the cookie
     */
    public synchronized AgentPath getAgentPath(Cookie authCookie) {
        AuthData authData = checkAuthCookie( authCookie );

        if ( authData == null ) {
            return null;
        } else {
            return authData.agent;
        }
    }

    /**
     * Authorisation data is decrypted from the cookie
     * 
     * @param authCookie the cookie sent by the client
     * @return AuthData decrypted from the cookie
     */
    public synchronized AuthData checkAuthCookie(Cookie authCookie) {
        if(authCookie == null) return checkAuthData(null);
        else                   return checkAuthData(authCookie.getValue());
    }

    /**
     * Authorization data is decrypted from the input string and the corresponding AgentPath is returned
     * 
     * @param authData authorisation data normally taken from cookie or token
     * @return AgentPath created from the decrypted autData
     */
    private AuthData checkAuthData(String authData) {
        if (!requireLogin) return null;

        if (authData == null) {
            throw new WebAppExceptionBuilder().message("Missing authentication data").status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            AuthData data = decryptAuthData(authData);
            return data;
        }
        catch (InvalidAgentPathException | InvalidDataException e) {
            log.debug("Invalid agent or login data",  e);

            throw new WebAppExceptionBuilder().message("Invalid agent or login data").status(Response.Status.UNAUTHORIZED).build();
        }
        catch (Exception e) {
            log.debug("Error reading authentication data:{}", authData, e);

            throw new WebAppExceptionBuilder().message("Error reading authentication data").status(Response.Status.UNAUTHORIZED).build();
        }
    }

    /**
     * AgentProxy is resolved either from cookie or from the name. If property REST.requireLoginCookie=true
     * the AgentProxy must be resolved from the authorisation data or an error is thrown.
     * 
     * @param agentName the name of the Agent
     * @param authCookie the cookie sent by the client
     * @returnAgentProxy
     */
    public AgentProxy getAgent(String agentName, Cookie authCookie) throws ObjectNotFoundException {
        if(authCookie == null) return getAgent(agentName, (String)null);
        else                   return getAgent(agentName, authCookie.getValue());
    }

    /**
     * AgentProxy is resolved either from authorisation data or from the name. If property REST.requireLoginCookie=true
     * the AgentProxy must be resolved from the authorisation data or an error is thrown.
     * 
     * @param agentName the name of the Agent
     * @param authData authorisation data (from cookie or token)
     * @return AgentProxy
     */
    public AgentProxy getAgent(String agentName, String authData) throws ObjectNotFoundException {
        AgentPath agentPath;
        AuthData agentAuthData = checkAuthData(authData);
        try {
            if(agentAuthData == null ) {
                if (StringUtils.isNoneBlank(agentName)) {
                    agentPath = Gateway.getLookup().getAgentPath(agentName);
                }
                else {
                    throw new ObjectNotFoundException("AgentName is empty");
                }
            }
            else {
                agentPath = agentAuthData.agent;
            }

            return (AgentProxy)Gateway.getProxyManager().getProxy(agentPath);
        }
        catch (ObjectNotFoundException e) {
            log.error("Agent not found", e);
            throw new ObjectNotFoundException("Agent not found");
        }
    }

    /**
     * Constructs the Map containing all data describing a PagedResult. Normally this result is converted to JSON
     * 
     * @param uri The resource URI which is used to create pervPage and nextPage URL
     * @param start the start index of the result set to be retrieved
     * @param batchSize the size of the result set to be retrieved
     * @param totalRows the total number of rows in the result set
     * @param rows the actual rows
     * @return the Map containing all data describing a PagedResult
     */
    public Map<String, Object> getPagedResult(UriInfo uri, int start, int batchSize, int totalRows, List<?> rows) {
        LinkedHashMap<String, Object> pagedReturnData = new LinkedHashMap<>();

        pagedReturnData.put("start", start);
        pagedReturnData.put("pageSize", batchSize);
        pagedReturnData.put("totalRows", totalRows);

        if (batchSize > 0 && start - batchSize >= 0) {
            pagedReturnData.put("prevPage", 
                    uri.getAbsolutePathBuilder()
                    .replaceQueryParam("start", start - batchSize)
                    .replaceQueryParam("batch", batchSize)
                    .build());
        }

        if (batchSize > 0 && start + batchSize < totalRows) {
            pagedReturnData.put("nextPage", 
                    uri.getAbsolutePathBuilder()
                    .replaceQueryParam("start", start + batchSize)
                    .replaceQueryParam("batch", batchSize)
                    .build());
        }

        pagedReturnData.put("rows", rows);

        return pagedReturnData;
    }

    /**
     * Converts QueryParams to Item Properties
     * 
     * @param search the string to decompose in the format: name,prop:val,prop:val
     * @return the decoded list of Item Properties
     */
    public List<Property> getPropertiesFromQParams(String search) {
        String[] terms = search.split(",");
    
        List<Property> props = new ArrayList<>();
    
        for (int i = 0; i < terms.length; i++) {
            if (terms[i].contains(":")) { // assemble property if we have name:val
                String[] nameval = terms[i].split(":");
                String value = nameval[1];
    
                if (nameval.length != 2) {
                    throw new WebAppExceptionBuilder().message("Invalid search term: " + terms[i]).status(Status.BAD_REQUEST).build();
                }
    
                try {
                    value = URLDecoder.decode(nameval[1], "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    log.error("Error decoding search value: " + nameval[1], e);
                    throw new WebAppExceptionBuilder().message("Error decoding search value: " + nameval[1]).status(Status.BAD_REQUEST).build();
                }
                
                props.add(new Property(nameval[0], value));
            }
            else if (i == 0) { // first search term can imply Name if no propname given
                props.add(new Property(NAME, terms[i]));
            }
            else {
                throw new WebAppExceptionBuilder().message("Only the first search term may omit property name").status(Status.BAD_REQUEST).build();
            }
        }
        return props;
    }

    private boolean isUserNoTimeout ( AgentPath agent ) {
        RolePath[] roles = agent.getRoles();
        String roleWithoutTimeout = Gateway.getProperties().getString("REST.role.withoutTimeout");

        boolean userNoTimeout = false;

        if (StringUtils.isNotBlank(roleWithoutTimeout)) {
            for(RolePath role: roles) {
                if (role.getName().equals(roleWithoutTimeout)) {
                    log.trace("AuthData - cookie timeout is disabled for the current user:{}", agent.getName());
                    userNoTimeout = true;
                }
            }
        }

        return userNoTimeout;
    }

    /**
     * 
     * @param ip
     */
    protected  Map<String, Object> makeItemDomainPathsData(ItemPath ip) {
        PagedResult result = Gateway.getLookup().searchAliases(ip, 0, 50);

        Map<String, Object> returnVal = new LinkedHashMap<String, Object>();
        ArrayList<Object> domainPathesData = new ArrayList<>();

        for (Path p: result.rows) domainPathesData.add(p.getStringPath());

        if (domainPathesData.size() != 0) {
            returnVal.put("uuid", ip.getUUID().toString());
            returnVal.put("name", ((DomainPath)result.rows.get(0)).getName());
            returnVal.put("domainPaths", domainPathesData);
        }
        else if (ip instanceof AgentPath) {
            returnVal.put("uuid", ip.getUUID().toString());
            returnVal.put("name", ((AgentPath)ip).getAgentName());
            returnVal.put("error", "Agent has no aliases");
        }

        return returnVal;
    }

    /**
     * Handles encoding/decoding of agent and timestamp data
     *
     */
    public class AuthData {
        AgentPath agent;
        Date timestamp;

        public AuthData(AgentPath agent) {
            this.agent = agent;
            timestamp = new Date();
        }

        public AuthData(byte[] bytes) throws InvalidAgentPathException, InvalidDataException {
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            SystemKey sysKey = new SystemKey(buf.getLong(), buf.getLong());
            agent = new AgentPath(new ItemPath(sysKey));
            timestamp = new Date(buf.getLong());
            int cookieLife = Gateway.getProperties().getInt("REST.loginCookieLife", 0);

            boolean userNoTimeout = isUserNoTimeout( this.agent );

            if (!userNoTimeout && cookieLife > 0 && (new Date().getTime() - timestamp.getTime()) / 1000 > cookieLife) {
                throw new InvalidDataException("Cookie too old");
            }
        }

        public byte[] getBytes() {
            byte[] bytes = new byte[Long.SIZE * 3];
            SystemKey sysKey = agent.getSystemKey();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            buf.putLong(sysKey.msb);
            buf.putLong(sysKey.lsb);
            buf.putLong(timestamp.getTime());
            return bytes;
        }
    }
}
