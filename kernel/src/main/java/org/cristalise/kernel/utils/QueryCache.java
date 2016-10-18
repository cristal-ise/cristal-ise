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
import org.cristalise.kernel.process.resource.BuiltInResources;
import org.cristalise.kernel.querying.Query;

public class QueryCache extends DescriptionObjectCache<Query> {

    @Override
    public String getTypeCode() {
        return BuiltInResources.QUERY.getName();
    }

    @Override
    public String getSchemaName() {
        return "Query";
    }

    @Override
    public Query buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException {
        try {
            return new Query(name, version, path, data);
        }
        catch (Exception ex) {
            Logger.error(ex);
            throw new InvalidDataException("Error parsing query '" + name + "' v" + version + ": " + ex.getMessage());
        }
    }
}