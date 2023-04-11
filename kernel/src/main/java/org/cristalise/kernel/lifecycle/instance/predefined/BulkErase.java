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
package org.cristalise.kernel.lifecycle.instance.predefined;

import static org.cristalise.kernel.SystemProperties.BulkErase_force;
import static org.cristalise.kernel.SystemProperties.BulkErase_limit;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.DomainPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.lookup.Lookup.PagedResult;
import org.cristalise.kernel.lookup.Path;
import org.cristalise.kernel.lookup.SearchFilter;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BulkErase extends Erase {
    public static final String description =  "Deletes all Items selected bz the SearchFilter : Root Domainpath and list of Properties";

    private static final int LIMIT = BulkErase_limit.getInteger();
    private static final boolean FORCE_FLAG = BulkErase_force.getBoolean();

    public BulkErase() {
        super("SearchFilter", description);
    }

    /**
     * {@value #description}}
     * 
     * @param requestData is empty
     */
    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, PersistencyException, InvalidCollectionModification
    {
        SearchFilter filter = (SearchFilter) Gateway.getMarshaller().unmarshall(requestData);

        log.debug("runActivityLogic() - item:{} filter:{} limit:{} forceFlag:{}", item, filter, LIMIT, FORCE_FLAG);

        int recordsDeleted = eraseAllItemsOfSearch(agent, filter, transactionKey, 0, LIMIT);
        filter.setRecordsFound(recordsDeleted);

        log.info("runActivityLogic() - DONE #{} items with filter:{}", filter.getRecordsFound(), filter);

        return Gateway.getMarshaller().marshall(filter);
    }

    private int eraseAllItemsOfSearch(AgentPath agent, SearchFilter filter, TransactionKey transactionKey, final int offset, final int limit)
            throws ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, InvalidDataException, PersistencyException, InvalidCollectionModification
    {
        PagedResult result = Gateway.getLookup().search(new DomainPath(filter.getSearchRoot()), filter.getProperties(), offset, limit);

        log.debug("eraseAllItemsOfSearch() - offset:{} maxRows:{}", offset, result.maxRows);

        //something seriously wrong, it seems result.maxRows does not change when using delete with limit and offset
        if (limit != 0 && offset > result.maxRows) {
            String msg = "Serious inconsistency - offset:" + offset + " > maxRows:" + result.maxRows;
            log.error("eraseAllItemsOfSearch() - {}", msg);
            throw new InvalidDataException(msg);
        }

        for (Path p : result.rows) {
            ItemPath item = ((DomainPath)p).getTarget();
            eraseOneItem(agent, item, FORCE_FLAG, transactionKey);
        }

        if (limit == 0) {
            // maxRows is not populated when limit == 0 (i.e. no paging was done)
            return result.rows.size();
        }
        else if (result.maxRows > LIMIT) {
            // there are more pages to read and delete
            int recordDeleted = eraseAllItemsOfSearch(agent, filter, transactionKey, offset + LIMIT, LIMIT);
            return recordDeleted + LIMIT;
        }
        else {
            // no more pages to read
            return result.maxRows;
        }
    }
}
