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

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.persistency.outcome.Schema;
import org.cristalise.kernel.process.resource.BuiltInResources;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchemaCache extends DescriptionObjectCache<Schema> {

    @Override
    protected String getTypeCode() {
        return BuiltInResources.SCHEMA_RESOURCE.getTypeCode();
    }

    @Override
    protected String getSchemaName() {
        return BuiltInResources.SCHEMA_RESOURCE.getSchemaName();
    }

    @Override
    protected String getTypeRoot() {
        return BuiltInResources.SCHEMA_RESOURCE.getTypeRoot();
    }

    @Override
    protected Schema buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException {
        try {
            Schema thisSchema = new Schema(name, version, path, data);
            thisSchema.validate();
            return thisSchema;
        }
        catch (Exception ex) {
            log.error("Could not parse Schema '" + name + "' v" + version, ex);
            throw new InvalidDataException("Could not parse Schema '" + name + "' v" + version + ": " + ex.getMessage());
        }
    }
}