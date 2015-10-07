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
}
