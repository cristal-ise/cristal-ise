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
package org.cristalise.dsl.test.lifecycle.instance

import groovy.transform.CompileStatic

import org.cristalise.dsl.lifecycle.instance.WorkflowBuilder
import org.cristalise.kernel.graph.model.DirectedEdge
import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.Join
import org.cristalise.kernel.lifecycle.instance.Split
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.outcome.Outcome
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.utils.Logger


/**
 *
 */
@CompileStatic
class WorkflowTestBuilder extends WorkflowBuilder {
    ItemPath  itemPath  = null
    AgentPath agentPath = null

    StateMachine eaSM = null
    StateMachine caSM = null

    /**
     * 
     */
    public WorkflowTestBuilder() {
        super()

        eaSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/Default.xml"));
        caSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/CompositeActivity.xml"));

        itemPath  = new ItemPath()
        agentPath = new AgentPath(new ItemPath(), "WorkflowTestBuilder")
    }

    /**
     * 
     * @param sm
     * @param name
     * @return
     */
    public static int getTransID(StateMachine sm, String name) {
        Transition t = sm.getTransitions().find{ it.name == name }
        assert t, "Transition name '$name' is invalid for StateMachine $sm.name"
        return t.id
    }

    /**
     * 
     * @param name
     * @param status
     */
    public void checkActStatus(String name, Map status) {
        assert vertexCache[name], "Activity '$name' is NOT found in cache"
        checkActStatus((Activity)vertexCache[name], status)
    }

    /**
     * 
     * @param act
     * @param status
     */
    public static void checkActStatus(Activity act, Map status) {
        assert act
        assert act.getStateName() == "$status.state", "Activity '$act.name' state is NOT correct"
        assert act.getActive() == status.active, "Activity '$act.name' shall ${(status.active) ? '' : 'NOT '}be active"
    }

    /**
     * 
     * @param splitName
     * @param toNames
     */
    public void checkSplit(String splitName, List<String> toNames ) {
        Logger.msg 5, "checkSplit() - Split '$splitName' -> $toNames"
        
        assert vertexCache.containsKey(splitName), "Split '$splitName' is missing from cache"
        toNames.each { assert vertexCache.containsKey(it), "Vertex '$it' is missing from cache" }

        List<Integer> splitOutEdgeIDs = vertexCache[splitName].getOutEdges().collect { DirectedEdge e ->  e.terminusVertexId }.sort()
        List<Integer> expectedIDs = toNames.collect { vertexCache[it].ID }.sort()

        assert splitOutEdgeIDs == expectedIDs
    }

    /**
     * 
     * @param joinName
     * @param fromNames
     */
    public void checkJoin(String joinName, List<String> fromNames) {
        Logger.msg 5, "checkJoin() - Split '$joinName' -> $fromNames"

        assert vertexCache.containsKey(joinName), "Join '$joinName' is missing from cache"
        fromNames.each { assert vertexCache.containsKey(it), "Vertex '$it' is missing from cache" }

        List<Integer> joinInEdgeIDs = vertexCache[joinName].getInEdges().collect { DirectedEdge e ->  e.originVertexId }.sort()
        List<Integer> expectedIDs  = fromNames.collect { vertexCache[it].ID }.sort()

        assert  joinInEdgeIDs == expectedIDs
    }

    /**
     * 
     * @param names
     */
    public void checkSequence(String... names) {
        assert names.size() > 1

        Logger.msg 5, "checkSequence() - '$names'"

        for(int i = 1; i < names.size(); i++) {
            checkOneToOneNext(names[i-1], names[i])
        }
    }

    /**
     * 
     * @param from
     * @param to
     */
    public void checkOneToOneNext(String from, String to) {
        checkNext(from, to)
        assert vertexCache[from].getOutEdges().size() == 1
        assert vertexCache[to  ].getInEdges().size() == 1
    }

    /**
     * 
     * @param from
     * @param to
     */
    public void checkNext(String from, String to) {
        Logger.msg 5, "checkNext() - Vertex '$from' -> '$to'"
        WfVertex fromV = vertexCache[from]
        
        assert fromV, "Vertex '$from' is missing from cache"

        List<Integer> fromIDs = []

        if(fromV instanceof Activity) {
            assert ((Activity)fromV).next, "Vertex '$from' has NO next"
            int id = ((Activity)fromV).next.terminusVertexId
            assert id != fromV.ID, "Vertex '$from' shall NOT be linked to ITSELF"
            fromIDs.add(id)
        }
        else if(fromV instanceof Split) {
            fromIDs = fromV.getOutEdges().collect { DirectedEdge e ->  e.terminusVertexId }
        }
        else if(fromV instanceof Join) {
            int id = fromV.getOutEdges()[0].terminusVertexId
            assert id != fromV.ID, "Vertex '$from' shall NOT be linked to ITSELF"
            fromIDs.add(id)
        }

        Logger.msg 5, "checkNext() - fromIDs: $fromIDs"

        assert fromIDs.contains(vertexCache[to].ID), "Vertex '$from' shall be linked to '$to'"
    }

    /**
     * 
     * @param path
     * @param name
     */
    public void checkActPath(String name, String path) {
        assert vertexCache[name]
        assert wf.search(path).is(vertexCache[name]), "Activity '$name' equals '$path'"
    }

    /**
     * 
     * @param name
     * @param trans
     * @param outcome
     */
    public void requestAction(String name, String trans, Outcome outcome) {
        requestAction((Activity)vertexCache[name], trans, outcome)
    }

    /**
     * 
     * @param name
     * @param trans
     */
    public void requestAction(String name, String trans) {
        requestAction((Activity)vertexCache[name], trans, null)
    }

    /**
     * 
     * @param act
     * @param trans
     */
    public void requestAction(Activity act, String trans, Outcome outcome) {
        Logger.msg 1, "WorkflowTestBuilder.requestAction() - $act.path, $trans"

        int transID = -1
        String requestData = null
        
        if(outcome != null) requestData = outcome.getData()

        if(act instanceof CompositeActivity) transID = getTransID(caSM, trans)
        else                                 transID = getTransID(eaSM, trans)

        wf.requestAction(agentPath, act.path, itemPath, transID, requestData)
    }

    /**
     * 
     */
    public void initialise() {
        wf.initialise(itemPath, agentPath)
    }

    /**
     * 
     * @param cl
     * @return
     */
    public Workflow buildAndInitWf(Closure cl) {
        super.build(cl)
        initialise()
        return wf
    }
}
