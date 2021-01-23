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
package org.cristalise.dsl.lifecycle.stateMachine

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.cristalise.kernel.lifecycle.instance.stateMachine.State
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lifecycle.instance.stateMachine.Transition


/**
 *
 */
@CompileStatic @Slf4j
class StateMachineDelegate {
    String ns = ""
    String name = ""
    int version = -1

    StateMachine sm = null
    
    Map<String, State>      stateCache = [:]
    Map<String, Transition> transCache = [:]

    public StateMachineDelegate(String ns, String n, int v) {
        ns = ns
        name = n
        version = v

        sm = new StateMachine(n, v)
        sm.namespace = ns
    }

    public void processClosure(Closure cl) {
        assert cl, "StateMachineDelegate only works with a valid Closure"

        cl.delegate = this
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    public void state(String stateName, Closure cl = null) {
        log.debug "state() - stateName: $stateName"
        assert stateName
        def state = stateCache[stateName]
        if(!state) {
            state = sm.createState(stateName)
            stateCache[stateName] = state
        }

//        if(cl) new StateDelegate(state).processClosure(cl)
    }

    public void transition(String transName, Map<String,String> states = null, Closure cl = null) {
        log.debug "transition() - transName: $transName, states: $states"
        assert transName

        def trans = transCache[transName]
        if(!trans){
            trans = sm.createTransition(transName)
            transCache[transName] = trans
        }

        if(states) {
            assert states.origin && states.target

            def origin = stateCache[states.origin]
            if(!origin) {
                origin = sm.createState(states.origin)
                stateCache[states.origin] = origin
            }

            def target = stateCache[states.target]
            if(!target) {
                target = sm.createState(states.target)
                stateCache[states.target] = target
            }

            trans.originStateId = origin.id
            trans.targetStateId = target.id
        }

        if(cl) new TransitionDelegate(trans).processClosure(cl)
    }

    public void initialState(String stateName) {
        log.debug "initialState() - stateName: $stateName"
        assert stateCache && stateCache[stateName]

        sm.initialState = stateCache[stateName]
    }

    public void finishingState(String...stateNames) {
        log.debug "finishingState() - states: $stateNames"

        for (s in stateNames) {
            assert stateCache && stateCache[s]
            stateCache[s].finished = true
        }
    }
}
