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
package org.cristalise.storage.jooqdb.lookup;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.property.BuiltInItemProperties;
import org.cristalise.kernel.property.Property;
import org.cristalise.storage.jooqdb.JooqHandler;
import org.cristalise.storage.jooqdb.clusterStore.JooqItemPropertyHandler;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

/**
 * Table to store data of ItemPath and AgentPath
 */
public class JooqItemHandler {
    static final Table<?> ITEM_TABLE = table(name("ITEM"));

    static final Field<UUID>    UUID     = field(name("UUID"),     UUID.class);
    static final Field<String > IOR      = field(name("IOR"),      String.class);
    static final Field<Boolean> IS_AGENT = field(name("IS_AGENT"), Boolean.class);
    static final Field<String>  PASSWORD = field(name("PASSWORD"), String.class);

    public int update(DSLContext context, ItemPath path) throws PersistencyException {
        boolean isAgent = path instanceof AgentPath;
        String pwd = null;
        if (isAgent) pwd = ((AgentPath)path).getPassword();

        return context
                .update(ITEM_TABLE)
                .set(IOR,      path.getIORString())
                .set(IS_AGENT, isAgent)
                .set(PASSWORD, pwd)
                .where(UUID.equal(path.getUUID()))
                .execute();
    }

    public int delete(DSLContext context, UUID uuid) throws PersistencyException {
        return context
                .delete(ITEM_TABLE)
                .where(UUID.equal(uuid))
                .execute();
    }

    public int insert(DSLContext context, AgentPath agentPath, JooqItemPropertyHandler properties) throws PersistencyException {
        int rows = context
                .insertInto(ITEM_TABLE)
                    .set(UUID,     agentPath.getUUID())
                    .set(IOR,      agentPath.getIORString())
                    .set(IS_AGENT, true)
                    .set(PASSWORD, agentPath.getPassword())
                .execute();

        if (rows != 1) throw new PersistencyException("Insert into ITEM table rows:"+rows);

        rows = properties.insert(context, agentPath.getUUID(), new Property(BuiltInItemProperties.NAME, agentPath.getAgentName()));

        if (rows != 1) throw new PersistencyException("Insert into ITEM_PROPERTY table rows:"+rows);

        return rows;
    }

    public int insert(DSLContext context, ItemPath path) throws PersistencyException {
        return context
                .insertInto(ITEM_TABLE)
                    .set(UUID,     path.getUUID())
                    .set(IOR,      path.getIORString())
                    .set(IS_AGENT, false)
                    .set(PASSWORD, (String)null)
                .execute();
    }

    public boolean exists(DSLContext context, UUID uuid) throws PersistencyException {
        Record1<Integer> count = context
                .selectCount().from(ITEM_TABLE)
                .where(UUID.equal(uuid))
                .fetchOne();

        return count != null && count.get(0, Integer.class) == 1;
    }

    public ItemPath fetch(DSLContext context, UUID uuid, JooqItemPropertyHandler properties) throws PersistencyException {
        Record result = context
                .select().from(ITEM_TABLE)
                .where(UUID.equal(uuid))
                .fetchOne();

        if(result != null) {
            boolean isAgent = result.get(IS_AGENT);
            String  ior     = result.get(IOR);
            String  pwd     = result.get(PASSWORD);

            if(isAgent) {
                AgentPath ap = new AgentPath(uuid, ior, pwd);
                
                Property prop = (Property)properties.fetch(context, uuid, "Name");
                ap.setAgentName(prop.getValue());

                return ap;
            }
            else
                return new ItemPath(uuid, ior);
        }
        return null;
    }

    public void createTables(DSLContext context) throws PersistencyException {
        context.createTableIfNotExists(ITEM_TABLE)
            .column(UUID,     JooqHandler.UUID_TYPE    .nullable(true))
            .column(IOR,      JooqHandler.STRING_TYPE  .nullable(true))
            .column(IS_AGENT, SQLDataType.BOOLEAN      .nullable(false))
            .column(PASSWORD, JooqHandler.STRING_TYPE  .nullable(true))
            .constraints(
                    constraint("PK_"+ITEM_TABLE).primaryKey(UUID))
        .execute();
    }
}
