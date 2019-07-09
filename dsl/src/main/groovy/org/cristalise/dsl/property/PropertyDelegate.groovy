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

import org.cristalise.kernel.utils.CastorHashMap
import org.cristalise.kernel.utils.Logger


/**
 * Wrapper/Delegate class of CastorHashMap used in Lifecycle and Collection Properties
 *
 */
@CompileStatic
class PropertyDelegate {

    CastorHashMap props = new CastorHashMap()

    public void processClosure(Closure cl) {
        assert cl, "Delegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void Property(String name) {
        Property((name): "")
    }

    public void Property(Map<String, String> attrs) {
        assert attrs, "Property must have the name and value pair set"

        attrs.each { k, v ->
            Logger.msg 0, "PropertyDelegate.Property() - adding name/Value: $k/$v"

            updateProps(k, v, false)
        }
    }

    public void AbstractProperty(Map<String, String> attrs) {
        assert attrs, "AbstractProperty must have the name and value pair set"

        attrs.each { k, v ->
            Logger.msg 8, "PropertyDelegate.AbstractProperty() - adding name/Value: $k/$v"

            updateProps(k, v, true)
        }
    }

    /**
     * Ensures that GString value is resolved
     * 
     * @param key
     * @param value
     */
    private void updateProps(String key, String value, Boolean isAbstract) {
        props.put(
            (key   instanceof String) ? (String)key   : key,
            (value instanceof String) ? (String)value : value,
            isAbstract)
    }
}
