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

import static org.apache.commons.lang3.StringUtils.*
import static org.cristalise.kernel.collection.Collection.Cardinality.*
import static org.cristalise.kernel.collection.Collection.Type.*
import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*

import org.cristalise.dsl.collection.DependencyBuilder
import org.cristalise.kernel.property.PropertyDescriptionList
import org.cristalise.kernel.test.utils.CristalTestSetup;
import spock.lang.Specification


/**
 *
 */
class CollectionBuilderSpecs extends Specification implements CristalTestSetup {

    def setup()   {}
    def cleanup() {}

    def 'Build Dependency adding member with UUID'() {
        when:
        def builder = DependencyBuilder.build("depend") {
            Properties {
                Property("toto": true)
                Property((DEPENDENCY_CARDINALITY): ManyToOne)
                Property((DEPENDENCY_TYPE): Bidirectional)
                Property((DEPENDENCY_TO): 'ClubMember')
            }
            Member(itemPath: "b9415b57-3a4a-4b31-825a-d307d1280ac0") { 
                Property("version": 0)
            }
        }

        then:
        builder.dependency.properties.size() == 4
        builder.dependency.properties['toto'] == true
        builder.dependency.properties[DEPENDENCY_CARDINALITY.toString()] == ManyToOne.toString()
        builder.dependency.properties[DEPENDENCY_TYPE.toString()] == Bidirectional.toString()
        builder.dependency.properties[DEPENDENCY_TO.toString()] == 'ClubMember'
        builder.dependency.members.list.size() == 1
        builder.dependency.members.list[0].childUUID
        builder.dependency.members.list[0].childUUID == 'b9415b57-3a4a-4b31-825a-d307d1280ac0'

        builder.dependency.members.list[0].properties.size() == 5
        builder.dependency.members.list[0].properties['version'] == 0
        builder.dependency.members.list[0].properties['toto'] == true
    }

    def 'Build Dependency adding member with PropertyDescription'() {
        when:
        def pdl = new PropertyDescriptionList('TestPDL', 0)
        def builder = DependencyBuilder.build('testns', 'depend') {
            Properties {
                Property("toto": true)
            }
            Member(pdl) { 
                Property("version": 0)
            }
        }

        then:
        builder.dependency.properties.size() == 1
        builder.dependency.members.list.size() == 1
        builder.dependency.members.list[0].itemPath
        builder.dependency.members.list[0].itemPath.path[0] == '/desc/PropertyDesc/testns/TestPDL'

        builder.dependency.members.list[0].properties.size() == 2
        builder.dependency.members.list[0].properties['version'] == 0
        builder.dependency.members.list[0].properties['toto'] == true
    }
}
