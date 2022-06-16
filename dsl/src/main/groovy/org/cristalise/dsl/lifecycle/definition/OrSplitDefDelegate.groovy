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

import org.cristalise.kernel.graph.model.GraphPoint
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.NextDef
import org.cristalise.kernel.lifecycle.OrSplitDef
import org.cristalise.kernel.lifecycle.WfVertexDef
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class OrSplitDefDelegate extends SplitDefDelegate {

    OrSplitDef orSplitDef
    JoinDef    joinDef

    public OrSplitDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef, Map<String, Object> initialProps) {
        super(parent, originSlotDef)

        orSplitDef = (OrSplitDef) compActDef.newChild("", Types.OrSplit, 0, new GraphPoint())
        joinDef    = (JoinDef)    compActDef.newChild("", Types.Join, 0, new GraphPoint())

        String pairingId = "OrSplit${orSplitDef.getID()}"
        setPairingId(pairingId, orSplitDef, joinDef)

        setInitialProperties(orSplitDef, initialProps)
    }

    @Override
    public void initialiseDelegate() {
        addAsNext(orSplitDef)
    }

    @Override
    public void finaliseDelegate() {
        lastSlotDef = joinDef

        props.each { k, v ->
            orSplitDef.properties.put(k, v, props.getAbstract().contains(k))
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
}
