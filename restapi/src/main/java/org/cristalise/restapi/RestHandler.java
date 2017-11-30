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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.xml.bind.DatatypeConverter;

import org.codehaus.jackson.map.ObjectMapper;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.SystemKey;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.InvalidAgentPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.utils.Logger;

public class RestHandler {

    private ObjectMapper mapper;
    private boolean requireLogin = true;

    private static SecretKey cookieKey;
    private static Cipher encryptCipher;
    private static Cipher decryptCipher;

    public static final String COOKIENAME = "cauth";

    public RestHandler() {
        mapper = new ObjectMapper();
        requireLogin = Gateway.getProperties().getBoolean("REST.requireLoginCookie", true);

        if (cookieKey == null) 
            try {
                try {
                    initKeys(256);
                } catch (InvalidKeyException ex) {
                    if (Gateway.getProperties().getBoolean("REST.allowWeakKey", false) == false) {
                        Logger.error(ex);
                        Logger.die("Weak cookie crypto not allowed, and unlimited strength crypto not installed.");
                    }
                    Logger.msg("Unlimited crypto not installed. Trying 128-bit key.");
                    initKeys(128);
                }
            } catch (Exception e) {
                Logger.error(e);
                throw ItemUtils.createWebAppException("Error initializing cookie encryption", e, Response.Status.INTERNAL_SERVER_ERROR);
            } 
    }

    private static void initKeys(int keySize) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(keySize);
        cookieKey = kgen.generateKey();
        System.out.println("Cookie key: "+DatatypeConverter.printBase64Binary(cookieKey.getEncoded()));
        encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, cookieKey);
        decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, cookieKey, new IvParameterSpec(encryptCipher.getIV()));
    }

    private AuthData decryptAuthData(String authData)
            throws InvalidAgentPathException, IllegalBlockSizeException, BadPaddingException, InvalidDataException
    {
        byte[] bytes = DatatypeConverter.parseBase64Binary(authData);
        return new AuthData(decryptCipher.doFinal(bytes));
    }

    protected String encryptAuthData(AuthData auth)
            throws IllegalBlockSizeException, BadPaddingException {
        byte[] bytes = encryptCipher.doFinal(auth.getBytes());
        return DatatypeConverter.printBase64Binary(bytes);
    }

    public Response toJSON(Object data) {
        String childPathDataJSON;
        try {
            childPathDataJSON = mapper.writeValueAsString(data);
        } catch (IOException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Problem building response JSON: ", e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return Response.ok(childPathDataJSON).build();
    }

    /**
     * Authorisation data is decrypted from the cookie
     * 
     * @param authCookie the cookie sent by the client
     * @return AgentPath decrypted from the cookie
     */
    public AgentPath checkAuthCookie(Cookie authCookie) {
        if(authCookie == null) return checkAuthData(null);
        else                   return checkAuthData(authCookie.getValue());
    }

    /**
     * Authorization data is decrypted from the input string and the corresponding AgentPath is returned
     * 
     * @param authData authorisation data normally taken from cookie or token
     * @return AgentPath created from the decrypted autData
     */
    public AgentPath checkAuthData(String authData) {
        if (!requireLogin) return null;

        if (authData == null)
            throw ItemUtils.createWebAppException("Missing authentication data", Response.Status.UNAUTHORIZED);

        try {
            AuthData data = decryptAuthData(authData);
            return data.agent;
        } catch (InvalidAgentPathException | InvalidDataException e) {
            throw ItemUtils.createWebAppException("Invalid agent or login data", e, Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Error reading authentication data", e, Response.Status.UNAUTHORIZED);
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
    public AgentProxy getAgent(String agentName, Cookie authCookie) {
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
    public AgentProxy getAgent(String agentName, String authData) {
        AgentPath agentPath = checkAuthData(authData);

        try {
            if(agentPath == null ) {
                if (agentName != null && !"".equals(agentName)) {
                    agentPath = Gateway.getLookup().getAgentPath(agentName);
                }
                else
                    throw ItemUtils.createWebAppException("Agent is empty", Response.Status.INTERNAL_SERVER_ERROR);
            }

            return (AgentProxy)Gateway.getProxyManager().getProxy(agentPath);
        }
        catch (ObjectNotFoundException e) {
            Logger.error(e);
            throw ItemUtils.createWebAppException("Agent not found", e, Response.Status.NOT_FOUND);
        }
    }

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

            if (cookieLife > 0 && (new Date().getTime() - timestamp.getTime()) / 1000 > cookieLife) {
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
