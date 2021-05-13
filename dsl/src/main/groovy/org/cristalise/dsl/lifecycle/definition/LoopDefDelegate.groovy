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

import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.lifecycle.WfVertexDef
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types

import groovy.transform.CompileStatic

@CompileStatic
class LoopDefDelegate extends BlockDefDelegate {

    public LoopDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef) {
        super(parent, originSlotDef)
    }

    public void processClosure(Closure cl) {
        assert cl, "Split only works with a valid Closure"

        JoinDef joinDefFirst = (JoinDef) compActDef.newChild("", Types.Join, 0, new GraphPoint())
        LoopDef loopDef      = (LoopDef) compActDef.newChild("", Types.LoopSplit, 0, new GraphPoint())
        JoinDef joinDefLast  = (JoinDef) compActDef.newChild("", Types.Join, 0, new GraphPoint())

        String pairingId = "Loop${loopDef.getID()}";
        loopDef     .setBuiltInProperty(PAIRING_ID, pairingId)
        joinDefFirst.setBuiltInProperty(PAIRING_ID, pairingId)

        addAsNext(joinDefFirst)

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        addAsNext(loopDef) // sets loop input to the lastSlotDef
        def nextLast = addAsNext(joinDefLast) // sets loop output to the joinDefLast
        def nextFirst = compActDef.addNextDef(loopDef, joinDefFirst) // sets loop output to the joinDefFirst

        nextFirst.setBuiltInProperty(ALIAS, 'true')
        nextLast.setBuiltInProperty(ALIAS, 'false')

        setRoutingExpr(loopDef, 'true')

        props.each { k, v ->
            loopDef.properties.put(k, v, props.getAbstract().contains(k))
        }
    }
}
