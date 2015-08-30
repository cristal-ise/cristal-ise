

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

import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
public class CompActDelegate extends BlockDelegate {
    public static final Types type = Types.Composite

    CompositeActivity currentCA = null
    boolean firstFlag = true

    public CompActDelegate(String caName, Map<String, WfVertex> cache) {
        assert cache
        vertexCache = cache
        
        //the rootCA should not be auto named neither counted 
        if(caName == 'rootCA') {
            name = caName
            currentCA = (CompositeActivity)vertexCache[caName]
        }
        else {
            index = DelegateCounter.getNextCount(type)
            name = getAutoName(caName, type, index)
        }
        
        parentCABlock = null
    }

    public WfVertex createVertex(Types t, String name) {
        assert currentCA
        WfVertex v = currentCA.newChild(t, name, firstFlag, (GraphPoint)null)

        firstFlag = false
        updateVertexCache(t, name, v)
        return v
    }

    public WfVertex addVertex(Types t, String name) {
        WfVertex v = createVertex(t, name)

        Logger.msg 1, "CA.addVertex(path: $currentCA.path) - type: '$t'; id: '$v.ID'; name: '$name;' path: '$v.path'"

        if(!firstVertex) firstVertex = v
        if(lastVertex) lastVertex.addNext(v)
        lastVertex = v

        return v
    }

    public void processClosure(BlockDelegate parentBlock, Closure cl) {
        assert cl, "CompAct only works with a valid Closure"
        assert parentBlock, "CA must belong to Block/CA"
        
        Logger.msg 1, "CompAct(start) ---------------------------------------"

        currentCA =  (CompositeActivity)parentBlock.addVertex(Types.Composite, name)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        setVertexProperties(currentCA)

        Logger.msg 1, "CompAct(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    public void Block(Closure cl) {
        def b = new BlockDelegate(this, vertexCache)
        b.processClosure(cl)
        linkWithChild(b)
    }

    public void Split(Map props, Types t, Closure cl) {
        def b = new SplitDelegate(props, t, this, vertexCache)
        b.processClosure(cl)
        linkWithChild(b)
    }

    public void Loop(Map props, Closure cl) {
        def b = new LoopDelegate(props, this, vertexCache)
        b.processClosure(cl)
        linkWithChild(b)
    }
}
