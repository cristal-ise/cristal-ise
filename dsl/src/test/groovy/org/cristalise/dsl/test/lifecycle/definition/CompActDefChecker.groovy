package org.cristalise.dsl.test.lifecycle.definition


import org.cristalise.kernel.graph.model.DirectedEdge
import org.cristalise.kernel.graph.model.Vertex
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.ActivitySlotDef
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.lifecycle.WfVertexDef

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic @Slf4j
class CompActDefChecker {
    
    public CompositeActivityDef caDef

    protected Map<String, WfVertexDef> vertexCache = null

    CompActDefChecker(CompositeActivityDef ca) {
        caDef = ca
    }

    private Vertex getOutVertex(Vertex currentVertex, Object sequenceData) {
        def verticles = caDef.childrenGraphModel.getOutVertices(currentVertex)
        def foundVertex = null

        if (sequenceData instanceof String) {
            foundVertex = verticles.find { it.name == sequenceData }
            assert foundVertex : "CurrentVertex:$currentVertex has no outVertex with name:$sequenceData"
        }
        else if (sequenceData instanceof Class) {
            foundVertex = verticles.find { ((Class)sequenceData).isInstance(it) }
            assert foundVertex : "CurrentVertex:$currentVertex has no outVertex with class:$sequenceData"
        }
        else {
            assert false : "Uncovered sequenceData:$sequenceData"
        }

        return foundVertex
    }

    /**
     *
     * @param names
     */
    public void checkSequence(Object... sequence) {
        log.info "checkSequence() - '$sequence'"

        assert sequence && sequence.length > 1

        def currentVertex = caDef.getChildrenGraphModel().startVertex

        if (sequence[0] instanceof String) {
            assert currentVertex.name == sequence[0] : "firstVertex shall have name: $sequence[0]"
        }
        else if (sequence[0] instanceof Class) {
            assert ((Class)sequence[0]).isInstance(currentVertex) : "firstVertex shall be class:$sequence[0]"
        }

        for(int i = 1; i < sequence.length; i++) {
            currentVertex = getOutVertex(currentVertex, sequence[i])
        }
    }

    public void checkLoop(JoinDef startJoin, Class loopBodyStartClass) {
        def loopBodyStart = caDef.childrenGraphModel.getOutVertices(startJoin)
        assert loopBodyStart.length == 1
        assert loopBodyStartClass.isInstance(loopBodyStart[0])

        //FIXME: hack to make nested loop test case green
        if (loopBodyStartClass.equals(ActivitySlotDef)) {
            def loop = caDef.childrenGraphModel.getOutVertices(loopBodyStart[0])
            assert loop.length == 1
            assert loop[0] instanceof LoopDef
        }
    }
}

