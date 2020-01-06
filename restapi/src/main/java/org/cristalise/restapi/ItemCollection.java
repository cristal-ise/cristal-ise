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

import static org.cristalise.kernel.persistency.ClusterType.COLLECTION;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.collection.Dependency;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.entity.proxy.AgentProxy;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilder;
import org.cristalise.kernel.persistency.outcomebuilder.OutcomeBuilderException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.scripting.Script;
import org.cristalise.kernel.scripting.ScriptingEngineException;
import org.cristalise.kernel.utils.LocalObjectLoader;

@Path("/item/{uuid}/collection")
public class ItemCollection extends ItemUtils {
	
	private ScriptUtils scriptUtils = new ScriptUtils();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollections(@PathParam("uuid") String uuid, @CookieParam(COOKIENAME) Cookie authCookie, @Context UriInfo uri) {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        return toJSON(enumerate(item, COLLECTION, "collection", uri, cookie), cookie).build();
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLastCollection(@PathParam("uuid") String uuid, 
                                      @PathParam("name") String collName, 
                                      @CookieParam(COOKIENAME) Cookie authCookie,
                                      @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        try {
            return toJSON(makeCollectionData(item.getCollection(collName), uri), cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @GET
    @Path("{name}/version")
    public Response getCollectionVersions(@PathParam("uuid") String uuid, 
                                         @PathParam("name") String collName,
                                         @CookieParam(COOKIENAME) Cookie authCookie,
                                         @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        return toJSON(enumerate(item, COLLECTION + "/" + collName, "collection/" + collName + "/version", uri, cookie), cookie).build();
    }

    @GET
    @Path("{name}/version/{version}")
    public Response getCollectionVersion(@PathParam("uuid") String uuid, 
                                         @PathParam("name") String collName,
                                         @PathParam("version") String collVersion,
                                         @CookieParam(COOKIENAME) Cookie authCookie,
                                         @Context UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);
        Integer version = collVersion.equals("last") ? null : Integer.valueOf(collVersion);

        try {
            Map<String, Object> collData = makeCollectionData(item.getCollection(collName, version), uri);
            return toJSON(collData, cookie).build();
        }
        catch (ObjectNotFoundException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("{name}/formTemplate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCollectionUpdateFormTemplate(
                        @PathParam("uuid") String uuid, 
                        @PathParam("name") String collName, 
                        @CookieParam(COOKIENAME) Cookie authCookie,
                        @Context UriInfo uri)
    {
        AuthData authData = checkAuthCookie(authCookie);
        NewCookie cookie = checkAndCreateNewCookie(authData);
        ItemProxy item = getProxy(uuid, cookie);

        try {
            Dependency dep = (Dependency) item.getCollection(collName);

            HashMap<String, Object> inputs = new HashMap<>();

            String lovProp = (String) dep.getProperties().get("ListOfValues");

            if (StringUtils.isNotBlank(lovProp)) {
                String[] lovInfo = lovProp.split(":");
                if ("ScriptRef".equals( lovInfo[0] )) {
                    Script script = LocalObjectLoader.getScript(lovInfo[1], Integer.valueOf(lovInfo[2]));
                    Map<? extends String, ? extends Object> result = (Map<? extends String, ? extends Object>) scriptUtils.executeScript(item, script, null);
                    result.remove(null);
                    Map<String, Object> valuesToCaptions = new TreeMap<String, Object>(result);
                    inputs.put("memberNames", valuesToCaptions); // Put the new member here e.g.ListOfValues
                }
            }

            if (inputs.isEmpty()) {
                List<String> names = getItemNames(dep.getClassProperties());

                if (Gateway.getProperties().getBoolean("REST.CollectionForm.checkInputs", false)) {
                    if (names.size() == 0) {
                        throw new WebAppExceptionBuilder()
                                .message("No Item was found")
                                .status(Response.Status.NOT_FOUND)
                                .newCookie(cookie).build();
                    }
                }

                inputs.put("memberNames", names); // Put the new member here e.g.ListOfValues
            }

            AgentProxy agent = Gateway.getProxyManager().getAgentProxy(authData.agent);

            inputs.put(Script.PARAMETER_AGENT, agent);
            inputs.put(Script.PARAMETER_ITEM, item);

            // this shall contain the SchemaName and version like this: Shift:0
            String[] schemaInfo = ((String) dep.getProperties().get("MemberUpdateSchema")).split(":");

            Schema schema = LocalObjectLoader.getSchema(schemaInfo[0], Integer.valueOf(schemaInfo[1]));
            return Response.ok(new OutcomeBuilder(schema, false).generateNgDynamicForms(inputs)).cookie(cookie).build();
        }
        catch (ObjectNotFoundException | NumberFormatException | InvalidDataException | OutcomeBuilderException | ScriptingEngineException e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }
}
