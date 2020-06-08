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

import static org.cristalise.kernel.persistency.ClusterType.ATTACHMENT;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;

import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.persistency.outcome.OutcomeAttachment;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("/item/{uuid}/attachment")
public class ItemOutcomeAttachment extends ItemUtils {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOutcomeSchemas(@PathParam("uuid")       String uuid,
                                      @CookieParam(COOKIENAME) Cookie authCookie,
                                      @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        return toJSON(enumerate(item, ATTACHMENT, "attachment", uri, cookie), cookie).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}")
    public Response getOutcomeVersions(@PathParam("uuid")       String  uuid,
                                       @PathParam("schema")     String  schema,
                                       @CookieParam(COOKIENAME) Cookie  authCookie,
                                       @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        return toJSON(enumerate(item, ATTACHMENT + "/" + schema, "attachment/" + schema, uri, cookie), cookie).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{schema}/{version}")
    public Response getOutcomeEvents(@PathParam("uuid")       String  uuid,
                                     @PathParam("schema")     String  schema,
                                     @PathParam("version")    Integer version,
                                     @CookieParam(COOKIENAME) Cookie  authCookie,
                                     @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        return toJSON(enumerate(item, ATTACHMENT+"/"+schema+"/"+version, "attachment/"+schema+"/"+version, uri, cookie), cookie).build();
    }

    @GET
    @Produces( {MediaType.APPLICATION_OCTET_STREAM})
    @Path("{schema}/{version}/{event}")
    public Response queryBinaryData(@PathParam("uuid")       String  uuid,
                                    @PathParam("schema")     String  schema,
                                    @PathParam("version")    Integer version,
                                    @PathParam("event")      Integer event,
                                    @QueryParam("inline")    String  inline,
                                    @CookieParam(COOKIENAME) Cookie  authCookie,
                                    @Context                 UriInfo uri)
    {
        NewCookie cookie = checkAndCreateNewCookie(checkAuthCookie(authCookie));
        ItemProxy item = getProxy(uuid, cookie);

        log.debug("queryBinaryData() - {}/{}/{}/{}", item, schema, version, event);

        try {
            OutcomeAttachment attachment = item.getOutcomeAttachment(schema, version, event);
            String fileName = attachment.getFileName();
            String mimeType = MediaType.APPLICATION_OCTET_STREAM;
            String contentDisposition = "attachment;";

            if (StringUtils.isNotBlank(fileName)) {
                if (inline != null) {
                    contentDisposition = "inline;";
                    if ("guessContent".equals(inline)) {
                        mimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(attachment.getBinaryData()));
                    }
                    else{
                        mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
                    }
                }
                contentDisposition += " filename=\"" + fileName + "\"";

                log.debug("queryBinaryData() - {} : {}", mimeType, contentDisposition);
            }

            return Response
                    .ok(attachment.getBinaryData())
                    .type(mimeType)
                    .header("Content-Disposition", contentDisposition)
                    .build();
        }
        catch (Exception e) {
            throw new WebAppExceptionBuilder().exception(e).newCookie(cookie).build();
        }
    }
}
