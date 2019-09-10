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

import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.LifecycleVertexOutlineCreator
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.Next
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
        LifecycleVertexOutlineCreator lifecycleVertexOutlineCreator = new LifecycleVertexOutlineCreator();
        lifecycleVertexOutlineCreator.setOutline(v)
        Logger.msg 1, "CA.createVertex(path: $currentCA.path) - type: '$t'; id: '$v.ID'; name: '$name;' path: '$v.path'"

        firstFlag = false
        updateVertexCache(t, name, v)
        return v
    }

    public WfVertex addVertex(Types t, String name) {
        WfVertex v = createVertex(t, name)

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

    /*************************************************************
    * Unbalanced DSL methods and members
    * TODO: Probably it needs to be moved the a new class
    *************************************************************/

    def dslCache

    private List decodeBuilderMap(Map<String, String> vMap) {
        assert vMap, "decodeBuilderMap can only handle valid map"
        assert vMap.size() == 1, "decodeBuilderMap can only handle single key/value in map"

        String vName
        Types vType

        if(vMap.ElemAct) {
            vName = vMap.ElemAct
            vType = Types.Atomic
        }
        else if(vMap.AndSplit) {
            vName = vMap.AndSplit
            vType = Types.AndSplit
        }
        else if(vMap.OrSplit) {
            vName = vMap.OrSplit
            vType = Types.OrSplit
        }
        else if(vMap.XOrSplit) {
            vName = vMap.XOrSplit
            vType = Types.XOrSplit
        }
        else if(vMap.LoopSplit) {
            vName = vMap.LoopSplit
            vType = Types.LoopSplit
        }
        else if(vMap.Join) {
            vName = vMap.Join
            vType = Types.Join
        }
        else throw new InvalidDataException("Unhandled builder map values: $vMap")

        return [vName, vType]
    }


    private CompActDelegate connectTo (Map<String, String> vMap, boolean connectFlag) {
        //Unfortunately multiple assignment syntax does not work with CompiletStatic
        //def (String vName, Types vType) = decodeBuilderMap(vMap)
        def tempList = decodeBuilderMap(vMap)
        String vName = tempList[0]
        Types vType = (Types) tempList[1]

        if(vertexCache.containsKey(vName)) {
            if(connectFlag) return connect(getVertex(vName, vType))
            else            return to(vertexCache[vName])
        }
        else {
            if(connectFlag) return connect(createVertex(vType, vName))
            else            return to(createVertex(vType, vName))
        }
    }

    /**
     * This method creates a single VfVertex
     * 
     * @param vMap a Map ala groovy style ancoding the type and the name of the new
     * @return 
     */
    public WfVertex create(Map<String, String> vMap) {
        def tempList = decodeBuilderMap(vMap)
        String vName = tempList[0]
        Types vType = (Types) tempList[1]

        if(vertexCache.containsKey(vName)) return getVertex(vName, vType)
        else                               return createVertex(vType, vName)
    }

    public CompActDelegate connect(Map<String, String> vMap) {
        return connectTo(vMap, true)
    }

    public CompActDelegate connect(WfVertex v) {
        dslCache = v
        return this
    }

    public CompActDelegate to(Map<String, String> vMap) {
        return connectTo(vMap, false)
    }

    public CompActDelegate to(WfVertex v) {
        assert dslCache, "Call connect() before calling to()"
        assert dslCache instanceof WfVertex, "to() links WfVertex to WfVertex, cannot handle '${dslCache.class.name}'"
        
        dslCache = dslCache.addNext(v)

        return this
    }

    public CompActDelegate alias(Object val) {
        assert dslCache, "Call to() before calling alias()"
        assert dslCache instanceof Next, "alias() set Alias property of Next, cannot handle '${dslCache.class.name}'"

        ((Next)dslCache).properties.Alias = val
        dslCache = null

        return this
    }

    public void setFirst(String name) {
        if(vertexCache.containsKey(name)) { setFirst(vertexCache[name]) }
        else throw new InvalidDataException("Unknown name:$name")
    }

    public void setFirst(WfVertex v) {
        ((CompositeActivity)v.getParent()).setFirstVertex(v.ID)
    }
}
