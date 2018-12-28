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
package org.cristalise.storage.jooqdb;

import java.util.UUID;

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.persistency.outcome.Outcome;
import org.jooq.DSLContext;

/**
 * Provides mechanism to update application(domain) specific table(s) during the transaction
 * of storing an Outcome. 
 */
public interface JooqDomainHandler {
    
    /**
     * This method is called each time the connection is created. The implementation could ignore this or
     * use <code>context.createTableIfNotExists(TABLE)</code> to initialise database tables gracefully.
     * 
     * @param context The configured DSLContext of jooq
     * @throws PersistencyException throw this exception in case of any error that requires to abort a transaction
     */
    public void createTables(DSLContext context) throws PersistencyException;

    /**
     * This method is called each time an Outcome is stored.
     * 
     * @param context The configured DSLContext of jooq
     * @param uuid the Item's UUID
     * @param schemaName the schema name of the item
     * @return the number of rows created/updated
     * @throws PersistencyException throw this exception in case of any error that requires to abort a transaction
     */
    public int put(DSLContext context, UUID uuid, String schemaName, Object locker) throws PersistencyException;
    
    /**
     * This method is called each time anything but an Outcome is stored.
     * 
     * @param context The configured DSLContext of jooq
     * @param uuid the Item's UUID
     * @param obj Object that is being stored
     * @return the number of rows created/updated
     * @throws PersistencyException throw this exception in case of any error that requires to abort a transaction
     */
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj, Object locker) throws PersistencyException;

    /**
     * This method is called each time an Outcome is deleted.
     * 
     * @param context The configured DSLContext of jooq
     * @param uuid the Item's UUID
     * @param primaryKeys the identifiers of the Outcome withing the Item
     * @return the number of rows deleted
     * @throws PersistencyException throw this exception in case of any error that requires to abort a transaction
     */
    public int delete(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException;
}
