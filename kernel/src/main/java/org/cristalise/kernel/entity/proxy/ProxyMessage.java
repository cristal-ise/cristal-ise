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

import java.util.Arrays;

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
    public static final String ebLocalAddress = "cristalise-localProxyMessage";

    /**
     * The reference of the changed Item. Can be null for messages of Lookup changes
     */
    private ItemPath itemPath = null;
    /**
     * Either the clusterPath of the changed Item, or DomainPath in case of a Lookup change
     */
    private String path  = "";
    /**
     The type of the the actual change
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

    /**
     * Parses the message string using ':' as separator.
     * 
     * @param message the string containing the message
     * @throws InvalidDataException wrong message format
     */
    public ProxyMessage(String message) throws InvalidDataException {
        if (StringUtils.isBlank(message)) throw new InvalidDataException("Blank proxy message");

        String[] msgSections = message.split(":", 2);

        if (msgSections.length != 2) {
            throw new InvalidDataException("Invalid proxy message (use ':' as separator):'"+Arrays.toString(msgSections)+"'");
        }

        if (StringUtils.isNotBlank(msgSections[0])) {
            if (msgSections[0].equals("tree")) {
                clusterStoreMesssage = false;
            }
            else {
                try {
                    itemPath = new ItemPath(msgSections[0]);
                }
                catch (InvalidItemPathException e) {
                    throw new InvalidDataException("Invalid UUID in proxy message:'"+Arrays.toString(msgSections)+"'", e);
                }
            }
        }

        if (msgSections[1].startsWith("-")) {
            messageType = DELETE;
            path = msgSections[1].substring(1);
        }
        else {
            path = msgSections[1];
        }
    }

    /**
     * 
     * @return
     */
    public ClusterType getClusterType() {
        if (clusterStoreMesssage) {
            int slashIdx = path.indexOf('/');

            if (slashIdx != -1) return ClusterType.getValue(path.substring(0, slashIdx));
            else                return ClusterType.ROOT;
        }
        else {
            return null;
        }
    }

    /**
     * The key of the object within the Cluster
     * @return the ClusterPath without the ClusterType prefix
     */
    public String getObjectKey() {
        if (clusterStoreMesssage) {
            int slashIdx = path.indexOf('/');

            if (slashIdx != -1) return path.substring(slashIdx + 1);
            else                return path;
        }
        else {
            return path;
        }
    }

    /**
     * Constructs the UUID/ClusterType local address to be used to send or publish the change notification messages
     * 
     * @return returns concatenated string of UUID/ClusterType
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
