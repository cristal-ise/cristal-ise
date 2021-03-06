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

import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.C2KLocalObject;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.ClusterType;
import org.cristalise.kernel.persistency.TransactionKey;

/**
 * Use this class in clients for read only access to the database
 *
 */
public class JooqClientReader extends JooqClusterStorage {
    @Override
    public short queryClusterSupport(ClusterType type) {
        return READ;
    }

    @Override
    public String getName() {
        return getId()+" ClientReader";
    }

    @Override
    public String getId() {
        return "JOOQCLIENT:"+JooqDataSourceHandler.dialect;
    }

    @Override
    public void put(ItemPath itemPath, C2KLocalObject obj, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("Writing not supported in JooqClientReader");
    }

    @Override
    public void delete(ItemPath itemPath, String path, TransactionKey transactionKey) throws PersistencyException {
        throw new PersistencyException("Delete not supported in JooqClientReader");
    }
}