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

import org.cristalise.dsl.csv.TabularGroovyParserBuilder
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder
import org.cristalise.dsl.lifecycle.definition.TabularActivityDefBuilder
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.ActivitySlotDef
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.lifecycle.WfVertexDef
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class TabularAndSplitDefBuilderSpecs extends Specification implements CristalTestSetup {
    CompositeActivityDef caDef

    def setup()   {}
    def cleanup() {
        if (caDef) {
            DefaultGraphLayoutGenerator.layoutGraph(caDef.childrenGraphModel)
            CompActDefBuilder.generateWorkflowSVG('target', caDef)
        }
    }

    def xlsxFile = "src/test/data/TabularActivityBuilderAndSplitDef.xlsx"

    def 'CompositeActivityDef starting with AndSplit'() {
        when:
        def parser = TabularGroovyParserBuilder.build(new File(xlsxFile), 'StartWithAndSplit', 2)
        def tadb = new TabularActivityDefBuilder(new CompositeActivityDef('TabularBuilder_StartWithAndSplit', 0))
        caDef = tadb.build(parser)
        def litOfActDefs = caDef.getRefChildActDef()
        def startVertex = caDef.childrenGraphModel.startVertex
        def checker = new CompActDefChecker(caDef)
        def andSplitDef = caDef.getChildren().find { it instanceof AndSplitDef }

        then:
        litOfActDefs.size() == 1
        litOfActDefs[0] instanceof ActivityDef
        litOfActDefs[0].name == 'TestItem_AndSplit'

        caDef.childrenGraphModel.vertices.length == 4
        startVertex && startVertex instanceof AndSplitDef

        AndSplitDef
        checker.checkSequence(AndSplitDef, 'Left', JoinDef)
        checker.checkSequence(AndSplitDef, 'Right', JoinDef)

        caDef.verify()
    }
}
