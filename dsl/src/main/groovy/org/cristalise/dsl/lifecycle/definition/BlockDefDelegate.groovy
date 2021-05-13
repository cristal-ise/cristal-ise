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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_EXPR
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_VERSION

import org.cristalise.dsl.property.PropertyDelegate
import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.NextDef
import org.cristalise.kernel.lifecycle.WfVertexDef;

import groovy.transform.CompileStatic


@CompileStatic
abstract class BlockDefDelegate extends PropertyDelegate {

    public WfVertexDef lastSlotDef = null
    public CompositeActivityDef compActDef

    BlockDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef) {
        compActDef = parent
        lastSlotDef = originSlotDef
    }

    public NextDef addAsNext(WfVertexDef newSlotDef) {
        NextDef nextDef = null
        if(lastSlotDef) nextDef = compActDef.addNextDef(lastSlotDef, newSlotDef)
        else            compActDef.getChildrenGraphModel().setStartVertexId(newSlotDef.ID)

        lastSlotDef = newSlotDef;

        return nextDef
    }

    public NextDef addActDefAsNext(String actName, ActivityDef actDef) {
        def newSlotDef = compActDef.addExistingActivityDef(actName, actDef, new GraphPoint())
        return addAsNext(newSlotDef)
    }

    def LoopDef(Closure cl) {
        def loopDef =  new LoopDefDelegate(compActDef, lastSlotDef)
        return loopDef.processClosure(cl)
    }

    def ElemActDef(ActivityDef actDef) {
        return addActDefAsNext(actDef.actName, actDef)
    }

    def ElemActDef(String actName, ActivityDef actDef) {
        return addActDefAsNext(actName, actDef)
    }

    def CompActDef(CompositeActivityDef actDef) {
        return addActDefAsNext(actDef.actName, actDef)
    }

    def CompActDef(String actName, CompositeActivityDef actDef) {
        return addActDefAsNext(actName, actDef)
    }

    public void setRoutingExpr(GraphableVertex aSplit, String exp) {
        assert aSplit instanceof AndSplitDef, "'$aSplit.name' must be instance of SplitDef"

        aSplit.setBuiltInProperty(ROUTING_EXPR, exp)
    }

    public void setRoutingScript(GraphableVertex aSplit, String name, Integer version) {
        assert aSplit instanceof AndSplitDef, "'$aSplit.name' must be instance of SplitDef"

        aSplit.setBuiltInProperty(ROUTING_SCRIPT_NAME,    name);
        aSplit.setBuiltInProperty(ROUTING_SCRIPT_VERSION, version)
    }
}
