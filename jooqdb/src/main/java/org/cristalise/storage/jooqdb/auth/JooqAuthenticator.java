/**
 * This file is part of the CRISTAL-iSE jOOQ Cluster Storage Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.storage.jooqdb.auth;

import java.util.List;
import java.util.UUID;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.cristalise.storage.jooqdb.lookup.JooqItemHandler;
import org.jooq.DSLContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JooqAuthenticator implements Authenticator {

    DSLContext context = null;

    private JooqItemHandler         items;
    private JooqItemPropertyHandler properties;

    private Argon2Password paswordHasher;

    @Override
    public boolean authenticate(String resource) throws InvalidDataException, ObjectNotFoundException {
        try {
            context = JooqHandler.connect();

            items      = new JooqItemHandler();
            properties = new JooqItemPropertyHandler();

            if (!JooqHandler.readOnlyDataSource) {
                items     .createTables(context);
                properties.createTables(context);
            }

            paswordHasher = new Argon2Password();

            return true;
        }
        catch (PersistencyException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }

    private UUID getAgentUUID(String agentName) throws ObjectNotFoundException {
        List<UUID> uuids = properties.findItemsByName(context, agentName);

        if (uuids.size() == 0) throw new ObjectNotFoundException("Could not find agent name:"+agentName);
        if (uuids.size() != 1) throw new ObjectNotFoundException("Ambiguous agent name:"+agentName);

        try {
            ItemPath item = items.fetch(context, uuids.get(0), properties);

            if (item instanceof AgentPath) return item.getUUID();
            else                           throw new ObjectNotFoundException("'"+agentName + " is not an Agent");
        }
        catch (PersistencyException e) {
            throw new ObjectNotFoundException("Could not retrieve agentName:"+agentName + " error:"+e.getMessage());
        }
    }

    @Override
    public boolean authenticate(String agentName, String password, String resource) throws InvalidDataException, ObjectNotFoundException {
        if (context == null) if (!authenticate(resource)) return false;

        try {
            String pwd = items.fetchPassword(context, getAgentUUID(agentName));
            return paswordHasher.checkPassword(pwd, password.toCharArray());
        }
        catch (PersistencyException e) {
            log.error("", e);
            throw new InvalidDataException("Problem authenticating agent:"+agentName+" error:"+e.getMessage());
        }   
    }

    @Override
    public Object getAuthObject() {
        if (context == null) {
            try {
                authenticate(null);
            }
            catch (InvalidDataException | ObjectNotFoundException e) {
                log.error("", e);
            }
        }
        return context;
    }

    @Override
    public void disconnect() {
        context.close();
    }
}
