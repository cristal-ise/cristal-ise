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

import org.cristalise.dsl.entity.RoleBuilder;
import org.cristalise.kernel.common.ObjectNotFoundException
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class RoleCreateSpecs extends Specification implements CristalTestSetup {

    
    def setup()   { inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', 8) }
    def cleanup() { cristalCleanup() }

    def "Parent Role must exists"() {
        when:
        RoleBuilder.create {
            Role(name: 'Clerk/SubClerk', jobList: true)
        }

        then:
        thrown ObjectNotFoundException
    }

    def "Creating Roles and Subroles"() {
        when:
        def roles = RoleBuilder.create {
            Role(name: 'Clerk')
            Role(name: 'Clerk/SubClerk', jobList: true)
        }

        then:
        roles[0].exists()
        roles[0].string == "/role/Clerk"
        roles[0].hasJobList() == false

        roles[1].exists()
        roles[1].string == "/role/Clerk/SubClerk"
        roles[1].hasJobList() == true

        Gateway.lookup.getRolePath("Clerk").hasJobList() == false
        Gateway.lookup.getRolePath("SubClerk").hasJobList() == true
    }

    def "Creating Role with Permissions"() {
        when:
        def roles = RoleBuilder.create {
            Role(name: 'QA') {
                Permission('BatchFactory:Create:*')
                Permission(domain: 'Batch', actions: 'Review', targets: '*')
            }
        }

        then:
        Gateway.getLookup().exists(roles[0])
        def rp = Gateway.lookup.getRolePath("QA")
        rp.hasJobList() == false
        rp.permissions[0] == 'BatchFactory:Create:*'
        rp.permissions[1] == 'Batch:Review:*'
    }
}
