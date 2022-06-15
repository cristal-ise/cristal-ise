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
package org.cristalise.dsl.lifecycle.definition;

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PAIRING_ID
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_EXPR
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_VERSION

import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.lifecycle.NextDef
import org.cristalise.kernel.lifecycle.WfVertexDef
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class AndSplitDefDelegate extends SplitDefDelegate {

    AndSplitDef andSplitDef
    JoinDef     joinDef

    public AndSplitDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef, Map<String, Object> initialProps) {
        super(parent, originSlotDef)

        andSplitDef = (AndSplitDef) compActDef.newChild("", Types.AndSplit, 0, new GraphPoint())
        joinDef     = (JoinDef)     compActDef.newChild("", Types.Join, 0, new GraphPoint())

        String pairingId = "AndSplit${andSplitDef.getID()}"
        setPairingId(pairingId, andSplitDef, joinDef)

        setInitialProperties(andSplitDef, initialProps)
    }

    @Override
    public void initialiseDelegate() {
        addAsNext(andSplitDef)
    }

    @Override
    public void finaliseDelegate() {
        lastSlotDef = joinDef

        props.each { k, v ->
            andSplitDef.properties.put(k, v, props.getAbstract().contains(k))
        }
    }

    @Override
    public NextDef finaliseBlock(WfVertexDef newLastSlotDef, NextDef currentFirstEdge, Object alias) {
        log.debug('finaliseBlock() - linking lastSlotDef:{} to join:{}', newLastSlotDef, joinDef)
        def lastNextDef = compActDef.addNextDef(newLastSlotDef, joinDef)

        if (alias) {
            if (currentFirstEdge) currentFirstEdge.setBuiltInProperty(ALIAS, alias)
            else lastNextDef.setBuiltInProperty(ALIAS, alias)
        }

        return lastNextDef
    }

    @Override
    public BlockDefDelegate Block(Map<String, Object> initialProps = null, @DelegatesTo(BlockDefDelegate) Closure cl = null) {
        def blockD =  new BlockDefDelegate(compActDef, andSplitDef)

        if (cl) {
            blockD.processClosure(cl)
            finaliseBlock(blockD.lastSlotDef, blockD.firstEdge, initialProps?.Alias)
        }

        return blockD
    }

    @Override
    public LoopDefDelegate Loop(Map<String, Object> initialProps = null, @DelegatesTo(LoopDefDelegate) Closure cl = null) {
        def loopD =  new LoopDefDelegate(compActDef, andSplitDef, initialProps)

        if (cl) {
            loopD.processClosure(cl)
            finaliseBlock(loopD.lastSlotDef, loopD.firstEdge, initialProps?.Alias)
        }

        return loopD
    }

    @Override
    public AndSplitDefDelegate AndSplit(Map<String, Object> initialProps = null, @DelegatesTo(AndSplitDefDelegate) Closure cl = null) {
        def andD =  new AndSplitDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            andD.processClosure(cl)
            finaliseBlock(andD.lastSlotDef, andD.firstEdge, initialProps?.Alias)
        }

        return andD
    }

    @Override
    public OrSplitDefDelegate OrSplit(Map<String, Object> initialProps = null, @DelegatesTo(OrSplitDefDelegate) Closure cl) {
        def orD =  new OrSplitDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            orD.processClosure(cl)
            finaliseBlock(orD.lastSlotDef, orD.firstEdge, initialProps?.Alias)
        }

        return orD
    }

    @Override
    public XOrSplitDefDelegate XOrSplit(Map<String, Object> initialProps = null, @DelegatesTo(XOrSplitDefDelegate) Closure cl) {
        def xorD =  new XOrSplitDefDelegate(compActDef, lastSlotDef, initialProps)

        if (cl) {
            xorD.processClosure(cl)
            finaliseBlock(xorD.lastSlotDef, xorD.firstEdge, initialProps?.Alias)
        }

        return xorD
    }
}
