package org.cristalise.dsl.lifecycle.definition

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PAIRING_ID
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_EXPR
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_NAME
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.ROUTING_SCRIPT_VERSION

import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.WfVertexDef
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
        else {
            setRoutingExpr(splitDef, 'true')
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
