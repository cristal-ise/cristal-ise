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
package org.cristalise.dsl.test.lifecycle.stateMachine

import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.test.CristalTestSetup

import spock.lang.Ignore
import spock.lang.Specification


/**
 *
 */
class SMBuilderSpecs extends Specification implements CristalTestSetup {
    public void setup()   { inMemorySetup() }
    public void cleanup() { cristalCleanup() }

    def 'SM containing a single State is valid'() {
        when:
        def smb = StateMachineBuilder.build("testing", "dummySM", 0) {
            state("Idle")
        }

        then:
        smb.sm
        smb.sm.validate()
        smb.sm.getStates().find { it.name == "Idle" }
    }

    def 'SM containing a single State and Transition is valid'() {
        when:
        def smb = StateMachineBuilder.build("testing", "dummySM", 0) {
            transition("Fire", [origin:'Idle', target: 'Idle'])
        }

        then:
        smb.sm
        smb.sm.validate()
        smb.sm.getStates().find { it.name == "Idle" }
        smb.sm.getTransitions().find { it.name == "Fire" }.originState.name == "Idle"
        smb.sm.getTransitions().find { it.name == "Fire" }.targetState.name == "Idle"
    }

    def 'SM containing a single Transition is NOT valid'() {
        when:
        def smb = StateMachineBuilder.build("testing", "dummySM", 0) {
            transition("Useless")
        }

        then:
        smb.sm
        !smb.sm.validate()
    }

    @Ignore("unimplemented")
    def 'Builder can edit existing StateMachine'() {
        when: "the Skip transition is added"
        def smb = StateMachineBuilder.update("", "Default", 0) {
            transition("Skip", [origin: "Waiting", target: "Finished"]) {
                property(enabledProp: "Skippable", reservation:"clear")
            }
        }

        then:
        smb.sm
        smb.sm.validate()
    }

    def 'Build StateMachine using builder methods'() {
        when:
        def smb = StateMachineBuilder.build("testing", "SkipStateMachine", 0) {
            transition("Start", [origin: "Waiting", target: "Started"]) {
                property reservation: "set"
            }
            transition("Skip", [origin: "Waiting", target: "Finished"]) {
                property(enabledProp: "Skippable", reservation:"clear")
            }
            transition("Complete", [origin: "Started", target: "Finished"]) {
                property(reservation: "clear")
                outcome(name:"\${SchemaType}", version:"\${SchemaVersion}")
                script( name:"\${ScriptName}", version:"\${ScriptVersion}")
            }
            initialState("Waiting")
            finishingState("Finished")
        }

        then:
        smb.sm
        smb.sm.validate()
        smb.sm.getTransitions().find { it.name == "Start" }.originState.name == "Waiting"
        smb.sm.getTransitions().find { it.name == "Start" }.targetState.name == "Started"
        smb.sm.getTransitions().find { it.name == "Skip"  }.originState.name == "Waiting"
        smb.sm.getTransitions().find { it.name == "Skip"  }.targetState.name == "Finished"
    }

    @Ignore("deprecated")
    def 'Build StateMachine using XML builder'() {
        expect:
        StateMachineBuilder.build("testing", "SkipStateMachine", 0) {
            State(id: "0", name: "Waiting")
            State(id: "1", name: "Started")
            State(id: "2", name: "Finished", proceeds: "true")

            Transition(id:"1", name:"Start",    origin:"0", target:"1", reservation:"set")
            Transition(id:"3", name:"Skip",     origin:"0", target:"2", enablingProperty: "Skippable", reservation:"clear")
            Transition(id:"2", name:"Complete", origin:"1", target:"2", reservation:"clear") {
                Outcome(name:"\${SchemaType}", version:"\${SchemaVersion}")
                Script( name:"\${ScriptName}", version:"\${ScriptVersion}")
            }
        }
    }
}
