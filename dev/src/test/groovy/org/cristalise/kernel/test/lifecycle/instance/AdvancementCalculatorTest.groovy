/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
package org.cristalise.kernel.test.lifecycle.instance

import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.graph.model.Vertex
import org.cristalise.kernel.graph.traversal.GraphTraversal
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification

/**
 *
 */
class AdvancementCalculatorTest extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        inMemoryServer(null, true)
        wfBuilder = new WorkflowTestBuilder()
    }

    def setup() {}

    def cleanup() {
        if(wfBuilder && wfBuilder.wf) println Gateway.getMarshaller().marshall(wfBuilder.wf)
    }

    def cleanupSpec() {
        cristalCleanup()
    }

    def printVertex(v) {
        print " - $v.path"
        if(v instanceof Activity) println "- active:"+((Activity)v).active
        else println ""
    }

    List<WfVertex> getActiveActs(String fromVertex, int direction = GraphTraversal.kUp) {
        assert wfBuilder.vertexCache[fromVertex]

        List<WfVertex> activeActs = []

        GraphTraversal.getTraversal(
                            wfBuilder.vertexCache["rootCA"].getChildrenGraphModel(),
                            wfBuilder.vertexCache[fromVertex],
                            direction,
                            true
        ).each {
            printVertex(it)
            if(it instanceof Activity && ((Activity)it).active) activeActs.add(it)
        }

        return activeActs
    }

    def 'Loop(inner) to check GraphTraversal'() {
        given:
        wfBuilder.buildAndInitWf {
            Loop {
                ElemAct("inner")
            }
        }

        when:
        List<WfVertex> acts = getActiveActs('LoopJoin_last')

        then:
        acts && acts.size() == 1
        acts[0].name == 'inner'
    }

    def 'Two overlapping loops to test GraphTraversal'() {
        given:
        wfBuilder.buildAndInitWf {
            connect Join:      'Join1' to ElemAct:   'EA1'
            connect ElemAct:   'EA1'   to Join:      'Join2'
            connect Join:      'Join2' to LoopSplit: 'Loop1'
            connect LoopSplit: 'Loop1' to Join:      'Join1'  alias 'true'
            connect LoopSplit: 'Loop1' to ElemAct:   'EA2'    alias 'false'
            connect ElemAct:   'EA2'   to LoopSplit: 'Loop2'
            connect LoopSplit: 'Loop2' to Join:      'Join2'  alias 'true'
            connect LoopSplit: 'Loop2' to ElemAct:    'EA3'   alias 'false'

            setFirst('Join1')

            setRoutingScript('Loop1', 'javascript:"false";')
            setRoutingScript('Loop2', 'javascript:"true";')
        }

        when:
        wfBuilder.checkActStatus("EA1", [state: 'Waiting', active: true])

        then:
        ! getActiveActs('Join1')

        when:
        wfBuilder.requestAction("EA1", "Done")

        then:
        wfBuilder.checkActStatus("EA1", [state: 'Finished', active: false])
        wfBuilder.checkActStatus("EA2", [state: 'Waiting',  active: true])
        ! getActiveActs('Join2')
    }

    public void 'Complex unbalanced workflow to test GraphTraversal'() {
        given:
        wfBuilder.buildAndInitWf {
            connect ElemAct:   'first'        to OrSplit:   'DateSplit'
            connect OrSplit:   'DateSplit'    to Join:      'JoinTop'
            connect OrSplit:   'DateSplit'    to ElemAct:   'EA1'
            connect OrSplit:   'DateSplit'    to ElemAct:   'EA2'
            connect ElemAct:   'EA1'          to Join:      'DateJoin'
            connect ElemAct:   'EA2'          to Join:      'DateJoin'
            connect Join:      'DateJoin'     to ElemAct:   'EA3'
            connect ElemAct:   'EA3'          to Join:      'Join1'
            connect Join:      'Join1'        to LoopSplit: 'CounterLoop'
            connect Join:      'JoinTop'      to ElemAct:   'counter'
            connect ElemAct:   'counter'      to OrSplit:   'CounterSplit'
            connect OrSplit:   'CounterSplit' to Join:      'Join1' 
            connect OrSplit:   'CounterSplit' to Join:      'Join2'
            connect LoopSplit: 'CounterLoop'  to Join:      'JoinTop'
            connect LoopSplit: 'CounterLoop'  to Join:      'Join2'
            connect Join:      'Join2'        to ElemAct:   'last'

            setFirst('first')
        }

        when:
        Vertex[] vertices = GraphTraversal.getTraversal(wfBuilder.vertexCache["rootCA"].getChildrenGraphModel(),
                                                        wfBuilder.vertexCache["Join2"], 
                                                        GraphTraversal.kUp,
                                                        true)
        then:
        vertices.each {
            it
            printVertex(it)
        }
    }
}
