package org.cristalise.restapi;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;

public class PathUtils extends RestHandler {

    public PathUtils() {
        super();
    }

    protected Map<String, Object> makeLookupData(String path, org.cristalise.kernel.lookup.Path nextPath, UriInfo uri) {
        String name = nextPath.getName();
        URI nextPathURI = null;
        UUID uuid = null;

        if (nextPath instanceof DomainPath) {
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
    
        //Now the "json structure" can be created
        LinkedHashMap<String, Object> childPathData = new LinkedHashMap<>();

        childPathData.put("name",   name);
        childPathData.put("url",    nextPathURI);

        if (uuid != null) childPathData.put("uuid", uuid);

        if (path.equals("/")) childPathData.put("path", nextPath.getName());
        else                  childPathData.put("path", path + "/" + nextPath.getName());

        return childPathData;
    }

}