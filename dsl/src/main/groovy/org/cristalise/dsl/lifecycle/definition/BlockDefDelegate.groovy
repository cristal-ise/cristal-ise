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
package org.cristalise.dsl.lifecycle.definition

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PAIRING_ID

import org.cristalise.dsl.property.PropertyDelegate
import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.ActivitySlotDef
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.NextDef
import org.cristalise.kernel.lifecycle.WfVertexDef;
import org.cristalise.kernel.lifecycle.instance.WfVertex

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j


@CompileStatic @Slf4j
class BlockDefDelegate extends PropertyDelegate {

    public WfVertexDef lastSlotDef = null
    public CompositeActivityDef compActDef

    BlockDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef) {
        compActDef = parent
        lastSlotDef = originSlotDef
    }

    public void processClosure(Closure cl) {
        assert cl, "Block only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    protected NextDef addAsNext(WfVertexDef newSlotDef) {
        log.debug('addAsNext() - newSlotDef:{} lastSlotDef:{}', newSlotDef, lastSlotDef)

        NextDef nextDef = null
        if(lastSlotDef) nextDef = compActDef.addNextDef(lastSlotDef, newSlotDef)
        else            compActDef.getChildrenGraphModel().setStartVertexId(newSlotDef.ID)

        lastSlotDef = newSlotDef;

        return nextDef
    }

    protected ActivitySlotDef addActDefAsNext(String actName, ActivityDef actDef) {
        def newSlotDef = compActDef.addExistingActivityDef(actName, actDef, new GraphPoint())
        addAsNext(newSlotDef)
        return newSlotDef
    }

    protected void setPairingId(id, GraphableVertex...vertices) {
        for (v in vertices) v.setBuiltInProperty(PAIRING_ID, id)
    }

    def Loop(Map<String, Object> props = null, @DelegatesTo(LoopDefDelegate) Closure cl) {
        def loopD =  new LoopDefDelegate(compActDef, lastSlotDef, props)
        loopD.processClosure(cl)

        lastSlotDef = loopD.joinDefLast

        return loopD.loopDef
    }

    def Act(ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actDef.actName, actDef, cl)
    }

    def Act(String actName, ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        def newSlotDef = addActDefAsNext(actName, actDef)

        if (cl) {
            def propD = new PropertyDelegate()
            propD.processClosure(cl)
            propD.props.each { k, v ->
                newSlotDef.properties.put(k, v, props.getAbstract().contains(k))
            }
        }
        return newSlotDef
    }

    // Alias of method Act(...)
    def ElemActDef(ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actDef.actName, actDef, cl)
    }

    // Alias of method Act(...)
    def ElemActDef(String actName, ActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actName, actDef, cl)
    }

    // Alias of method Act(...)
    def CompActDef(CompositeActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actDef.actName, actDef, cl)
    }

    // Alias of method Act(...)
    def CompActDef(String actName, CompositeActivityDef actDef, @DelegatesTo(PropertyDelegate) Closure cl = null) {
        return Act(actName, actDef, cl)
    }

    def AndSplit(Map<String, Object> props = null, @DelegatesTo(AndSplitDefDelegate) Closure cl) {
        def andD =  new AndSplitDefDelegate(compActDef, lastSlotDef, props)
        andD.processClosure(cl)

        lastSlotDef = andD.lastSlotDef

        return andD.andSplitDef
    }
}
