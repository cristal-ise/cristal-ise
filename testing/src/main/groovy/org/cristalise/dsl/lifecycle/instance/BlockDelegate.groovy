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
 * Block is a group of WfVertices, it should only be used within Splits
 */
@CompileStatic
public class BlockDelegate {
    String name = ""

    CompActDelegate parentCABlock = null

    WfVertex firstVertex = null
    WfVertex lastVertex  = null

    Map<String, WfVertex> vertexCache = null
    
    Map<String, Object>  properties = [:]

    int index = -1

    public BlockDelegate() {}

    public BlockDelegate(CompActDelegate caBlock, Map<String, WfVertex> cache) {
        assert caBlock
        parentCABlock = caBlock
        vertexCache = cache
    }

    
    protected void setVertexProperties(WfVertex vertex) {
        properties.each { key, value -> vertex.properties.put(key, value, false) }
    }

    public static String getNamePrefix(Types t) {
        switch(t) {
            case Types.Composite: return 'CA'
            case Types.Atomic:    return 'EA'
            default:              return t.toString()
        }
    }

    public static String getAutoName(String n, Types t, int i) {
        Logger.msg(5, "getAutoName() - name:'$n', type: $t, index: $i")

        String namePrefix = getNamePrefix(t)

        if(n) {
            if(t == Types.AndSplit || t == Types.OrSplit || t == Types.XOrSplit || t == Types.LoopSplit)
                assert n.startsWith(namePrefix), "Name shall start with '$namePrefix'"
            return n
        }
        else {
            return "${namePrefix}" + ((i == 0) ? "" : "$i")
        }
    }

    public void updateVertexCache(Types t, String n, WfVertex v) {
        if(n) {
            if(vertexCache.containsKey(n)) throw new RuntimeException("Vertex name '$n' must be unique")
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

    /**
     * Links the current Block with its child Block
     * 
     * @param childBlock the child Block to be linked with
     */
    protected void linkWithChild(BlockDelegate childBlock) {
        Logger.msg 1, "Block.linkWithChild() - ${childBlock.getClass()}"

        if(!firstVertex) firstVertex = childBlock.firstVertex
        else             lastVertex.addNext(childBlock.firstVertex)

        lastVertex = childBlock.lastVertex
    }

    /**
     * DSL method to add properties
     * 
     * @param props Map containing properties
     */
    public void Property(Map<String, Object> props) {
        properties << props
    }

    /**
     * Alias of the DSL method Block()
     * 
     * @param cl the closure to be executed to build the Block
     */
    public void B(Closure cl) {
        Block(cl)
    }

    /**
     * DSL method to add a Block to the Workflow
     * 
     * @param cl the closure to be executed to build the Block
     */
    public void Block(Closure cl) {
        def b = new BlockDelegate(parentCABlock, vertexCache)
        b.processClosure(cl)
        linkWithChild(b)
    }

    /**
     * DSL method to add a Composite Activity to the Workflow
     *
     * @param name the name of the Composite Activity, can be omitted
     * @param cl the closure to be executed to build the Composite Activity
     */
    public void CompAct(String name = "", Closure cl) {
        def b = new CompActDelegate(name, vertexCache)
        b.processClosure(this,cl)
    }

    /**
     * 
     * @param props
     * @param cl
     */
    public void Split(Map props, Types t, Closure cl) {
        def b = new SplitDelegate(props, t, parentCABlock, vertexCache)
        b.processClosure(cl)
        linkWithChild(b)
    }

    /**
     * DSL method to add a typed Split to the Workflow
     * 
     * @param name the name of the Split, can be omitted
     * @param type the type (And,Or,XOr, Loop) of the Split
     * @param cl the closure to be executed to build the Split
     */
    public void Split(String n = "", Types t, Closure cl) {
        Split(name: n, t, cl)
    }

    /**
     * DSL method to add an Elementary Activity to the Workflow
     *
     * @param name
     * @param cl the closure to be executed to build the Elementary Activity. Can be omitted.
     */
    public void ElemAct(String n = "", Closure cl = null) {
        new ElemActDelegate(n).processClosure(this, cl)
    }


    /**
     * Alias of the DSL method ElemAct()
     * 
     * @param name
     * @param cl the closure to be executed to build the Elementary Activity. Can be omitted.
     */
    public void EA(String n = "", Closure cl = null) {
        ElemAct(n, cl)
    }

    /**
     * Alias of DSL method CompAct()
     * 
     * @param name
     * @param cl
     */
    public void CA(String n = "", Closure cl) {
        CompAct(n, cl)
    }

    /**
     * 
     * 
     * @param name
     * @param cl
     */
    public void AndSplit(String n = "", Closure cl) {
        Split(n, Types.AndSplit, cl)
    }

    /**
     * 
     * @param props
     * @param cl
     */
    public void OrSplit(Map props, Closure cl) {
        Split(props, Types.OrSplit, cl)
    }

    /**
     * 
     * @param name
     * @param cl
     */
    public void OrSplit(String n = "", Closure cl) {
        Split(name: n, Types.OrSplit, cl)
    }

    /**
     * 
     * @param name
     * @param cl
     */
    public void XOrSplit(Map props, Closure cl) {
        Split(props, Types.XOrSplit, cl)
    }

    /**
     * 
     * @param name
     * @param cl
     */
    public void XOrSplit(String n = "", Closure cl) {
        Split(n, Types.XOrSplit, cl)
    }

    /**
     * 
     * @param name
     * @param cl
     */
    public void Loop(String n = "", Closure cl) {
        Split(n, Types.LoopSplit, cl)
    }
}
