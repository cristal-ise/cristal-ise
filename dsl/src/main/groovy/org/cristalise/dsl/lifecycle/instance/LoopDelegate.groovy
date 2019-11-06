

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

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PAIRING_ID

import org.cristalise.kernel.lifecycle.instance.Next
import org.cristalise.kernel.lifecycle.instance.Split
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.utils.Logger

import groovy.transform.CompileStatic


/**
 * 
 */
@CompileStatic
class LoopDelegate extends BlockDelegate {
    Types type = Types.LoopSplit

    public LoopDelegate(Map props, CompActDelegate caBlock, Map<String, WfVertex> cache) {
        assert caBlock
        assert cache

        index = DelegateCounter.getNextCount(type)

        String n = ""
        if(props) {
            properties = props
            n = properties?.name
        }

        name = getAutoName(n, type, index)

        parentCABlock = caBlock
        vertexCache = cache
    }

    /**
     * 
     * @param cl
     */
    public void processClosure(Closure cl) {
        assert cl, "Split only works with a valid Closure"

        Logger.msg 1, "$name -----------------------------------------"

        String joinName = name.replace('Split', 'Join')

        def joinFirst  = parentCABlock.createVertex(Types.Join, "${joinName}_first")
        def split      = parentCABlock.createVertex(type, name)
        def joinLast   = parentCABlock.createVertex(Types.Join, "${joinName}_last")

        split.setBuiltInProperty(PAIRING_ID, name)
        joinFirst.setBuiltInProperty(PAIRING_ID, name)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if(firstVertex) {
            joinFirst.addNext(firstVertex)
            lastVertex.addNext(split)
        }
        else {
            //in case of an empty Loop
            joinFirst.addNext(split)
        }

        Next n = ((Split)split).addNext(joinFirst)
        n.getProperties().setBuiltInProperty(ALIAS, 'true')

        n = ((Split)split).addNext(joinLast)
        n.getProperties().setBuiltInProperty(ALIAS, 'false')

        setSplitProperties(split)
        setVertexProperties(split);

        firstVertex = joinFirst
        lastVertex = joinLast

        Logger.msg 1, "$name(end) +++++++++++++++++++++++++++++++++++++++"
    }
}
