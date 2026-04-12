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
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

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
import org.json.JSONObject;
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
        if (queryVersion == null) {
            if (Module_Versioning_strict.getBoolean()) {
                throw new InvalidDataException("Version for Query '" + queryName + "' cannot be null");
            }
            else {
                log.warn("executeQuery() - Version for Query {}' was null, using version 0 as default", queryName);
                queryVersion = 0;
            }
        }

        if (queryName != null) {
            try {
                Query query = LocalObjectLoader.getQuery(queryName, queryVersion);

                SecurityManager secMan = Gateway.getSecurityManager();
                AgentProxy agentProxy = (AgentProxy)additionalInputs.get(Script.PARAMETER_AGENT);
                if (null == agentProxy) {
                    throw new AccessRightsException("Input parameter '" + Script.PARAMETER_AGENT + "' was not specified");
                }
                AgentPath agentPath = agentProxy.getPath();
                if (!secMan.checkPermissions(agentPath, BuiltInAction.ACTION_EXECUTE, query.getItemPath(), null)) {
                    throw new AccessRightsException("'" + agentPath.getAgentName() + "' is NOT permitted to " + BuiltInAction.ACTION_EXECUTE + " query: " + query.getName());
                }

                JSONObject json =  new JSONObject(inputJson == null ? "{}" : URLDecoder.decode(inputJson, "UTF-8"));

                CastorHashMap inputs = new CastorHashMap();
                for (String key: json.keySet()) {
                    inputs.put(key, json.get(key));
                }

                inputs.putAll(additionalInputs);

                query.setParemeterValues(null, null, inputs);

                String xmlResult = Gateway.getStorage().executeQuery(query);
                
                boolean jsonFlag = produceJSON(headers.getAcceptableMediaTypes());

                if (jsonFlag) return Response.ok(XML.toJSONObject(xmlResult, true).toString());
                else          return Response.ok(xmlResult);
            }
            catch (UnsupportedOperationException | AccessRightsException | InvalidDataException | ObjectNotFoundException | PersistencyException e) {
                throw e;
            }
            catch (Exception e) {
                log.error("Error executing query", e);
                throw new InvalidDataException("Error executing query: " + e.getMessage());
            }
        }
        else {
            throw new ObjectNotFoundException( "Name or UUID of Query was missing" );
        }
    }
}
