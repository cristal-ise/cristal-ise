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

import org.apache.commons.lang3.StringUtils
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.property.Property
import org.cristalise.kernel.property.PropertyArrayList

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 * Wrapper/Delegate class of PropertyArrayList used for Item and Agent Properties
 * 
 */
@CompileStatic @Slf4j
class PropertyDelegate {

    PropertyArrayList itemProps = new PropertyArrayList();

    public void processClosure(Closure cl) {
        assert cl, "PropertyDelegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    private void setProperty(String key, Object value, boolean mutable) {
        String stringValue = null

        if (value != null) {
            if      (value instanceof DomainContext) stringValue = ((DomainContext)value).getDomainPath()
            else if (value instanceof String)        stringValue = (String)value
            else                                     stringValue = value.toString()
        }

        def propType = mutable ? 'Property' : 'InmutableProperty'
        log.debug('{} - {}:{}', propType, key, stringValue)

        itemProps.put(new Property(key, stringValue, mutable))
    }

    public void InmutableProperty(Map<String, Object> attrs) {
        assert attrs, "InmutableProperty must have the name and value pair set"

        attrs.each { k, v ->
            // ItempProperties have type String, so blank values are also rejected here
            if (v == null || (v instanceof String && StringUtils.isBlank(v))) {
                throw new InvalidDataException("Inmutable EntityProperty '$k' must have valid value")
            }

            setProperty(k, v, false)
        }
    }

    public void Property(String name) {
        Property((name): "")
    }

    public void Property(Map<String, Object> attrs) {
        assert attrs, "Mutable EntityProperty must have the name and value pair set"

        attrs.each { k, v ->
            setProperty(k, v, true)
        }
    }
}
