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

import static org.cristalise.kernel.SystemProperties.Module_Versioning_strict;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cristalise.kernel.common.AccessRightsException;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.querying.Query;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.security.SecurityManager;
import org.cristalise.kernel.security.SecurityManager.BuiltInAction;
import org.cristalise.kernel.utils.CastorHashMap;
import org.cristalise.kernel.utils.LocalObjectLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.XML;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryUtils extends ItemUtils {

    public QueryUtils() {
        super();
    }

    public Response.ResponseBuilder executeQuery(
            HttpHeaders         headers,
            String              queryName,
            Integer             queryVersion,
            String              inputJson,
            Map<String, Object> additionalInputs)
                throws ObjectNotFoundException, UnsupportedOperationException, InvalidDataException, PersistencyException, AccessRightsException
    {
        if (queryName == null) {
            throw new ObjectNotFoundException("Name or UUID of Query was missing");
        }

        queryVersion = getQueryVersion(queryName, queryVersion);

        try {
            Query query = LocalObjectLoader.getQuery(queryName, queryVersion);
            checkPermissions(query, additionalInputs);

            log.info("executeQuery() - query:{}", query.getName());

            CastorHashMap inputs = parseInputs(inputJson, additionalInputs);
            query.setParemeterValues(null, null, inputs);

            String xmlResult = Gateway.getStorage().executeQuery(query);

            return createResponse(headers, query, xmlResult);
        }
        catch (UnsupportedOperationException | AccessRightsException | InvalidDataException | ObjectNotFoundException | PersistencyException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Error executing query", e);
            throw new InvalidDataException("Error executing query: " + e.getMessage(), e);
        }
    }

    private void checkPermissions(Query query, Map<String, Object> additionalInputs) throws AccessRightsException, ObjectNotFoundException {
        AgentProxy agentProxy = (AgentProxy) additionalInputs.get(Script.PARAMETER_AGENT);
        if (agentProxy == null) {
            throw new AccessRightsException("Input parameter '" + Script.PARAMETER_AGENT + "' was not specified");
        }

        AgentPath agentPath = agentProxy.getPath();
        SecurityManager secMan = Gateway.getSecurityManager();
        if (!secMan.checkPermissions(agentPath, BuiltInAction.ACTION_EXECUTE, query.getItemPath(), null)) {
            throw new AccessRightsException("'" + agentPath.getAgentName() + "' is NOT permitted to " + BuiltInAction.ACTION_EXECUTE + " query: " + query.getName());
        }
    }

    private CastorHashMap parseInputs(String inputJson, Map<String, Object> additionalInputs) throws Exception {
        CastorHashMap inputs = new CastorHashMap();
        if (inputJson != null && !inputJson.isEmpty()) {
            JSONObject json = new JSONObject(URLDecoder.decode(inputJson, StandardCharsets.UTF_8));
            json.keySet().forEach(key -> inputs.put(key, json.get(key)));
        }

        if (additionalInputs != null) inputs.putAll(additionalInputs);
        
        return inputs;
    }

    private Response.ResponseBuilder createResponse(HttpHeaders headers, Query query, String xmlResult) {
        if (produceJSON(headers.getAcceptableMediaTypes())) {
            JSONObject resultJson = XML.toJSONObject(xmlResult, true);
            String recordPointer = "/" + query.getRootElement() + "/" + query.getRecordElement();
            var records = resultJson.query(recordPointer);

            if (records instanceof JSONObject singleRecordJson) {
                // workaround for XML.toJSONObject limitation of creating jsonArray for single record
                JSONObject rootJson      = new JSONObject().put(query.getRecordElement(), new JSONArray().put(singleRecordJson));
                JSONObject newResultJson = new JSONObject().put(query.getRootElement(), rootJson);

                log.info("createResponse() - query:{} returning single record as JSON array", query.getName());
                return Response.ok(newResultJson.toString());
            } else {
                log.info("createResponse() - query:{} returning #{} of records", query.getName(), ((JSONArray)records).length());
                return Response.ok(resultJson.toString());
            }
        } else {
            return Response.ok(xmlResult);
        }
    }

    private @NonNull Integer getQueryVersion(String queryName, Integer queryVersion) throws InvalidDataException {
        if (queryVersion == null) {
            if (Module_Versioning_strict.getBoolean()) {
                throw new InvalidDataException("Version for Query '" + queryName + "' cannot be null");
            } else {
                log.warn("getQueryVersion() - Version for Query {}' was null, using version 0 as default", queryName);
                return  0;
            }
        } else {
            return queryVersion;
        }
    }
}
