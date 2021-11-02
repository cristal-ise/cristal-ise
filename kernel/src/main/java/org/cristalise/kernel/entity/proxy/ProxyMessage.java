/**
 * This file is part of the CRISTAL-iSE kernel.
 * Copyright (c) 2001-2015 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.entity.proxy;

import static org.cristalise.kernel.entity.proxy.ProxyMessage.Type.ADD;
import static org.cristalise.kernel.entity.proxy.ProxyMessage.Type.DELETE;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;

import lombok.Getter;
import lombok.Setter;


@Getter @Setter
public class ProxyMessage {

    public enum Type {ADD, DELETE};
    public static final String ebAddress = "cristalise-proxyMessage";

    /**
     * The reference of the changed Item. Can be null for messages of Lookup changes
     */
    private ItemPath itemPath = null;
    /**
     * Either the clusterPath of the changed Item, or DomainPath in case of a Lookup change
     */
    private String path  = "";
    /**
     The type pf the the actual operation
     */
    private Type messageType = ADD;
    /**
     * If the message was from the ClusterStore or from the Lookup DomainPath change
     */
    private boolean clusterStoreMesssage = true;

    public ProxyMessage() {
        super();
    }

    public ProxyMessage(ItemPath itemPath, String path, Type type) {
        this();
        setItemPath(itemPath);
        setPath(path);
        setMessageType(type);
    }

    public ProxyMessage(String line) throws InvalidDataException {
        if (StringUtils.isBlank(line)) throw new InvalidDataException("Blank proxy message");

        String[] tok = line.split(":", 2);

        if (tok.length != 2) {
            throw new InvalidDataException("String '" + line + "' is not a valid proxy message (i.e. ':' is used as separator");
        }

        if (tok[0].length() > 0) {
            if (tok[0].equals("tree")) {
                clusterStoreMesssage = false;
            }
            else {
                try {
                    itemPath = new ItemPath(tok[0]);
                }
                catch (InvalidItemPathException e) {
                    throw new InvalidDataException("Item in proxy message " + line + " was not valid");
                }
            }
        }

        path = tok[1];

        if (path.startsWith("-")) {
            messageType = DELETE;
            path = path.substring(1);
        }
    }

    /**
     * 
     * @return
     */
    public ClusterType getClusterType() {
        if (clusterStoreMesssage) return ClusterType.getValue(path.substring(0, path.indexOf('/')));
        else                      return null;
    }

    /**
     * 
     * @return
     */
    public String getObjectKey() {
        if (clusterStoreMesssage) return path.substring(path.indexOf('/') + 1);
        else                      return path;
    }

    /**
     * 
     * @return
     */
    public String getLocalEventBusAddress() {
        if (clusterStoreMesssage) return itemPath.getName() + "/" + getClusterType();
        else                      return "tree";
    }

    /**
     * 
     * @return
     */
    public String getLocalEventBusMessage() {
        return getObjectKey() + ":" + messageType;
    }

    /**
     * This is also used to create the message sent to the subscribers, therefore the format cannot
     * be changed without changing the parsing 
     */
    @Override
    public String toString() {
        return (itemPath == null ? "tree" : itemPath.getUUID()) + ":" + (messageType == DELETE ? "-" : "") + path;
    }
}
