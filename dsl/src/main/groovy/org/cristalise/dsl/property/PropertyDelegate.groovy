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

import org.cristalise.kernel.entity.DomainContext
import org.cristalise.kernel.graph.model.BuiltInVertexProperties
import org.cristalise.kernel.utils.CastorHashMap

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


/**
 * Wrapper/Delegate class of CastorHashMap used in Lifecycle and Collection Properties
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

    private void addProperty(String name, Object value, boolean isAbstract) {
        log.debug '{}Property() - {}:{}', (isAbstract ? 'Abstract' : ''), name, value

        if (value != null) {
            if      (value.getClass().isEnum())      value = value.toString()
            else if (value instanceof DomainContext) value = ((DomainContext)value).getDomainPath()
        }

        props.put(name, value, isAbstract)
    }

    private void addProperties(Map<String, Object> attrs, boolean isAbstract) {
        assert attrs, "Property must have at least one name and value pair set"
        attrs.each { k, v -> addProperty(k, v, isAbstract) }
    }

    public void Property(String name) {
        Property(name, (Object)"")
    }

    public void Property(BuiltInVertexProperties prop, Object value) {
        Property(prop.getName(), value)
    }

    public void Property(String name, Object value) {
        addProperty(name, value, false)
    }

    public void Property(Map<String, Object> attrs) {
        addProperties(attrs, false)
    }

    public void AbstractProperty(String name) {
        AbstractProperty(name, (Object)"")
    }

    public void AbstractProperty(BuiltInVertexProperties prop, Object value) {
        AbstractProperty(prop.name(), value)
    }

    public void AbstractProperty(String name, Object value) {
        addProperty(name, value, true)
    }

    public void AbstractProperty(Map<String, Object> attrs) {
        addProperties(attrs, true)
    }
}
