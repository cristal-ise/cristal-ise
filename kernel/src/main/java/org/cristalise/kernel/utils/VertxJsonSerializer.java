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

import lombok.extern.slf4j.Slf4j;
import org.cristalise.kernel.common.InvalidDataException;
import io.vertx.core.json.Json;

@Slf4j
public class VertxJsonSerializer implements KernelSerializer {

    @Override
    public String marshall(Object obj) throws InvalidDataException {
        if (obj == null) return "null";

        try {
            return Json.encode(obj);
        }
        catch (Exception ex) {
            log.error("marshall() - failed", ex);
            throw new InvalidDataException("marshall failed", ex);
        }
    }

    @Override
    public Object unmarshall(String data) throws InvalidDataException {
        if (data == null) throw new InvalidDataException("unmarshall failed: data is null");
        if ("null".equals(data.trim())) return null;

        try {
            return Json.decodeValue(data);
        }
        catch (Exception ex) {
            log.error("marshall() - failed", ex);
            throw new InvalidDataException("unmarshall failed", ex);
        }
    }

    @Override
    public <T> T unmarshall(String data, Class<T> type) throws InvalidDataException {
        Objects.requireNonNull(type, "type");
        if (data == null) throw new InvalidDataException("unmarshall failed: data is null");
        if ("null".equals(data.trim())) return null;

        try {
            return Json.decodeValue(data, type);
        }
        catch (Exception ex) {
            log.error("marshall() - failed", ex);
            throw new InvalidDataException("unmarshall failed", ex);
        }
    }
}
