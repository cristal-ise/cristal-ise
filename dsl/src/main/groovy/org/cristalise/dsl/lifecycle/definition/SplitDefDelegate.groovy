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
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_EXPR
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_VERSION

import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.lifecycle.WfVertexDef
import org.cristalise.kernel.scripting.Script
import groovy.transform.CompileStatic

@CompileStatic
abstract class SplitDefDelegate extends BlockDefDelegate {

    SplitDefDelegate(CompositeActivityDef parent, WfVertexDef originSlotDef) {
        super(parent, originSlotDef)
    }

    protected void setPairingId(id, GraphableVertex...vertices) {
        for (v in vertices) v.setBuiltInProperty(PAIRING_ID, id)
    }

    protected void setInitialProperties(WfVertexDef splitDef, Map<String, Object> initialProps) {
        if(initialProps?.javascript) {
            setRoutingScript(splitDef, (String)"javascript:${initialProps.javascript};", null);
            initialProps.remove('javascript')
        }
        else if(initialProps?.groovy) {
            setRoutingScript(splitDef, (String)"groovy:${initialProps.groovy}", null);
            initialProps.remove('groovy')
        }
        else if (initialProps?.RoutingScript) {
            if (initialProps.RoutingScript instanceof Script) {
                def script = initialProps.RoutingScript as Script
                setRoutingScript(splitDef, script.getName(), script.getVersion());
            }
            else if (initialProps.RoutingScript instanceof String) {
                def nameAndVersion = ((String)initialProps.RoutingScript).split(':')
                def name = nameAndVersion[0]
                def version =  nameAndVersion.length == 2 ? nameAndVersion[1] : '0'
                setRoutingScript(splitDef, name, version as Integer)
            }
            initialProps.remove('RoutingScript')
        }
        else if (initialProps?.RoutingExpr) {
            setRoutingExpr(splitDef, initialProps.RoutingExpr as String)
            initialProps.remove('RoutingExpr')
        }
        else if (splitDef instanceof LoopDef) {
            setRoutingExpr(splitDef, 'false')
        }

        if (initialProps) initialProps.each { k, v -> props.put(k, v, false) }
    }

    protected void setRoutingExpr(WfVertexDef splitDef, String exp) {
        splitDef.setBuiltInProperty(ROUTING_EXPR, exp)
    }

    protected void setRoutingScript(WfVertexDef splitDef, String name, Integer version) {
        splitDef.setBuiltInProperty(ROUTING_SCRIPT_NAME,    name);
        splitDef.setBuiltInProperty(ROUTING_SCRIPT_VERSION, version)
    }
}
