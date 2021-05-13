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
package org.cristalise.dsl.test.lifecycle.definition

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.PAIRING_ID

import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder;
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class CompActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
    def setup()   {}
    def cleanup() {}

    def 'CompositeActivityDef can be built without any ElementaryActivityDefs'() {
        when:
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {}

        then:
        caDef.name == 'CADef'
        caDef.version == 0

        caDef.properties.getAbstract().size() == 0
    }

    def 'CompositeActivityDef can build a sequence of ElementaryActivityDefs'() {
        when:
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {
            ElemActDef('EA1', 0) {}
            ElemActDef('EA2', 0)
        }

        then:
        caDef.name == 'CADef'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 2
        caDef.childrenGraphModel.startVertex.name == "EA1"
    }

    def 'CompositeActivityDef can build a sequence including LoopDef'() {
        when:
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {
            ElemActDef('EA1', 0) {}
            ElemActDef('EA2', 0)
            LoopDef {
                ElemActDef('EA3', 0)
            }
        }
        def loopDef = caDef.getChildren().find { it instanceof LoopDef }

        then:
        caDef.name == 'CADef'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 6
        caDef.childrenGraphModel.startVertex.name == "EA1"
        caDef.childrenGraphModel.vertices.find { it.name == "EA3" }

        loopDef
        loopDef.getOutGraphables().size() == 2
        loopDef.getOutGraphables().findAll {
            it.getBuiltInProperty(PAIRING_ID) == loopDef.getBuiltInProperty(PAIRING_ID)}.size() == 1
        loopDef.getInGraphables().size() == 1
        loopDef.getOutEdges()
    }
}
