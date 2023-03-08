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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.SCHEMA_NAME;

import java.io.IOException;

import org.cristalise.kernel.common.CannotManageException;
import org.cristalise.kernel.common.InvalidCollectionModification;
import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.common.ObjectAlreadyExistsException;
import org.cristalise.kernel.common.ObjectCannotBeUpdated;
import org.cristalise.kernel.common.ObjectNotFoundException;
import org.cristalise.kernel.common.PersistencyException;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.lookup.AgentPath;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.TransactionKey;
import org.cristalise.kernel.process.Gateway;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * {@value #description}
 */
public class ImportImportItem extends PredefinedStep {
    public static final String description = "Creates or updates the Item created using ImportItem object";

    public ImportImportItem() {
        super();
        setBuiltInProperty(SCHEMA_NAME, "Item");
    }

    @Override
    protected String runActivityLogic(AgentPath agent, ItemPath item, int transitionID, String requestData, TransactionKey transactionKey)
            throws InvalidDataException, ObjectNotFoundException, ObjectCannotBeUpdated, CannotManageException, ObjectAlreadyExistsException,
            InvalidCollectionModification, PersistencyException
    {
        try {
            ImportItem importItem = (ImportItem) Gateway.getMarshaller().unmarshall(requestData);
            importItem.create(agent, true, transactionKey);

            return Gateway.getMarshaller().marshall(importItem);
        }
        catch (MarshalException | ValidationException | IOException | MappingException e) {
            log.error("Couldn't unmarshall Item: " + requestData, e);
            throw new InvalidDataException("Couldn't unmarshall Item: " + requestData);
        }
    }
}
