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

import static org.cristalise.kernel.process.resource.BuiltInResources.PROPERTY_DESC_RESOURCE;

import org.cristalise.kernel.common.InvalidDataException;
import org.cristalise.kernel.lookup.ItemPath;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.property.PropertyDescriptionList;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyDescriptionCache extends DescriptionObjectCache<PropertyDescriptionList> {

    @Override
    protected String getTypeCode() {
        return PROPERTY_DESC_RESOURCE.getTypeCode();
    }

    @Override
    protected String getSchemaName() {
        return PROPERTY_DESC_RESOURCE.getSchemaName();
    }

    @Override
    protected String getTypeRoot() {
        return PROPERTY_DESC_RESOURCE.getTypeRoot();
    }

    @Override
    protected PropertyDescriptionList buildObject(String name, int version, ItemPath path, String data) throws InvalidDataException {
        try {
            PropertyDescriptionList pdl = (PropertyDescriptionList) Gateway.getMarshaller().unmarshall(data);
            pdl.setName(name);
            pdl.setVersion(version);
            pdl.setItemPath(path);
            return pdl;
        }
        catch (Exception ex) {
            log.error("Could not parse PropertyDescriptionList '" + name + "' v" + version, ex);
            throw new InvalidDataException("Could not parse PropertyDescriptionList '" + name + "' v" + version + ": " + ex.getMessage());
        }
    }
}