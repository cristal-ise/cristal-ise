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

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.cristalise.kernel.process.resource.BuiltInResources.SCRIPT_RESOURCE;

import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.json.JSONObject;
import org.json.XML;

import com.google.common.collect.ImmutableMap;

@Path("/script")
public class ScriptAccess extends ResourceAccess {

    static Semaphore mutex = new Semaphore(1);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listAllScripts(
            @DefaultValue("0") @QueryParam("start") Integer start,
            @QueryParam("batch")                    Integer batchSize,
            @CookieParam(COOKIENAME)                Cookie  authCookie,
            @Context                                UriInfo uri)
    {
        checkAuthCookie(authCookie);

        if (batchSize == null) batchSize = Gateway.getProperties().getInt("REST.DefaultBatchSize", 75);

        return listAllResources(SCRIPT_RESOURCE, uri, start, batchSize);
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listScriptVersions(
            @PathParam("name")       String  name,
            @CookieParam(COOKIENAME) Cookie  authCookie, 
            @Context                 UriInfo uri)
    {
        checkAuthCookie(authCookie);
        return listResourceVersions(SCRIPT_RESOURCE, name, uri);
    }

    @GET
    @Path("{name}/{version}")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getScript(
            @Context                 HttpHeaders headers,
            @PathParam("name")       String      name, 
            @PathParam("version")    Integer     version, 
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        checkAuthCookie(authCookie);
        return getResource(SCRIPT_RESOURCE, name, version, produceJSON(headers.getAcceptableMediaTypes()));
    }
    
    @GET
    @Path("scriptResult")
    @Produces( {MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON } )
    public Response getScriptResult(
            @Context                 HttpHeaders headers,
            @QueryParam("script")    String      scriptName,
            @QueryParam("version")   Integer     scriptVersion,
            @QueryParam("inputs")    String      inputJson,
            @CookieParam(COOKIENAME) Cookie      authCookie)
    {
        checkAuthCookie(authCookie);
        
        try (DSLContext context = JooqHandler.connect()) {
            return executeScript(headers, null, scriptName, scriptVersion, inputJson,
            		ImmutableMap.of("dsl", context));
        } catch (DataAccessException | PersistencyException e) {
            throw ItemUtils.createWebAppException("Error connecting to database, please contact support", e, Response.Status.NOT_FOUND);
		}
    }

	public Response executeScript(HttpHeaders headers, ItemProxy item, String scriptName, Integer scriptVersion,
			String inputJson, Map<String, Object> additionalInputs) {
		// FIXME: version should be retrieved from the current item or the Module
        // String view = "last";
        if (scriptVersion == null) scriptVersion = 0;

        Script script = null;
        if (scriptName != null) {
            try {
                script = LocalObjectLoader.getScript(scriptName, scriptVersion);
                
                JSONObject json = 
                		new JSONObject(
                				inputJson == null ? "{}" : URLDecoder.decode(inputJson, "UTF-8"));
                
                CastorHashMap inputs = new CastorHashMap();
                for (String key: json.keySet()) {
                	inputs.put(key, json.get(key));
                }
                inputs.putAll(additionalInputs);
                
                return returnScriptResult(scriptName, item, null, script, inputs, produceJSON(headers.getAcceptableMediaTypes()));
            }
            catch (Exception e) {
                Logger.error(e);
                throw ItemUtils.createWebAppException("Error executing script, please contact support", e, Response.Status.NOT_FOUND);
            }
        }
        else {
            throw ItemUtils.createWebAppException("Name or UUID of Script was missing", Response.Status.NOT_FOUND);
        }
	}

    public Response returnScriptResult(String scriptName, ItemProxy item, final Schema schema, final Script script, CastorHashMap inputs, boolean jsonFlag)
            throws ScriptingEngineException, InvalidDataException
    {
        try {
            mutex.acquire();
            return runScript(scriptName, item, schema, script, inputs, jsonFlag);
        }
        catch (ScriptingEngineException e) {
            throw e;
        }
        catch (Exception e) {
            throw new InvalidDataException(e.getMessage());
        }
        finally {
            mutex.release();
        }
    }
    
    /**
     * 
     * @param scriptName
     * @param item
     * @param schema
     * @param script
     * @param jsonFlag whether the response is a JSON or XML
     * @return
     * @throws ScriptingEngineException
     * @throws InvalidDataException
     */
    protected Response runScript(String scriptName, ItemProxy item, final Schema schema, final Script script, CastorHashMap inputs, boolean jsonFlag)
            throws ScriptingEngineException, InvalidDataException
    {
        String xmlOutcome = null;
        Object scriptResult = executeScript(item, script, inputs);

        if (scriptResult instanceof String) {
            xmlOutcome = (String)scriptResult;
        }
        else if (scriptResult instanceof Map) {
            //the map shall have one Key only
            String key = ((Map<?,?>) scriptResult).keySet().toArray(new String[0])[0];
            xmlOutcome = (String)((Map<?,?>) scriptResult).get(key);
        }
        else
            throw ItemUtils.createWebAppException("Cannot handle result of script:" + scriptName, NOT_FOUND);

        if (xmlOutcome == null)
            throw ItemUtils.createWebAppException("Cannot handle result of script:" + scriptName, NOT_FOUND);

        if (schema != null) return getOutcomeResponse(new Outcome(xmlOutcome, schema), new Date(), jsonFlag);
        else {
            if (jsonFlag) return Response.ok(XML.toJSONObject(xmlOutcome).toString()).build();
            else          return Response.ok((xmlOutcome)).build();
        }
    }

    /**
     * 
     * @param item
     * @param script
     * @return
     * @throws ScriptingEngineException
     * @throws InvalidDataException
     */
    protected Object executeScript(ItemProxy item, final Script script, CastorHashMap inputs)
            throws ScriptingEngineException, InvalidDataException {
        
        Object scriptResult = null;
        try {
            scriptResult = script.evaluate(item == null ? script.getItemPath() : item.getPath(), inputs, null, null);
        }
        catch (ScriptingEngineException e) {
            throw e;
        }
        catch (Exception e) {
            throw new InvalidDataException(e.getMessage());
        }
        return scriptResult;
    }

}
