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
package org.cristalise.kernel.security;

import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter @Slf4j
public enum BuiltInAuthc {

    /**
     * Built in Admin role
     */
    ADMIN_ROLE("Admin"), 

    /**
     * Built in 'system' agent
     */
    SYSTEM_AGENT("system");

    private String name;

    private BuiltInAuthc(final String n) {
        name = n;
    }

    public Path getPath() throws ObjectNotFoundException {
        return getPath(null);
    }

    public Path getPath(TransactionKey transactionKey) throws ObjectNotFoundException {
        if      (this.equals(SYSTEM_AGENT)) return Gateway.getLookup().getAgentPath(name, transactionKey);
        else if (this.equals(ADMIN_ROLE))   return Gateway.getLookup().getRolePath(name, transactionKey);
        else {
            log.warn("getPath() - Enum value '{}' is not handled correctly", name);
            return null;
        }
    }
}
