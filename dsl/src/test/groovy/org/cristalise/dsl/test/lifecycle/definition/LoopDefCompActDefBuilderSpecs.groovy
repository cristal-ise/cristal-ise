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
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.LoopDef
import org.cristalise.kernel.lifecycle.instance.stateMachine.StateMachine
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.persistency.outcome.Schema
import org.cristalise.kernel.scripting.Script
import org.cristalise.kernel.test.utils.CristalTestSetup

import spock.lang.Specification


/**
 *
 */
class LoopDefCompActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
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

    def 'CompositeActivityDef can start with LoopDef'() {
        when:
        def ea1 = new ActivityDef('EA1', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-StartLoop', version: 0) {
            Layout {
                Loop { Act(ea1) }
            }
        }

        def loopDef = caDef.getChildren().find { it instanceof LoopDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-StartLoop'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4
        caDef.childrenGraphModel.startVertex.class.simpleName == 'JoinDef'

        loopDef
        loopDef.properties.RoutingExpr == 'true'
        loopDef.getOutGraphables().findAll { GraphableVertex v ->
            v.getBuiltInProperty(PAIRING_ID) == loopDef.getBuiltInProperty(PAIRING_ID) }.size() == 1

        loopDef.getInGraphables().collect { it.name } == ['EA1']
        loopDef.getOutGraphables().collect { it.class.simpleName } == ['JoinDef', 'JoinDef']
    }

    def 'CompositeActivityDef can build a sequence including LoopDef'() {
        when:
        def first   = new ActivityDef('first', 0)
        def looping = new ActivityDef('looping', 0)
        def last    = new ActivityDef('last', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-IncludeLoop', version: 0) {
            Layout {
                Act(first)
                Loop {
                    Act(looping)
                }
                Act(last)
            }
        }

        def loopDef = caDef.getChildren().find { it instanceof LoopDef }
        def lastDef = caDef.getChildren().find { it.name == 'last'}

        then:
        caDef.verify()
        caDef.name == 'CADef-IncludeLoop'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 6
        caDef.childrenGraphModel.startVertex.name == "first"
        ((GraphableVertex)(caDef.childrenGraphModel.startVertex)).getOutGraphables().collect { it.class.simpleName } == ['JoinDef']

        loopDef
        loopDef.getOutGraphables().findAll { GraphableVertex v ->
            v.getBuiltInProperty(PAIRING_ID) == loopDef.getBuiltInProperty(PAIRING_ID) }.size() == 1

        loopDef.getOutGraphables().collect { it.class.simpleName } == ['JoinDef', 'JoinDef']
        loopDef.getInGraphables().collect { it.name } == ['looping']

        lastDef
        lastDef.getInGraphables().collect { it.class.simpleName } == ['JoinDef']
        lastDef.getOutGraphables().size() == 0
    }

    def 'LoopDef can define RoutingScript'() {
        when:
        def looping = new ActivityDef('looping', 0)
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-Loop-RoutingScript', version: 0) {
            Layout {
                Loop(javascript: true) {
                    Property(toto: 123)
                    Act(looping)
                }
            }
        }

        def loopDef = caDef.getChildren().find { it instanceof LoopDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-Loop-RoutingScript'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4

        loopDef
        loopDef.properties.RoutingScriptName == 'javascript:true;'
        loopDef.properties.RoutingScriptVersion == null;
        loopDef.properties.toto == 123
    }

    def 'LoopDef can be Infinitive'() {
        when:
        def looping = new ActivityDef('looping', 0)
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-Loop-Infinitive', version: 0) {
            Layout {
                LoopInfinitive() {
                    Property(toto: 123)
                    Act(looping)
                }
            }
        }

        def loopDef = caDef.getChildren().find { it instanceof LoopDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-Loop-Infinitive'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4

        loopDef
        loopDef.properties.RoutingScriptName == 'groovy:true;'
        loopDef.properties.RoutingScriptVersion == null;
        loopDef.properties.toto == 123
    }
}