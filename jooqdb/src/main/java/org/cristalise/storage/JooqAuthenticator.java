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
package org.cristalise.storage;

import static org.jooq.impl.DSL.using;

import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.process.auth.Authenticator;
import org.cristalise.kernel.utils.Logger;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.cristalise.storage.jooqdb.lookup.JooqItemHandler;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

public class JooqAuthenticator implements Authenticator {
    
    DSLContext context = null;

    private JooqItemHandler         items;
    private JooqItemPropertyHandler properties;

    public static DSLContext connect() throws PersistencyException {
        String uri  = Gateway.getProperties().getString(JooqHandler.JOOQ_URI);
        String user = Gateway.getProperties().getString(JooqHandler.JOOQ_USER); 
        String pwd  = Gateway.getProperties().getString(JooqHandler.JOOQ_PASSWORD);

        if (StringUtils.isAnyBlank(uri, user, pwd)) {
            throw new IllegalArgumentException("JOOQ (uri, user, password) config values must not be blank");
        }

        SQLDialect dialect = SQLDialect.valueOf(Gateway.getProperties().getString(JooqHandler.JOOQ_DIALECT, "POSTGRES"));

        Logger.msg(1, "JooqAuthenticator.open() - uri:'"+uri+"' user:'"+user+"' dialect:'"+dialect+"'");

        try {
            return using(DriverManager.getConnection(uri, user, pwd), dialect);
        }
        catch (Exception ex) {
            Logger.error("JooqAuthenticator could not connect to URI '"+uri+"' with user '"+user+"'");
            Logger.error(ex);
            throw new PersistencyException(ex.getMessage());
        }
    }

    @Override
    public boolean authenticate(String resource) throws InvalidDataException, ObjectNotFoundException {
        try {
            context = connect();

            items      = new JooqItemHandler();
            properties = new JooqItemPropertyHandler();

            items     .createTables(context);
            properties.createTables(context);

            return true;
        }
        catch (PersistencyException e) {
            throw new InvalidDataException(e.getMessage());
        }
    }

    @Override
    public boolean authenticate(String agentName, String password, String resource) throws InvalidDataException, ObjectNotFoundException {
        if (context == null) {
            if (!authenticate(resource)) return false;
        }

        List<UUID> is = properties.findItemsByName(context, agentName);

        try {
            items.fetch(context, is.get(0), properties);
        }
        catch (PersistencyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public Object getAuthObject() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnect() {
        context.close();
    }
}
