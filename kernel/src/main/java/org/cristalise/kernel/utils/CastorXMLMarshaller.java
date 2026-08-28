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
package org.cristalise.kernel.utils;

import java.util.Objects;
import org.cristalise.kernel.common.InvalidDataException;

public class CastorXMLMarshaller implements KernelMarshaller {

    private final CastorXMLUtility marshaller;

    public CastorXMLMarshaller(CastorXMLUtility marshaller) {
        this.marshaller = Objects.requireNonNull(marshaller, "marshaller");
    }

    @Override
    public String marshall(Object obj) throws InvalidDataException {
        // Keep Castor null compatibility behavior unchanged.
        return marshaller.marshall(obj);
    }

    @Override
    public Object unmarshall(String data) throws InvalidDataException {
        return marshaller.unmarshall(data);
    }

    @Override
    public <T> T unmarshall(String data, Class<T> type) throws InvalidDataException {
        Objects.requireNonNull(type, "type");

        Object value = unmarshall(data);
        if (value == null) return null;

        if (!type.isInstance(value)) {
            throw new InvalidDataException(
                "Deserialized object type mismatch. expected=" + type.getName() + ", actual=" + value.getClass().getName()
            );
        }

        return type.cast(value);
    }
}
