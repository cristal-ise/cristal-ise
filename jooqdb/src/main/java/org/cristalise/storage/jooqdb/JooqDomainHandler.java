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
import org.cristalise.kernel.persistency.TransactionKey;
import org.jooq.DSLContext;

/**
 * Provides mechanism to update application(domain) specific table(s) during the transaction.
 * @since 3.6.0
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
     * This method is called each time a C2KLocalObject is stored.
     * 
     * @param context The configured DSLContext of jooq
     * @param uuid the Item's UUID
     * @param obj Object that is being stored
     * @param transactionKey transaction key
     * @return the number of rows created/updated
     * @throws PersistencyException throw this exception in case of any error that requires to abort a transaction
     */
    public int put(DSLContext context, UUID uuid, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException;

    /**
     * This method is called each time a C2KLocalObject is deleted.
     * 
     * @param context The configured DSLContext of jooq
     * @param uuid the Item's UUID
     * @param transactionKey transaction key
     * @param primaryKeys the identifiers of the Outcome withing the Item
     * @return the number of rows deleted
     * @throws PersistencyException throw this exception in case of any error that requires to abort a transaction
     */
    public int delete(DSLContext context, UUID uuid, TransactionKey transactionKey, String...primaryKeys) throws PersistencyException;

    /**
     * Called each time the cristal-ise transaction is comitted
     * 
     * @param context The configured DSLContext of jooq
     * @param transactionKey transaction key
     */
    public void commit(DSLContext context, TransactionKey transactionKey);

    /**
     * Called each time the cristal-ise transaction is aborted
     * 
     * @param context The configured DSLContext of jooq
     * @param transactionKey transaction key
     */
    public void abort(DSLContext context, TransactionKey transactionKey);

    /**
     * Called when the cristal-ise bootstrap has finished
     * 
     * @param context The configured DSLContext of jooq
     * @throws PersistencyException Database error
     */
    public void postBoostrap(DSLContext context) throws PersistencyException;

    /**
     * Called when the cristal-ise start server has finished
     * 
     * @param context The configured DSLContext of jooq
     * @throws PersistencyException Database error
     */
    public void postStartServer(DSLContext context) throws PersistencyException;

    /**
     * Called when the cristal-ise connect has finished
     * 
     * @param context The configured DSLContext of jooq
     * @throws PersistencyException Database error
     */
    public void postConnect(DSLContext context) throws PersistencyException;
}
