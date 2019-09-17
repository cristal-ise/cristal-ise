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

import java.net.URLDecoder;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.cristalise.kernel.utils.Logger;
import org.json.JSONObject;
import org.json.XML;

public class ScriptUtils extends ItemUtils {
    
    static Semaphore mutex = new Semaphore(1);
    
    public ScriptUtils() {
        super();
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

    public Response.ResponseBuilder executeScript(HttpHeaders headers, ItemProxy item, String scriptName, Integer scriptVersion,
            String inputJson, Map<String, Object> additionalInputs) throws InvalidDataException, ObjectNotFoundException, UnsupportedOperationException
    {
        if (scriptVersion == null) {
            if ( Gateway.getProperties().getBoolean("Module.Versioning.strict", true)) {
                throw new InvalidDataException("Version for Script '" + scriptName + "' cannot be null");
            }
            else {
                Logger.warning("ScriptUtils.executeScript() - Version for Script '%s' was null, using version 0 as default", scriptName);
                scriptVersion = 0;
            }
        }

        Script script = null;
        if (scriptName != null) {
            try {
                script = LocalObjectLoader.getScript(scriptName, scriptVersion);

                JSONObject json =  new JSONObject(inputJson == null ? "{}" : URLDecoder.decode(inputJson, "UTF-8"));

                CastorHashMap inputs = new CastorHashMap();
                for (String key: json.keySet()) {
                    inputs.put(key, json.get(key));
                }
                inputs.putAll(additionalInputs);
                
                return returnScriptResult(scriptName, item, null, script, inputs, produceJSON(headers.getAcceptableMediaTypes()));
            }
            catch ( UnsupportedOperationException e ) {
                throw e;
            }
            catch (Exception e) {
                Logger.error(e);
                if (e.getMessage().contains("[errorMessage]")) {
                    throw new ObjectNotFoundException( e.getMessage() );
                } else {
                    // Throw the generic exception when error message is not defined in the script.
                    throw new ObjectNotFoundException( "Error executing script, please contact support" );
                }
            }
        }
        else {
            throw new ObjectNotFoundException( "Name or UUID of Script was missing" );
        }
    }

    public Response.ResponseBuilder returnScriptResult(String scriptName, ItemProxy item, final Schema schema, final Script script, CastorHashMap inputs, boolean jsonFlag)
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
     * @throws ObjectNotFoundException
     * @throws ScriptingEngineException
     * @throws InvalidDataException
     */
    protected Response.ResponseBuilder runScript(String scriptName, ItemProxy item, final Schema schema, final Script script, CastorHashMap inputs, boolean jsonFlag)
            throws ScriptingEngineException, InvalidDataException, ObjectNotFoundException
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
        else {
            throw new ObjectNotFoundException( "Cannot handle result of script:" + scriptName );
        }


        if (xmlOutcome == null) {
            throw new ObjectNotFoundException( "Cannot handle result of script:" + scriptName );
        }


        if (schema != null) return getOutcomeResponse(new Outcome(xmlOutcome, schema), new Date(), jsonFlag, null);
        else {
            if (jsonFlag) return Response.ok(XML.toJSONObject(xmlOutcome, true).toString());
            else          return Response.ok((xmlOutcome));
        }
    }
}
