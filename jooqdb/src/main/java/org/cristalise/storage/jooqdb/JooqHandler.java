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
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;

public interface JooqHandler {

    public DataType<UUID>    UUID_TYPE    = SQLDataType.UUID;
    public DataType<String>  NAME_TYPE    = SQLDataType.VARCHAR.length(64);
    public DataType<String>  VERSION_TYPE = SQLDataType.VARCHAR.length(64);
    public DataType<String>  STRING_TYPE  = SQLDataType.VARCHAR.length(4096);
    public DataType<Integer> EVENTID_TYPE = SQLDataType.INTEGER;
    public DataType<String>  XML_TYPE     = SQLDataType.CLOB;

    public void createTables(DSLContext context) throws PersistencyException;

    public String[] getNextPrimaryKeys(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException;

    public int put(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    public int update(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    public int delete(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException;

    public int insert(DSLContext context, UUID uuid, C2KLocalObject obj) throws PersistencyException;

    public C2KLocalObject fetch(DSLContext context, UUID uuid, String...primaryKeys) throws PersistencyException;
}
