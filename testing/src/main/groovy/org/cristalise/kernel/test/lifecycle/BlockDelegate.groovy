
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

package org.cristalise.kernel.test.lifecycle

import groovy.transform.CompileStatic

import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
public class BlockDelegate {

    CompActDelegate parentCABlock = null
    
    //CompositeActivity currentCA = null

    WfVertex firstVertex = null
    WfVertex lastVertex  = null

    Map<String,Activity> actCache

    public BlockDelegate() {}

    public BlockDelegate(CompActDelegate caBlock, Map<String,Activity> cache) {
        assert caBlock
        parentCABlock = caBlock
        actCache = cache
    }

    public WfVertex addVertex(Types t, String name) {
        WfVertex v = parentCABlock.createVertex(t, name)

        Logger.msg 1, "Block.addVertex() - type: '$t'; id: '$v.ID'; name: '$name;' path: '$v.path'"

        if(!firstVertex) firstVertex = v
        if(lastVertex) lastVertex.addNext(v)
        lastVertex = v

        if(name && v instanceof Activity) actCache[name] = (Activity)v
        
        return v
    }

    /**
     * 
     * @param parentBlock
     * @param cl
     */
    public void processClosure(BlockDelegate parentBlock, Closure cl) {
        assert cl, "Block only works with a valid Closure"

        Logger.msg 1, "Block(start) ---------------------------------------"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if(parentBlock) {
            if(!parentBlock.firstVertex) parentBlock.firstVertex = lastVertex
            if(parentBlock.lastVertex) parentBlock.lastVertex.addNext(firstVertex)
            parentBlock.lastVertex = lastVertex
        }

        Logger.msg 1, "Block(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    /**
     * 
     * @param cl
     */
    public void Block(Types type = null, Closure cl) {
        new BlockDelegate(parentCABlock, actCache).processClosure(this, cl)
    }

    /**
     * 
     * @param name
     * @return
     */
    public void ElemAct(String name = "", Closure cl = null) {
        new ElemActDelegate(name).processClosure(this, cl)
    }

    /**
     * 
     * @param name
     * @param cl
     * @return
     */
    public void CompAct(String name = "", Closure cl) {
        CompositeActivity ca = (CompositeActivity)addVertex(Types.Composite, name)
        new CompActDelegate(name, ca, actCache).processClosure(cl)
    }

    public void Split(String name = "", Types type, Closure cl) {
    }
}
