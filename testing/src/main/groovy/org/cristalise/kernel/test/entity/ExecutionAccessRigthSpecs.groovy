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

import static org.unitils.reflectionassert.ReflectionAssert.*;

import org.cristalise.dsl.entity.RoleBuilder;
import org.cristalise.dsl.lifecycle.stateMachine.StateMachineBuilder
import org.cristalise.dsl.test.builders.AgentTestBuilder;
import org.cristalise.dsl.test.builders.ItemTestBuilder
import org.cristalise.kernel.entity.agent.Job
import org.cristalise.kernel.entity.proxy.AgentProxy
import org.cristalise.kernel.entity.proxy.ItemProxy;
import org.cristalise.kernel.process.Gateway;
import org.cristalise.kernel.test.utils.CristalTestSetup;
import org.unitils.reflectionassert.ReflectionComparatorMode

import spock.lang.Specification
import spock.unitils.UnitilsSupport
import spock.util.concurrent.PollingConditions


/**
 *
 */
@UnitilsSupport
class ExecutionAccessRigthSpecs extends Specification implements CristalTestSetup {

    def setup() {
        def testProps = new Properties()
        testProps.put("Module.ImportAgent.enableRoleCreation", true)
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8, testProps)
    }
    def cleanup() { cristalCleanup() }

    def 'Job is only given to the Agent with the proper Role'() {
        when: ""
        AgentTestBuilder  oper1 = AgentTestBuilder.create(name: "oper1", password: 'dummy') {
            Roles {
                Role(name: 'oper') {
                    Permission('*')
                }
            }
        }

        AgentTestBuilder  clerk1 = AgentTestBuilder.create(name: "clerk1", password: 'dummy') {
            Roles {
                Role(name: 'clerk') {
                    Permission('*')
                }
            }
        }

        ItemTestBuilder dummyItem = ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1') {
                    Property('Agent Role': "oper")
                }
            }
        }

        then: "Agent 'oper1' gets 2 Jobs but the Agent 'clerk1' get no Job"
        oper1.getJobs(dummyItem.itemDomPath.itemPath).size() == 2
        clerk1.getJobs(dummyItem.itemDomPath.itemPath).size() == 0
    }

    def "Activity property 'Agent Role' can contain a list of Roles"() {
        when: ""
        AgentTestBuilder  oper1 = AgentTestBuilder.create(name: "oper1", password: 'dummy') {
            Roles {
                Role(name: 'oper') { Permission('*') }
            }
        }

        AgentTestBuilder  clerk1 = AgentTestBuilder.create(name: "clerk1", password: 'dummy') {
            Roles {
                Role(name: 'clerk') { Permission('*') }
            }
        }

        ItemTestBuilder dummyItem = ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1') {
                    Property('Agent Role': "oper, clerk")
                }
            }
        }

        then: "Agents 'oper1' and 'clerk1' gets the same set of Jobs"
        oper1.getJobs(dummyItem.itemDomPath.itemPath).size() == clerk1.getJobs(dummyItem.itemDomPath.itemPath).size()

        //Deep comparision of Job does not work, becuase many fields should be ignored
        //assertLenientEquals(oper1.getJobs(dummyItem.itemDomPath.itemPath), clerk1.getJobs(dummyItem.itemDomPath.itemPath))
    }

    def "Role hierachy boss/minion declares that boss1 could execute all Jobs of minion1"() {
        when: "Agent 'boss' has a Role of 'boss' and Agent 'minion1' has a Role of 'minion'"
        AgentTestBuilder  boss1 = AgentTestBuilder.create(name: "boss1", password: 'dummy') {
            Roles {
                Role(name: 'boss') { Permission('*') }
            }
        }

        AgentTestBuilder  minion1 = AgentTestBuilder.create(name: "minion1", password: 'dummy') {
            Roles {
                Role(name: 'boss/minion') { Permission('*') }
            }
        }

        and: "ElemAct is assogned to Role 'minion'"
        ItemTestBuilder dummyItem = ItemTestBuilder.create(name: "dummyItem", folder: "testing") {
            Property(Type: 'test')
            Workflow {
                EA('EA1') {
                    Property('Agent Role': "minion")
                }
            }
        }

        then: "Agents 'boss1' and 'minion1' gets the same set of Jobs"
        boss1.getJobs(dummyItem.itemDomPath.itemPath).size() == minion1.getJobs(dummyItem.itemDomPath.itemPath).size()
    }
}
