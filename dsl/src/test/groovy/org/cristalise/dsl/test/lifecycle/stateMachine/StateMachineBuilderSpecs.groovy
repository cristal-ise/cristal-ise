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
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Ignore
import spock.lang.Specification


/**
 *
 */
class StateMachineBuilderSpecs extends Specification implements CristalTestSetup {
    public void setup()   { inMemorySetup() }
    public void cleanup() { cristalCleanup() }

    def 'SM containing a single State is valid'() {
        when:
        def builder = StateMachineBuilder.build("testing", "dummySM", 0) {
            state("Idle")
        }

        then:
        builder.sm && builder.sm.isCoherent()
        builder.sm.getStates().find { it.name == "Idle" }
    }

    def 'SM containing a single State and Transition is valid'() {
        when:
        def builder = StateMachineBuilder.build("testing", "dummySM", 0) {
            transition("Fire", [origin:'Idle', target: 'Idle'])
        }

        then:
        builder.sm
        builder.sm.validate()
        builder.sm.getStates().find { it.name == "Idle" }
        builder.sm.getTransitions().find { it.name == "Fire" }.originState.name == "Idle"
        builder.sm.getTransitions().find { it.name == "Fire" }.targetState.name == "Idle"
    }

    def 'SM containing a single Transition is NOT valid'() {
        when:
        def builder = StateMachineBuilder.build("testing", "dummySM", 0) {
            transition("Useless")
        }

        then:
        builder.sm && ! builder.sm.isCoherent()
    }

    @Ignore("unimplemented")
    def 'Builder can edit existing StateMachine'() {
        when: "the Skip transition is added"
        def builder = StateMachineBuilder.update("", "Default", 0) {
            transition("Skip", [origin: "Waiting", target: "Finished"]) {
                property(enabledProp: "Skippable", reservation:"clear")
            }
        }

        then:
        builder.sm && builder.sm.isCoherent()
    }

    def 'Build Default StateMachine and crosscheck it with Kernel version'() {
        when:
        StateMachine defaultSM = (StateMachine)Gateway.getMarshaller().unmarshall(Gateway.getResource().getTextResource(null, "boot/SM/Default.xml"));

        def builder = StateMachineBuilder.build("testing", "Default", 0) {
            transition("Done", [origin: "Waiting", target: "Finished"]) {
                outcome(name:"\${SchemaType}", version:"\${SchemaVersion}")
                script( name:"\${ScriptName}", version:"\${ScriptVersion}")
                query(  name:"\${QueryName}",  version:"\${QueryVersion}")
            }
            transition("Start", [origin: "Waiting", target: "Started"]) {
                property reservation: "set"
            }
            transition("Complete", [origin: "Started", target: "Finished"]) {
                property(reservation: "clear")
                outcome(name: "\${SchemaType}", version:"\${SchemaVersion}")
                script( name: "\${ScriptName}", version:"\${ScriptVersion}")
                query(  name:"\${QueryName}",  version:"\${QueryVersion}")
            }
            transition("Suspend", [origin: "Started", target: "Suspended"]) {
                outcome(name: "Errors", version: "0")
            }
            transition("Resume", [origin: "Suspended", target: "Started"]) {
                property(reservation: "preserve")
                property(roleOverride: "Admin")
            }
            transition("Proceed", [origin: "Finished", target: "Finished"])
 
            initialState("Waiting")
            finishingState("Finished")
        }

        then:
        builder.sm && builder.sm.isCoherent()
        //KernelXMLUtility.compareXML(Gateway.getMarshaller().marshall(defaultSM), builder.smXML)
    }

    def 'Build Trigger StateMachine'() {
        when:
        def builder = StateMachineBuilder.build("testing", "TriggerStateMachine", 0) {
            transition("Done", [origin: "Waiting", target: "Finished"]) {
                outcome(name:"\${SchemaType}", version:"\${SchemaVersion}")
                script( name:"\${ScriptName}", version:"\${ScriptVersion}")
                query(  name:"\${QueryName}",  version:"\${QueryVersion}")
            }
            transition("Start", [origin: "Waiting", target: "Started"]) {
                property reservation: "set"
            }
            transition("Complete", [origin: "Started", target: "Finished"]) {
                outcome(name: "\${SchemaType}", version:"\${SchemaVersion}")
                script( name: "\${ScriptName}", version:"\${ScriptVersion}")
                query(  name: "\${QueryName}",  version:"\${QueryVersion}")
                property(reservation: "clear")
            }
            transition("Warning", [origin: "Started", target: "Started"]) {
                outcome(name: "\${WarningSchemaType}", version:"\${WarningSchemaVersion}")
                script( name: "\${WarningScriptName}", version:"\${WarningScriptVersion}")
                query(  name: "\${WarningQueryName}",  version:"\${WarningQueryVersion}")
                property(roleOverride: "TriggerAdmin")
                property(enabledProp: "WarningOn")
                property(reservation: "preserve")
            }
            transition("Timeout", [origin: "Started", target: "Paused"]) {
                outcome(name: "\${TimeoutSchemaType}", version:"\${TimeoutSchemaVersion}")
                script( name: "\${TimeoutScriptName}", version:"\${TimeoutScriptVersion}")
                query(  name: "\${TimeoutQueryName}",  version:"\${TimeoutQueryVersion}")
                property(roleOverride: "TriggerAdmin")
                property(enabledProp: "TimeoutOn")
            }
            transition("Resolve", [origin: "Paused", target: "Started"]) {
                property(roleOverride: "Admin")
                property(reservation: "clear")
            }
            transition("Interrupt", [origin: "Paused", target: "Finished"]) {
                property(roleOverride: "Admin")
                property(reservation: "clear")
            }
            transition("Suspend", [origin: "Started", target: "Suspended"]) {
                outcome(name: "Errors", version: "0")
            }
            transition("Resume", [origin: "Suspended", target: "Started"]) {
                property(roleOverride: "Admin")
                property(reservation: "preserve")
            }
            transition("Proceed", [origin: "Finished", target: "Finished"])
 
            initialState("Waiting")
            finishingState("Finished")
        }

        then:
        builder.sm && builder.sm.isCoherent()
    }


    def 'Build SkipStateMachine using builder methods'() {
        when:
        def builder = StateMachineBuilder.build("testing", "SkipStateMachine", 0) {
            transition("Start", [origin: "Waiting", target: "Started"]) {
                property reservation: "set"
            }
            transition("Done", [origin: "Waiting", target: "Finished"]) {
                property(reservation: "clear")
                outcome(name:"\${SchemaType}", version:"\${SchemaVersion}")
                script( name:"\${ScriptName}", version:"\${ScriptVersion}")
            }
            transition("Skip", [origin: "Waiting", target: "Finished"]) {
                property(reservation: "clear")
                property(enabledProp: "Skippable")
                outcome(name:'Errors', version: "0")
            }
            transition("Complete", [origin: "Started", target: "Finished"]) {
                property(reservation: "clear")
                outcome(name: "\${SchemaType}", version:"\${SchemaVersion}")
                script( name: "\${ScriptName}", version:"\${ScriptVersion}")
            }
            transition("Suspend", [origin: "Started", target: "Suspended"]) {
                property(reservation: "set")
                outcome(name: "Errors", version: "0")
            }
            transition("Resume", [origin: "Suspended", target: "Started"]) {
                property(reservation: "preserve")
                property(roleOverride: "Admin")
            }

            initialState("Waiting")
            finishingState("Finished")
        }

        then:
        builder.sm && builder.sm.isCoherent()
        builder.sm.getTransitions().find { it.name == "Start" }.originState.name == "Waiting"
        builder.sm.getTransitions().find { it.name == "Start" }.targetState.name == "Started"
        builder.sm.getTransitions().find { it.name == "Skip"  }.originState.name == "Waiting"
        builder.sm.getTransitions().find { it.name == "Skip"  }.targetState.name == "Finished"
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
