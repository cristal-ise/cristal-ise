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
package org.cristalise.dsl.test.collection

import org.cristalise.dsl.collection.DependencyBuilder
import org.cristalise.test.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class CollectionBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   { loggerSetup()    }
    def cleanup() { cristalCleanup() }

    def 'Build Dependency'() {
        when:
        def builder = DependencyBuilder.build("depend") {
            Properties {
                Property("toto": true)
            }
            Member(itemPath: "/entity/b9415b57-3a4a-4b31-825a-d307d1280ac0") { 
                Property("version": 0)
            }
        }

        then:
        builder.dependency.properties.size() == 1
        builder.dependency.members.list.size() == 1

        builder.dependency.members.list[0].properties.size() == 2
    }
}
