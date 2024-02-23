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
package org.cristalise.dsl.test.entity

import org.cristalise.dsl.entity.AgentBuilder;
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class AgentBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   { inMemorySetup()  }
    def cleanup() { cristalCleanup() }

    def 'Agent must have at least one Role defined'() {
        when:
        AgentBuilder.build(name: "dummy", password: "dummy") {}

        then:
        thrown InvalidDataException
    }

    def 'Agent with Role a role has name/type property added'() {
        when:
        def newAgent = AgentBuilder.build(name: "dummy", password: "dummy") {
            Roles {
                Role(name: 'User')
            }
        }

        then:
        newAgent.properties.size() == 2
        newAgent.properties[0].name == "Name"
        newAgent.properties[0].value == "dummy"
        newAgent.properties[1].name == "Type"
        newAgent.properties[1].value == "Agent"

        newAgent.roles.size() == 1
        newAgent.roles[0].name == 'User'
    }
}
