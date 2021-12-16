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

import static org.cristalise.kernel.graph.model.BuiltInVertexProperties.*
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder;
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.OrSplitDef
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification


/**
 *
 */
class OrSplitCompActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
    CompositeActivityDef caDef
    
    def setup() {
        inMemorySetup()
    }

    def cleanup() {
        if (caDef) {
            DefaultGraphLayoutGenerator.layoutGraph(caDef.childrenGraphModel)
            CompActDefBuilder.generateWorkflowSVG('target', caDef)
        }
        cristalCleanup()
        caDef = null
    }


    def 'CompositeActivityDef can start with OrSplit'() {
        when:
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-StartOrSplit', version: 0) {
            Layout {
                OrSplit {
                    Block { Act(left)  }
                    Block { Act(right) }
                }
            }
        }

        def orSplitDef = caDef.getChildren().find { it instanceof OrSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-StartOrSplit'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4
        caDef.childrenGraphModel.startVertex.class.simpleName == 'OrSplitDef'

        orSplitDef
        joinDef

        orSplitDef.getBuiltInProperty(PAIRING_ID)
        joinDef.getBuiltInProperty(PAIRING_ID)
        orSplitDef.getBuiltInProperty(PAIRING_ID) == joinDef.getBuiltInProperty(PAIRING_ID)

        orSplitDef.getInGraphables().size() == 0
        orSplitDef.getOutGraphables().collect {it.name} == ['left','right']
        joinDef.getInGraphables().collect {it.name} == ['left','right']
        joinDef.getOutGraphables().size() == 0
    }
}
