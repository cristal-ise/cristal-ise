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

import org.cristalise.kernel.lifecycle.instance.Next
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
    
    public SplitDelegate(Map props, Types t, CompActDelegate caBlock, Map<String, WfVertex> cache) {
        assert caBlock
        assert cache
        assert t
        assert (t == Types.AndSplit || t == Types.OrSplit || t == Types.XOrSplit || t == Types.LoopSplit), "Type shall be either And/Or/XOR/Loop Split"

        type = t
        index = DelegateCounter.getNextCount(type)

        String n = ""
        if(props) {
            properties = props
            n = properties?.name
        }

        name = getAutoName(n, type, index)
        joinName = name.replace('Split', 'Join')

        parentCABlock = caBlock
        vertexCache = cache
    }

    private void setSplitProperties(WfVertex aSplit) {
        if(properties.javascript) {
            setRoutingScript(aSplit, (String)"javascript:\"${properties.javascript}\";", '');
            properties.remove('javascript')
        }
        else {
            setRoutingScript(aSplit, "javascript:\"true\";", '');
        }
    }

    private static void setRoutingScript(WfVertex aSplit, String name, String version) {
        Logger.msg 5, "SplitDelegate.setRoutingScript() - splitName: $aSplit.name, name: '$name' version: '$version'"

        aSplit.getProperties().put('RoutingScriptName',    name,    false);
        aSplit.getProperties().put('RoutingScriptVersion', version, false)
    }

    public void processClosure(Closure cl) {
        assert cl, "Split only works with a valid Closure"

        Logger.msg 1, "$name(type: $type) -----------------------------"

        def aSplit = parentCABlock.createVertex(type, name)
        def aJoin  = parentCABlock.createVertex(Types.Join, joinName)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        if(type == Types.LoopSplit && childBlocks.size() != 1) {
            throw new UnsupportedOperationException("Loop can have only one Block")
        }

        childBlocks.each {
            Next n = aSplit.addNext(it.firstVertex)
            if(type == Types.LoopSplit) n.getProperties().put("Alias", 'true')
            it.lastVertex.addNext(aJoin)
        }

        setSplitProperties(aSplit)
        setVertexProperties(aSplit);

        firstVertex = aSplit
        lastVertex = aJoin

        //Loop is linked to its Join
        if(type == Types.LoopSplit) {
            ((org.cristalise.kernel.lifecycle.instance.Split)aSplit).addNext(aJoin)
        }

        Logger.msg 1, "$name(end) +++++++++++++++++++++++++++++++++++++++"
    }

    public void Block(Closure cl) {
        def b = new BlockDelegate(parentCABlock, vertexCache)
        childBlocks.add(b)
        b.processClosure(cl)
    }

    public void ElemAct(String name = "", Closure cl = null) {
        throw new UnsupportedOperationException("Split cannot have standalone ElemAct, it shall be within a Block")
    }

    public void CompAct(String name = "", Closure cl) {
        throw new UnsupportedOperationException("Split cannot have standalone CompAct, it shall be within a Block")
    }

    public void Split(String name = "", Types type, Closure cl) {
        throw new UnsupportedOperationException("Split cannot have standalone Split, it shall be within a Block")
    }
}
