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
package org.cristalise.kernel.test.entity.agent

import org.cristalise.dsl.entity.role.RoleBuilder
import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.dsl.test.builders.AgentTestBuilder;
import org.cristalise.dsl.test.builders.ItemTestBuilder;
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification
import spock.util.concurrent.PollingConditions


/**
 *
 */
class JoblistSpecs extends Specification implements CristalTestSetup {

    PollingConditions pollingWait = new PollingConditions(timeout: 1, initialDelay: 0.2, factor: 1)

    def setup()   { inMemoryServer() }
    def cleanup() { cristalCleanup() }

    def 'Joblist of Agent is automatically updated'() {
        when: "the workflow of Item is initialised its first Activity is activated"
        AgentTestBuilder dummyAgent = AgentTestBuilder.create(name: "dummyAgent") {
            Roles {
                Role(name: 'toto', jobList: true)
            }
        }

        ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Workflow {
                EA('EA1') {
                    Property('Agent Role': "toto")
                }
            }
        }

        then: "Agent gets Job(s) for the activity it was assigned to"
        pollingWait.eventually { dummyAgent.jobList }

        dummyAgent.checkJobList([[stepName: "EA1", agentRole: "toto", transitionName: "Start"],
                                 [stepName: "EA1", agentRole: "toto", transitionName: "Done" ]])
    }

    def 'StateMachine Transition can override Role specified in Actitiy'() {
        when:
        StateMachineBuilder.create("testing", "RoleOverrideSM", 0) {
            transition("Start", [origin:"Waiting", target:"Started"]) {
                 property(reservation: "set")
            }
            transition("Complete", [origin:"Started", target:"Finished"]) {
                property(reservation:"clear")

                outcome(name:'${SchemaType}', version:'${SchemaVersion}')
                script (name:'${ScriptName}', version:'${ScriptVersion}')
            }
            transition("Timeout", [origin:"Waiting", target:"Waiting"]) {
                property(roleOverride: '${RoleOverride}')
                outcome(name:"Errors", version: "0")
            }
            initialState("Waiting")
            finishingState("Finished")
        }

        RoleBuilder.create { Role(name: 'toto') }

        AgentTestBuilder timeoutAgent = AgentTestBuilder.create(name: "TimeoutManager") {
            Roles {
                Role(name: 'Timeout', jobList: true)
            }
        }

        ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Workflow {
                EA('EA1') {
                    Property('Agent Role'    : "toto")
                    Property(RoleOverride    : "Timeout")
                    Property(StateMachineName: "RoleOverrideSM")
                }
            }
        }

        then:
        pollingWait.eventually { timeoutAgent.jobList }

        timeoutAgent.checkJobList([[stepName: "EA1", agentRole: "Timeout", transitionName: "Timeout"]])
    }
}
