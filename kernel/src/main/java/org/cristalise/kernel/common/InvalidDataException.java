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
package org.cristalise.kernel.common;

import static org.cristalise.kernel.common.CriseVertxException.FailureCodes.InvalidData;

public class InvalidDataException extends CriseVertxException {
    private static final long serialVersionUID = -4491884465493921352L;

    public InvalidDataException() {
        super(InvalidData);
    }

    public InvalidDataException(Throwable e) {
        super(InvalidData, e);
    }

    public InvalidDataException(String msg) {
        super(InvalidData, msg);
    }

    public InvalidDataException(String message, Throwable cause) {
        super(InvalidData, message, cause);
    }
}
