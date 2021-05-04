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

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.InvalidItemPathException;
import org.cristalise.kernel.lookup.ItemPath;

import lombok.Getter;
import lombok.Setter;


@Getter @Setter
public class ProxyMessage {
    
    public static final String ebAddress = "cristalise-proxyMessage";

    // special server message paths
    public static final String  BYEPATH  = "bye";
    public static final String  ADDPATH  = "add";
    public static final String  DELPATH  = "del";
    public static final String  PINGPATH = "ping";
    public static final boolean ADDED    = false;
    public static final boolean DELETED  = true;

    private ItemPath itemPath = null;
    private String   path     = "";
    private String   server   = null;
    private boolean  state    = ADDED;

    public ProxyMessage() {
        super();
    }

    public ProxyMessage(ItemPath itemPath, String path, boolean state) {
        this();
        setItemPath(itemPath);
        setPath(path);
        setState(state);
    }

    public ProxyMessage(String line) throws InvalidDataException {
        if (StringUtils.isBlank(line)) throw new InvalidDataException("Blank proxy message");

        String[] tok = line.split(":", 2);

        if (tok.length != 2)
            throw new InvalidDataException("String '" + line + "' is not a valid proxy message (i.e. ':' is used as separator");

        if (tok[0].length() > 0 && !tok[0].equals("tree")) {
            try {
                itemPath = new ItemPath(tok[0]);
            }
            catch (InvalidItemPathException e) {
                throw new InvalidDataException("Item in proxy message " + line + " was not valid");
            }
        }
        path = tok[1];

        if (path.startsWith("-")) {
            state = DELETED;
            path = path.substring(1);
        }
    }

    /**
     * This is also used to create the message sent to the subscribers, therfore the format cannot
     * be changed without changing the parsing 
     */
    @Override
    public String toString() {
        return (itemPath == null ? "tree" : itemPath.getUUID()) + ":" + (state ? "-" : "") + path;
    }
}
