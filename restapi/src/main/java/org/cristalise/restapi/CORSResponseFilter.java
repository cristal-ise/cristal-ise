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

import org.cristalise.kernel.process.Gateway;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;

/**
 * Implements ContainerResponseFilter to add CORS headers to each Response.
 */
public class CORSResponseFilter implements ContainerResponseFilter {

    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException
    {
        MultivaluedMap<String, Object> respHeaders = responseContext.getHeaders();

        respHeaders.add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia");
        respHeaders.add("Access-Control-Allow-Origin",  Gateway.getProperties().getString("REST.corsAllowOrigin",  "*"));
        respHeaders.add("Access-Control-Allow-Credentials", Gateway.getProperties().getString("REST.corsAllowCredentials",  "true"));
        respHeaders.add("Access-Control-Allow-Methods", Gateway.getProperties().getString("REST.corsAllowMethods", "GET, POST, OPTIONS"));
    }
}
