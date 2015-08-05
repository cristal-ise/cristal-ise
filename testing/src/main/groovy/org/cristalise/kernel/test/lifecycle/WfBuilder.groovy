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

package org.cristalise.kernel.test.lifecycle

import groovy.transform.CompileStatic

import org.cristalise.kernel.lifecycle.instance.Activity
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.instance.predefined.server.ServerPredefinedStepContainer
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition
import org.cristalise.kernel.lookup.AgentPath
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.process.Gateway

/**
 *
 */
@CompileStatic
class WfBuilder {
    private ItemPath  itemPath  = null
    private AgentPath agentPath = null

    private StateMachine eaSM = null
    private StateMachine caSM = null

    private Workflow wf = null

    private Map<String,Activity> actCache = [:]

    /**
     * 
     */
    public WfBuilder() {
        eaSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/Default.xml"));
        caSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/CompositeActivity.xml"));

        itemPath  = new ItemPath()
        agentPath = new AgentPath(new ItemPath(), "test")
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
        checkActStatus(actCache[name], status)
    }

    /**
     * 
     * @param act
     * @param status
     */
    public static void checkActStatus(Activity act, Map status) {
        assert act, "Activity '$act.name' shall NOT be null"
        assert act.getStateName() == "$status.state", "Activity '$act.name' state is NOT correct"
        assert act.getActive() == status.active, "Activity '$act.name' shall ${(status.active) ? '' : 'NOT '}be active"
    }

    public void checkNext(String from, String to) {
        assert actCache[from].next.terminusVertexId != actCache[from].ID, "Vertex '$from' shall not be linked to ITSELF"
        assert actCache[from].next.terminusVertexId == actCache[to].ID, "Vertex '$from' shall be linked to '$to'"
    }

    public void checkActPath(String path, String name) {
        assert wf.search(path) == actCache[name], "Activity '$name' equals '$path'"
    }

    /**
     * 
     * @param name
     * @param trans
     */
    public void requestAction(String name, String trans) {
        requestAction(actCache[name], trans)
    }

    /**
     * 
     * @param act
     * @param trans
     */
    public void requestAction(Activity act, String trans) {
        int transID = -1

        if(act instanceof CompositeActivity) transID = getTransID(caSM, trans)
        else                                 transID = getTransID(eaSM, trans)

        wf.requestAction(agentPath, act.path, itemPath, transID, "")
    }

    /**
     * 
     * @param cl
     * @return
     */
    public Workflow buildWf(boolean doChecks = true, Closure cl) {
        assert cl, "buildWf() only works with a valid Closure"

        CompositeActivity rootCA = new CompositeActivity()
        wf = new Workflow(rootCA, new ServerPredefinedStepContainer())
        actCache['rootCA'] = rootCA

        new CompActDelegate('rootCA', rootCA, actCache).processClosure(cl)

        //FIXME: Move these checks to separate WfInitilaiseSpecs test class
        if(doChecks) {
            def act0 = wf.search("workflow/domain/0")

            checkActStatus('rootCA', [state: "Waiting", active: false])
            if(act0 instanceof Activity) checkActStatus(act0,     [state: "Waiting", active: false])
        }
    }
        
    /**
     * 
     * @param doChecks
     * @param cl
     * @return
     */
    public Workflow buildAndInitWf(boolean doChecks = true, Closure cl) {
        buildWf(doChecks, cl)

        wf.initialise(itemPath, agentPath)

        //FIXME: Move these checks to separate WfInitilaiseSpecs test class
        if(doChecks) {
            checkActStatus('rootCA', [state: "Started", active: true])

            def act0 = wf.search("workflow/domain/0")

            if(act0 instanceof CompositeActivity) {
                checkActStatus(act0, [state: "Started", active: true])
            }
            else if(act0 instanceof Activity) {
                checkActStatus(act0, [state: "Waiting", active: true])
            }
        }

        return wf
    }
}
