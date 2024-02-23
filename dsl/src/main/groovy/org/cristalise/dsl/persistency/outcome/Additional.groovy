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
package org.cristalise.dsl.persistency.outcome;

/**
 * Store arbitrary number of key/value pair which are used to create element in dynamicForms/additional
 */
public class Additional {
    def fields = [:]

    /**
     * Interceptor method of dynamic groovy to handle missing property exception for setter operations
     * 
     * @param name the name of the property
     * @param value the value to be set for the property
     * @return the previous value associated with property
     */
    public Object propertyMissing(String name, Object value) { 
        return fields[name] = value
    }

    /**
     * Interceptor method of dynamic groovy to handle missing property exception for getter operations
     * 
     * @param name the name of the property
     * @return the value associated with property
     */
    public Object propertyMissing(String name) {
        return fields[name]
    }
}
