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
/**
 *
 */
package org.cristalise.kernel.utils;

import static org.cristalise.kernel.process.resource.BuiltInResources.ITEM_DESC_RESOURCE;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ItemDescCache extends DescriptionObjectCache<ImportItem> {

    @Override
    public String getTypeCode() {
        return ITEM_DESC_RESOURCE.getTypeCode();
    }

    @Override
    public String getSchemaName() {
        return ITEM_DESC_RESOURCE.getSchemaName();
    }

    @Override
    public String getTypeRoot() {
        return ITEM_DESC_RESOURCE.getTypeRoot(); 
    }

    @Override
    public ImportItem buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException {
        try {
            ImportItem thisImportItem = (ImportItem) Gateway.getMarshaller().unmarshall(data);
            thisImportItem.setName(name);
            thisImportItem.setVersion(version);
            thisImportItem.setItemPath(path);
            return thisImportItem;
        }
        catch (Exception ex) {
            log.error("Could not unmarshall ImportItem '" + name + "' v" + version, ex);
            throw new InvalidDataException("Could not unmarshall ImportItem '" + name + "' v" + version + ": " + ex.getMessage());
        }
    }

}