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
package org.cristalise.dsl.lifecycle.instance

import groovy.transform.CompileStatic

import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class ElemActDelegate {
    public static final Types type = Types.Atomic
    
    WfVertex currentVertex = null

    String name = ""
    int index = -1

    public ElemActDelegate(String eaName) { 
        index = DelegateCounter.getNextCount(type)
        name = BlockDelegate.getAutoName(eaName, type, index)
    }

    public void Property(Map<String, Object> props) {
        Logger.msg 5, "ElemActDelegate.Property() - props: $props"
        props.each { key, value -> 
            currentVertex.properties.put(key, (value instanceof String) ? (String)value : value, false)
        }
    }

    public void processClosure(BlockDelegate parentBlock, Closure cl = null) {
        assert parentBlock, "Activity must belong to Block/CA"

        currentVertex = parentBlock.addVertex(type, name)

        if(cl) {
            cl.delegate = this
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }
    }
}