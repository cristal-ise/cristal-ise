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
package org.cristalise.dsl.property

import groovy.transform.CompileStatic

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.property.PropertyDescriptionList


/**
 * Wrapper/Delegate class of CastorArrayList used in Items
 *
 */
@CompileStatic
class PropertyDescriptionDelegate {

    public PropertyDescriptionList propDescList

    public PropertyDescriptionDelegate() {
        propDescList = new PropertyDescriptionList()
    }

    public PropertyDescriptionDelegate(String ns, String n, Integer v) {
        this()
        propDescList.namespace = ns
        propDescList.name = n
        propDescList.version = v
    }

    public void processClosure(Closure cl) {
        assert cl, "PropertyDescriptionDelegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void PropertyDesc(String name) {
        PropertyDesc(name: (Object)name)
    }

    public void PropertyDesc(Map<String, Object> attrs) {
        assert attrs && attrs.name, "PropertyDesc must have the name set"

        if(attrs.defaultValue != null && !(attrs.defaultValue instanceof String)) {
            throw new InvalidDataException("defaultValue must be String type, Property can only hold text value")
        }

        if(attrs.isClassIdentifier == null) attrs.isClassIdentifier = false
        if(attrs.isMutable         == null) attrs.isMutable         = true
        if(attrs.isTransitive      == null) attrs.isTransitive      = false

        propDescList.add(
            (String)attrs.name, 
            (String)attrs.defaultValue, 
            (boolean)attrs.isClassIdentifier, 
            (boolean)attrs.isMutable,
            (boolean)attrs.isTransitive
        )
    }
}
