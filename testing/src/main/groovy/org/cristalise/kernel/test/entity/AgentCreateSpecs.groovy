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

import org.cristalise.dsl.test.builders.AgentTestBuilder;
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Ignore
import spock.lang.Specification


/**
 *
 */
class AgentCreateSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8) }
    def cleanup() { cristalCleanup() }

    def 'Agent with Role is created'() {
        when:
        AgentTestBuilder agentBuilder = AgentTestBuilder.create(name: "dummy", password: 'dummy') {
            Roles {
                Role(name: 'toto', jobList: true)
            }
        }

        then:
        agentBuilder.newAgentPath.exists()
        agentBuilder.newAgentPath.agentName == 'dummy'
        agentBuilder.newAgent.roles
        agentBuilder.newAgent.roles.size() == 1
        agentBuilder.newAgent.roles[0].name == 'toto'
        agentBuilder.newAgent.roles[0].jobList == true
    }

    @Ignore("Unimplemented")
    def 'Agent can be updated'() {
        when:
        AgentTestBuilder.create(name: "dummy") {
            Roles {
                Role(name: 'toto')
            }
        }

        AgentTestBuilder agentBuilder = AgentTestBuilder.create(name: "dummy") {
            Roles {
                Role(name: 'tototo')
            }
        }

        then:
        agentBuilder.agent.exists()
        agentBuilder.agent.agentName == 'dummy'
        agentBuilder.agent.roles
        agentBuilder.agent.roles.size() == 2
        agentBuilder.agent.roles[0].name == 'toto'
        agentBuilder.agent.roles[0].hasJobList() == true
        agentBuilder.agent.roles[1].name == 'tototo'
        agentBuilder.agent.roles[1].hasJobList() == false
    }
}
