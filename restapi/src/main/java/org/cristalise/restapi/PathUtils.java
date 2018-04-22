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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.RolePath;
import org.cristalise.kernel.process.Gateway;

public class PathUtils extends RestHandler {

    public PathUtils() {
        super();
    }

    protected Map<String, Object> makeLookupData(String path, org.cristalise.kernel.lookup.Path nextPath, UriInfo uri) {
        String name = nextPath.getName();
        String type = "n/a";
        String domainPath = "";
        URI nextPathURI = null;
        UUID uuid = null;
        Boolean hasJoblist = null;

        if (nextPath instanceof DomainPath) {
            type = "domain";
            DomainPath nextDom = (DomainPath) nextPath;
            domainPath = nextDom.getStringPath();
            try {
                ItemPath nextItem = nextDom.getItemPath();
                type = "item";
                nextPathURI = ItemUtils.getItemURI(uri, nextItem.getUUID());
                uuid = nextItem.getUUID();
            }
            catch (ObjectNotFoundException ex) {
                nextPathURI = uri.getAbsolutePathBuilder().path(nextDom.getName()).build();
            }
        }
        else if (nextPath instanceof ItemPath) {
            type = "item";
            if (nextPath instanceof AgentPath) type = "agent";

            ItemPath itemPath = (ItemPath) nextPath;
            uuid = itemPath.getUUID();

            try {
                name = Gateway.getProxyManager().getProxy(itemPath).getName();
            }
            catch (ObjectNotFoundException e) {
                name = itemPath.getUUID().toString();
            }
            nextPathURI = ItemUtils.getItemURI(uri, itemPath);
        }
        else if (nextPath instanceof RolePath) {
            type = "role";
            hasJoblist = ((RolePath) nextPath).hasJobList();

            nextPathURI = uri.getAbsolutePathBuilder().path(nextPath.getName()).build();
        }

        //Now the "json structure" can be created
        LinkedHashMap<String, Object> childPathData = new LinkedHashMap<>();

        childPathData.put("name", name);
        childPathData.put("type", type);
        childPathData.put("url",  nextPathURI);

        if (StringUtils.isNotBlank(domainPath)) {
            childPathData.put("path", domainPath);
        }
        else {
            if (path.equals("/") || StringUtils.isBlank(path)) childPathData.put("path", "/" + name);
            else                                               childPathData.put("path", "/" + path + "/" + name);
        }

        //optional fields
        if (uuid      != null) childPathData.put("uuid", uuid);
        if (hasJoblist!= null) childPathData.put("hasJoblist", hasJoblist);

        return childPathData;
    }

}