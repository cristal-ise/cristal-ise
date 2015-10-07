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
import org.cristalise.dsl.test.entity.agent.AgentTestBuilder
import org.cristalise.dsl.test.entity.item.ItemTestBuilder
import org.cristalise.kernel.entity.agent.JobList
import org.cristalise.test.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class JoblistSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemoryServer() }
    def cleanup() { cristalCleanup() }

    def 'Joblist of Agent is automatically updated'() {
        when:
        AgentTestBuilder agentBuilder = AgentTestBuilder.create(name: "dummyAgent") {
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

        //some wait is needed until JobPusher thread finishes
        Thread.sleep(500)
        def jobList = new JobList(agentBuilder.agent, null)

        then:
        jobList
        jobList[0]
        jobList[1]
    }

    def 'StateMachine Transition can override Role specified in Actitiy'() {
        when:
        StateMachineBuilder.create("testing", "RoleOverrideSM", 0) {
            State(id: "0", name: "Waiting")
            State(id: "1", name: "Started")
            State(id: "2", name: "Finished", proceeds: "true")

            Transition(id:"1", name:"Start",    origin:"0", target:"1", reservation:"set")
            Transition(id:"2", name:"Complete", origin:"1", target:"2", reservation:"clear") {
                Outcome(name:"\${SchemaType}", version:"\${SchemaVersion}")
                Script( name:"\${ScriptName}", version:"\${ScriptVersion}")
            }
            Transition(id:"3", name:"Timeout", origin:"0", target:"0", roleOverride: '${RoleOverride}') {
                Outcome(name:"Errors", version: "0")
            }
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

        //some wait is needed until JobPusher thread finishes
        Thread.sleep(500)
        def jobList = new JobList(timeoutAgent.agent, null)

        then:
        jobList
        jobList[0]
    }
}
