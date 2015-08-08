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
class SplitDelegate extends BlockDelegate {
    Types type = null
    List<BlockDelegate> childBlocks = []
    
    String joinName = ""

    SplitDelegate() {}

    public SplitDelegate(String n, Types t, CompActDelegate caBlock, Map<String, WfVertex> cache) {
        assert t
        assert (t == Types.AndSplit || t == Types.OrSplit || t == Types.XOrSplit || t == Types.LoopSplit), "Type shall be either And/Or/XOR/Loop Split"
        assert caBlock
        assert cache

        //FIXME: Rework automatic naming of Split/Join
        if(n) {
            assert n.startsWith(t.toString()), "Name shall start with '$type'"
            name = n
        }
        else {
            name = t.toString()
        }
        joinName = name.replace(t.toString(), "Join")
        
        type = t
        parentCABlock = caBlock
        vertexCache = cache
    }

    public static void setRoutingScript(WfVertex aSplit) {
        aSplit.getProperties().put("RoutingScriptName", "javascript:true;");
        aSplit.getProperties().put("RoutingScriptVersion", "")
    }

    public void processClosure(BlockDelegate parentBlock, Closure cl) {
        assert cl, "Block only works with a valid Closure"

        Logger.msg 1, "$name(type: $type) ----------------------------------"

        def aSplit = parentCABlock.createVertex(type, name)
        def aJoin  = parentCABlock.createVertex(Types.Join, joinName)
        
        setRoutingScript(aSplit);

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        childBlocks.each {
            aSplit.addNext(it.firstVertex)
            it.lastVertex.addNext(aJoin)
        }

        firstVertex = aSplit
        lastVertex = aJoin

        //Loop is linked to its Join
        if(type == Types.LoopSplit) {
            ((org.cristalise.kernel.lifecycle.instance.Split)aSplit).addNext(aJoin)
            Logger.msg 1, "..............................................."
        }

        Logger.msg 1, "$name(end) +++++++++++++++++++++++++++++++++++++++++"
    }

    public void ElemAct(String name = "", Closure cl = null) {
        throw new RuntimeException("Split cannot have standalone ElemAct, it shall be within a Block")
        /*
        if(type == Types.LoopSplit) super.ElemAct(name,cl)
        else throw new RuntimeException("Split cannot have standalone Activity, it shall be within a Block/CA/Split/Loop")
        */
    }

    public void Block(Closure cl) {
        def b = new BlockDelegate(parentCABlock, vertexCache)
        childBlocks.add(b)
        b.processClosure(cl)
    }

    public void CompAct(String name = "", Closure cl) {
        throw new RuntimeException("Split cannot have standalone CompAct, it shall be within a Block")
        /*
        CompositeActivity ca = (CompositeActivity)addVertex(Types.Composite, name)
        def caB = new CompActDelegate(name, ca, vertexCache)
        childBlocks.add(caB)
        caB.processClosure(cl)
        */
    }

    public void Split(String name = "", Types type, Closure cl) {
        throw new RuntimeException("Split cannot have standalone Split, it shall be within a Block")
        /*
        def sB = new SplitDelegate(name, type, parentCABlock, vertexCache)
        childBlocks.add(sB)
        sB.processClosure(this, cl)
        */
    }
}
