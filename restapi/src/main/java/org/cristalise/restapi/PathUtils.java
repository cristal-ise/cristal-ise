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
        URI nextPathURI = null;
        UUID uuid = null;
        Boolean hasJoblist = null;

        if (nextPath instanceof DomainPath) {
            type = "domain";
            DomainPath nextDom = (DomainPath) nextPath;
            try {
                ItemPath nextItem = nextDom.getItemPath();
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

        if (path.equals("/") || StringUtils.isBlank(path)) childPathData.put("path", "/" + name);
        else                                               childPathData.put("path", "/" + path + "/" + name);

        //optional fields
        if (uuid      != null) childPathData.put("uuid", uuid);
        if (hasJoblist!= null) childPathData.put("hasJoblist", hasJoblist);

        return childPathData;
    }

}