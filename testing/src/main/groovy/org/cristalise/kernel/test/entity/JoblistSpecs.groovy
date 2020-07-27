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
package org.cristalise.kernel.test.entity

import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.dsl.test.builders.AgentTestBuilder
import org.cristalise.dsl.test.builders.ItemTestBuilder
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification
import spock.util.concurrent.PollingConditions


/**
 *
 */
class JoblistSpecs extends Specification implements CristalTestSetup {

    PollingConditions pollingWait = new PollingConditions(timeout: 2, initialDelay: 0.2, factor: 1)

    AgentTestBuilder dummyAgentBuilder
    AgentTestBuilder timeoutAgentBuilder

    def setup() {
        def testProps = new Properties()
        testProps.put("Module.ImportAgent.enableRoleCreation", true)
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8, testProps)
    }
    def cleanup() {
        if(dummyAgentBuilder) dummyAgentBuilder.jobList.deactivate()
        if(timeoutAgentBuilder) timeoutAgentBuilder.jobList.deactivate()
        cristalCleanup()
    }

    def 'The persistent Joblist of Agent is automatically updated'() {
        given: "the workflow of Item is initialised its first Activity is activated"
        dummyAgentBuilder = AgentTestBuilder.create(name: "dummyAgent", password: 'dummy') {
            Roles {
                Role(name: 'toto', jobList: true) { Permission('*') }
            }
        }

        ItemTestBuilder dummyItem = ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1') {
                    Property('Agent Role': "toto")
                }
            }
        }

        //"Agent gets 2 Jobs (Start, Done) for the Activity it was assigned to"
        pollingWait.eventually {
            dummyAgentBuilder.checkJobList(
                    [   [stepName: "EA1", agentRole: "toto", transitionName: "Start"],
                        [stepName: "EA1", agentRole: "toto", transitionName: "Done" ]])
        }

        when: "the Job associated with the Start Transition is executed"
        dummyAgentBuilder.executeJob(dummyItem.itemDomPath.itemPath, "EA1", "Start")

        then: "Agent gets two Jobs (Complete, Suspend) for the Activity it was assigned to"
        pollingWait.eventually {
            dummyAgentBuilder.checkJobList(
                    [   [stepName: "EA1", agentRole: "toto", transitionName: "Suspend" ],
                        [stepName: "EA1", agentRole: "toto", transitionName: "Complete"]])
        }

        when: "the Job associated with the Complete Transition is executed"
        dummyAgentBuilder.executeJob(dummyItem.itemDomPath.itemPath, "EA1", "Complete")

        then: "Agent gets the Proccess Job for the Activity it was assigned to"
        pollingWait.eventually {
            dummyAgentBuilder.checkJobList([[stepName: "EA1", agentRole: "toto", transitionName: "Proceed"]])
        }
    }

    def 'StateMachine Transition can override Role specified in Actitiy'() {
        given:
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
            transition("Timeout2", [origin:"Started", target:"Started"]) {
                property(roleOverride: '${RoleOverride}')
                outcome(name:"Errors", version: "0")
            }
            initialState("Waiting")
            finishingState("Finished")
        }

        AgentTestBuilder dummyAgentBuilder = AgentTestBuilder.create(name: "dummy", password: 'dummy') {
            Roles {
                Role(name: 'toto') { Permission('*') }
            }
        }

        AgentTestBuilder timeoutAgentBuilder = AgentTestBuilder.create(name: "TimeoutManager", password: 'dummy') {
            Roles {
                Role(name: 'Timeout', jobList: true) { Permission('*') }
            }
        }

        ItemTestBuilder dummyItemBuilder = ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1') {
                    Property('Agent Role'    : "toto")
                    Property(RoleOverride    : "Timeout")
                    Property(StateMachineName: "RoleOverrideSM")
                }
            }
        }

        pollingWait.eventually {
            timeoutAgentBuilder.checkJobList([[stepName: "EA1", agentRole: "Timeout", transitionName: "Timeout"]])
        }

        when:
        dummyAgentBuilder.executeJob(dummyItemBuilder.itemDomPath.itemPath, 'EA1', 'Start')

        then:
        pollingWait.eventually {
            assert timeoutAgentBuilder.jobList && timeoutAgentBuilder.jobList.size() == 1
            timeoutAgentBuilder.jobListContains([stepName: "EA1", agentRole: "Timeout", transitionName: "Timeout2"])
        }
    }
}
