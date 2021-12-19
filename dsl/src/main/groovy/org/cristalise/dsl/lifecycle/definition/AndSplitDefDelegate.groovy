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
    }

    public void processClosure(Closure cl) {
        assert cl, "Split only works with a valid Closure"

        addAsNext(andSplitDef)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        lastSlotDef = joinDef

        props.each { k, v ->
            andSplitDef.properties.put(k, v, props.getAbstract().contains(k))
        }
    }

    def Block(@DelegatesTo(BlockDefDelegate) Closure cl) {
        def blockD =  new BlockDefDelegate(compActDef, andSplitDef)
        blockD.processClosure(cl)

        //link to the end of the current Block with the Join of the AndSplit
        log.debug('Block() - linking lastSlotDef:{} to join:{}', blockD.lastSlotDef, joinDef)
        compActDef.addNextDef(blockD.lastSlotDef, joinDef)

        return blockD.lastSlotDef
    }

    @Override
    def LoopInfinitive(@DelegatesTo(LoopDefDelegate) Closure cl) {
        // This shall add the conditions to make the infinitive
        return Loop([groovy: true] as Map, cl)
    }

    @Override
    def Loop(Map<String, Object> initialProps = null, @DelegatesTo(LoopDefDelegate) Closure cl) {
        def loopD =  new LoopDefDelegate(compActDef, andSplitDef, null)
        loopD.processClosure(cl)

        //link to the end of the current Block with the Join of the AndSplit
        log.debug('Loop() - linking lastSlotDef:{} to join:{}', loopD.lastSlotDef, joinDef)
        compActDef.addNextDef(loopD.lastSlotDef, joinDef)

        return loopD.lastSlotDef
    }

    @Override
    def AndSplit(Map<String, Object> props = null, @DelegatesTo(AndSplitDefDelegate) Closure cl) {
        def andD =  new AndSplitDefDelegate(compActDef, lastSlotDef, props)
        andD.processClosure(cl)

        //link to the end of the current Block with the Join of the AndSplit
        log.debug('AndSplit() - linking lastSlotDef:{} to join:{}', andD.lastSlotDef, joinDef)
        compActDef.addNextDef(andD.lastSlotDef, joinDef)

        return andD.andSplitDef
    }

    @Override
    def OrSplit(Map<String, Object> props = null, @DelegatesTo(OrSplitDefDelegate) Closure cl) {
        def orD =  new OrSplitDefDelegate(compActDef, lastSlotDef, props)
        orD.processClosure(cl)

        //link to the end of the current Block with the Join of the AndSplit
        log.debug('OrSplit() - linking lastSlotDef:{} to join:{}', orD.lastSlotDef, joinDef)
        compActDef.addNextDef(orD.lastSlotDef, joinDef)

        return orD.orSplitDef
    }

    @Override
    def XOrSplit(Map<String, Object> props = null, @DelegatesTo(XOrSplitDefDelegate) Closure cl) {
        def xorD =  new XOrSplitDefDelegate(compActDef, lastSlotDef, props)
        xorD.processClosure(cl)

        //link to the end of the current Block with the Join of the AndSplit
        log.debug('XOrSplit() - linking lastSlotDef:{} to join:{}', xorD.lastSlotDef, joinDef)
        compActDef.addNextDef(xorD.lastSlotDef, joinDef)

        return xorD.xorSplitDef
    }
}
