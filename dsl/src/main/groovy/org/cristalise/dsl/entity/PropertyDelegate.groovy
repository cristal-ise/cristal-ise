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
package org.cristalise.dsl.entity

import groovy.transform.CompileStatic

import java.util.ArrayList;

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.imports.ImportItem;
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.property.PropertyArrayList
import org.cristalise.kernel.utils.Logger


/**
 * Wrapper/Delegate class of PropertyArrayList used for Item and Agent Properties
 * 
 */
@CompileStatic
class PropertyDelegate {

    PropertyArrayList itemProps = new PropertyArrayList();

    public void processClosure(Closure cl) {
        assert cl, "PropertyDelegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void InmutableProperty(Map<String, String> attrs) {
        assert attrs, "InmutableProperty must have the name and value pair set"

        attrs.each { k, v ->
            if(!v) throw new InvalidDataException("Inmutable EntityProperty '$k' must have valid value")

            Logger.msg 5, "InmutableProperty - name/Value: $k/$v"

            if(v instanceof String) updateProps(k, v, false)
            else                    throw new InvalidDataException("Property '$k' value must be String")
        }
    }

    public void Property(String name) {
        Property((name): "")
    }

    public void Property(Map<String, String> attrs) {
        assert attrs, "Mutable EntityProperty must have the name and value pair set"

        attrs.each { k, v ->
            Logger.msg 0, "Property - name/value: $k/$v"

            if(v instanceof String) updateProps(k, v, true)
            else                    throw new InvalidDataException("EntityProperty '$k' value must be String")
        }
    }

    /**
     * Ensures that GString value is resolved
     * 
     * @param key
     * @param value
     * @param mutable
     */
    private void updateProps(String key, String value, Boolean mutable) {
        itemProps.put(
            new Property(
                (key   instanceof String) ? (String)key   : key,
                (value instanceof String) ? (String)value : value,
                mutable)
            )
    }
}
