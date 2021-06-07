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
package org.cristalise.kernel.test.lifecycle.instance

import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.dsl.test.builders.WorkflowTestBuilder
import org.cristalise.kernel.common.AccessRightsException
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class CustomStateMachineSpecs extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', null, true)
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanupSpec() { cristalCleanup() }

    def 'Specific Transition is enabled/disabled by a Property'() {
        given:
        wfBuilder.eaSM = StateMachineBuilder.create("testing", "SkipStateMachine", 0) {
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

        wfBuilder.buildAndInitWf {
            EA('EA1') {
                Property(StateMachineName: "SkipStateMachine")
                Property(StateMachineVersion: '0')
                Property(Skippable: true)
            }
            EA('EA2') {
                Property(StateMachineName: "SkipStateMachine")
                Property(StateMachineVersion: '0')
                Property(Skippable: false)
            }
        }

        when:
        wfBuilder.requestAction("EA1", "Skip")

        then:
        wfBuilder.checkActStatus('EA1',[state: "Finished", active: false])
        wfBuilder.checkActStatus('EA2',[state: "Waiting",  active: true])

        when:
        wfBuilder.requestAction("EA2", "Skip")

        then:
        thrown AccessRightsException
    }
}
