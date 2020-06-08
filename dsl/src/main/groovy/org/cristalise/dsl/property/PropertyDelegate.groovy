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

import org.cristalise.kernel.utils.CastorHashMap

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 * Wrapper/Delegate class of CastorHashMap used in Lifecycle and Collection Properties
 *
 */
@CompileStatic @Slf4j
class PropertyDelegate {

    CastorHashMap props = new CastorHashMap()

    public void processClosure(Closure cl) {
        assert cl, "Delegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void Property(String name) {
        Property((name): (Object)"")
    }

    public void Property(Map<String, Object> attrs) {
        assert attrs, "Property must have the name and value pair set"

        attrs.each { k, v ->
            log.debug 'Property() - adding name/value: {}/{}', k, v

            props.put(k, (v instanceof String) ? (String)v : v, false)
        }
    }

    public void AbstractProperty(Map<String, Object> attrs) {
        assert attrs, "AbstractProperty must have the name and value pair set"

        attrs.each { k, v ->
            log.debug 'AbstractProperty() - adding name/value: {}/{}', k, v

            props.put(k, (v instanceof String) ? (String)v : v, true)
        }
    }
}
