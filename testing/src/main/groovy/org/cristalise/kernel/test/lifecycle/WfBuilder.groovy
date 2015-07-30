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
import org.cristalise.kernel.lifecycle.instance.AndSplit
import org.cristalise.kernel.lifecycle.instance.CompositeActivity
import org.cristalise.kernel.lifecycle.instance.WfVertex
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
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
    ItemPath itemPath = null
    AgentPath agentPath = null

    StateMachine eaSM = null
    StateMachine caSM = null

    Workflow          wf     = null
    CompositeActivity rootCA = null
    Activity          act0   = null
    
    //The actual CompAct under construction
    CompositeActivity currentCA    = null
    WfVertex          prevVertex   = null
    boolean           firstVertext = true

    /**
     * 
     */
    public WfBuilder() {
        eaSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/Default.xml"));
        caSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/CompositeActivity.xml"));

        itemPath  = new ItemPath()
        agentPath = new AgentPath(new ItemPath(), "test")

        rootCA = new CompositeActivity()
        wf     = new Workflow(rootCA, new ServerPredefinedStepContainer())
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
     * @param act
     * @param status
     */
    public static void checkActStatus(Activity act, Map status) {
        assert act
        assert act.getStateName() == "$status.state"
        assert act.getActive() == status.active
    }

    public void requestAction(Activity act, String trans) {
        int transID = -1

        if(act instanceof CompositeActivity) transID = getTransID(caSM, trans)
        else                                 transID = getTransID(eaSM, trans)

        wf.requestAction(agentPath, act.path, itemPath, transID, "")
    }

    /**
     * 
     * @param doChecks
     * @param cl
     * @return
     */
    public Workflow buildWf(boolean doChecks = true, Closure cl) {
        assert cl, "WfBuilder only works with a valid Closure"

        currentCA = rootCA

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        act0 = (Activity)wf.search("workflow/domain/0")

        //FIXME: Move these test to separate WfInitilaiseSpecs test class
        if(doChecks) {
            checkActStatus(rootCA, [state: "Waiting", active: false])
            checkActStatus(act0,   [state: "Waiting", active: false])
        }

        wf.initialise(itemPath, agentPath)

        //FIXME: Move these test to separate WfInitilaiseSpecs test class
        if(doChecks) {
            checkActStatus(rootCA, [state: "Started", active: true])

            if(act0 instanceof CompositeActivity) {
                checkActStatus(act0, [state: "Started", active: true])
            }
            else {
                checkActStatus(act0, [state: "Waiting", active: true])
            }
        }

        currentCA  = null
        prevVertex = null

        return wf
    }

    /**
     * 
     * @return
     */
    public Activity ElemAct() {
        return ElemAct("")
    }

    /**
     * 
     * @param name
     * @return
     */
    public Activity ElemAct(String name) {
        def currentVertex = currentCA.newChild(Types.Atomic, name, firstVertext, null)
        prevVertex?.addNext(currentVertex)
        prevVertex = currentVertex
        firstVertext = false
        return (Activity)currentVertex
    }

    /**
     * 
     * @param cl
     * @return
     */
    public CompositeActivity CompAct(Closure cl) {
        return CompAct("", cl)
    }

    /**
     * 
     * @param name
     * @param cl
     * @return
     */
    public CompositeActivity CompAct(String name, Closure cl) {
        assert cl, "WfBuilder.CompAct only works with a valid Closure"

        def currentVertex = currentCA.newChild(Types.Composite, name, firstVertext, null)
        prevVertex?.addNext(currentVertex)

        firstVertext = true
        def prevCA = currentCA
        currentCA = (CompositeActivity)currentVertex

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()

        firstVertext = false
        currentCA = prevCA

        return (CompositeActivity)currentVertex
    }

    /**
     * 
     * @param cl
     * @return
     */
    public AndSplit AndSplit(Closure cl) {
        return AndSplit("", cl)
    }

    /**
     * 
     * @param name
     * @param cl
     * @return
     */
    public AndSplit AndSplit(String name, Closure cl) {
        assert cl, "WfBuilder.AndSplit only works with a valid Closure"
    }
}
