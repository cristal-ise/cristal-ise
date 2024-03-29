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

import org.cristalise.dsl.entity.RoleBuilder;
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class RoleBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   {}
    def cleanup() {}

    def "Default value for jobList is false"() {
        when:
        def roles = RoleBuilder.build {
            Role(name: 'User')
        }

        then:
        roles[0].name == "User"
        roles[0].jobList == false
    }

    def "Build a list of Roles with namespace"() {
        when:
        def roles = RoleBuilder.build('ttt') {
            Role(name: 'User')
            Role(name: 'User/SubUser', jobList: true)
        }

        then:
        roles[0].namespace == "ttt"
        roles[0].name == "User"
        roles[0].jobList == false
        
        roles[1].namespace == "ttt"
        roles[1].name == "User/SubUser"
        roles[1].jobList == true
    }

    def "Build Role with Permissions"() {
        when:
        def roles = RoleBuilder.build {
            Role(name: 'QA') {
                Permission('BatchFactory:Create:*')
                Permission(domain: 'Batch', actions: 'Review', targets: '*')
            }
            Role(name: 'User')
        }

        then:
        roles[0].name == "QA"
        roles[0].jobList == false
        roles[0].permissions[0] == 'BatchFactory:Create:*'
        roles[0].permissions[1] == 'Batch:Review:*'
        roles[1].name == "User"
        roles[1].permissions.size() == 0
    }

}
