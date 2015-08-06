
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

import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.utils.Logger


/**
 * Block is a group of WfVertices, it should used only within Splits
 */
@CompileStatic
public class BlockDelegate {
    String name = ""

    CompActDelegate parentCABlock = null

    WfVertex firstVertex = null
    WfVertex lastVertex  = null

    Map<String, WfVertex> vertexCache = null

    public BlockDelegate() {}

    public BlockDelegate(CompActDelegate caBlock, Map<String, WfVertex> cache) {
        assert caBlock
        parentCABlock = caBlock
        vertexCache = cache
    }

    public void updateVertexCache(Types t, String n, WfVertex v) {
        if(n) {
            if(vertexCache.containsKey(n)) throw new RuntimeException("$n must be unique")
            else vertexCache[n] = v
        }
    }

    public WfVertex addVertex(Types t, String name) {
        WfVertex v = parentCABlock.createVertex(t, name)

        Logger.msg 1, "Block.addVertex() - type: '$t'; id: '$v.ID'; name: '$name;' path: '$v.path'"

        if(!firstVertex) firstVertex = v
        if(lastVertex) lastVertex.addNext(v)
        lastVertex = v

        return v
    }

    /**
     * 
     * @param parentBlock
     * @param cl
     */
    public void processClosure(Closure cl) {
        assert cl, "Block only works with a valid Closure"

        Logger.msg 1, "Block(start) ---------------------------------------"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        Logger.msg 1, "Block(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    def linkFirstWithLast(BlockDelegate b) {
        Logger.msg 1, "Block.linkFirstWithLast() - ${b.getClass()}"
        if(!firstVertex) firstVertex = b.lastVertex
        if(lastVertex) lastVertex.addNext(b.firstVertex)
        lastVertex = b.lastVertex
    }

    /**
     * 
     * @param cl
     */
    public void Block(Closure cl) {
        def b = new BlockDelegate(parentCABlock, vertexCache)
        b.processClosure(cl)
        linkFirstWithLast(b)
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
        new CompActDelegate(name, ca, vertexCache).processClosure(cl)
    }

    public void Split(String name = "", Types type, Closure cl) {
        new SplitDelegate(name, type, parentCABlock, vertexCache).processClosure(this, cl)
    }

    public void AndSplit(String name = "", Closure cl) {
        if(name) assert name.startsWith('AndSplit'), "Name shall start with 'AndSplit'"
        Split(name, Types.AndSplit, cl)
    }

    public void OrSplit(String name = "", Closure cl) {
        if(name) assert name.startsWith('OrSplit'), "Name shall start with 'OrSplit'"
        Split(name, Types.OrSplit, cl)
    }

    public void XOrSplit(String name = "", Closure cl) {
        if(name) assert name.startsWith('XOrSplit'), "Name shall start with 'XOrSplit'"
        Split(name, Types.XOrSplit, cl)
    }
}
