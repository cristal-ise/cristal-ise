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
class CompActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
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

    def 'Empty CompositeActivityDef can be built '() {
        when:
        //caDef variable redefined locally because generateWorkflowSVG() will fail for such caDef
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-Empty', version: 0) {}

        then:
        caDef.name == 'CADef-Empty'
        caDef.version == 0

        caDef.properties.getAbstract().size() == 0
        caDef.childrenGraphModel.vertices.length == 0
    }

    def 'CompositeActivityDef can define its Properties, StateMachine, Schema, Script, ElemActDef and CompActDef'() {
        when:
        def schema = new Schema('schema', 0, '<xs:schema/>')
        def script = new Script('script', 0, new ItemPath(), null)
        def sm = new StateMachine('sm', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-Dummy', version: 0) {
            Property(concreteProp: 'dummy')
            AbstractProperty(abstractProp: 'dummy')

            Schema(schema)
            Script(script)
            StateMachine(sm)

            ElemActDef('EditDefinition', 0)
            CompActDef('ManageItemDesc', 0)
        }

        then:
        caDef.name == 'CADef-Dummy'
        caDef.version == 0
        caDef.getScript()
        caDef.getSchema()
        caDef.getStateMachine()

        caDef.properties.size() == 12 //there are 10 default properties
        caDef.properties.getAbstract() == ['abstractProp']
        caDef.properties.concreteProp == 'dummy'
        caDef.properties.abstractProp == 'dummy'
    }

    def 'CompositeActivityDef can build single EA'() {
        when:
        def ea = new ActivityDef('EA', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'WfDef-EA', version: 0) {
            Layout {
                Act(ea)
            }
        }

        DefaultGraphLayoutGenerator.layoutGraph(caDef.childrenGraphModel)
        CompActDefBuilder.generateWorkflowSVG('target', caDef)

        then:
        caDef.verify()
        caDef.name == 'WfDef-EA'
        caDef.childrenGraphModel.startVertex.name == 'EA'
        caDef.childrenGraphModel.vertices.length == 1
    }

    def 'CompositeActivityDef can build single CA'() {
        when:
        def ca = new CompositeActivityDef('CA', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'WfDef-CA', version: 0) {
            Layout {
                Act(ca)
            }
        }

        DefaultGraphLayoutGenerator.layoutGraph(caDef.childrenGraphModel)
        CompActDefBuilder.generateWorkflowSVG('target', caDef)

        then:
        caDef.name == 'WfDef-CA'
        caDef.childrenGraphModel.startVertex.name == 'CA'
        caDef.childrenGraphModel.vertices.length == 1
    }

    def 'CompositeActivityDef can build a sequence of ElementaryActivityDefs'() {
        when:
        def ea1 = new ActivityDef('EA1', 0)
        def ea2 = new ActivityDef('EA2', 0)
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-Sequence', version: 0) {
            Layout {
                Act(ea1)
                Act('EA2_1', ea2)
                Act('EA2_2', ea2)
            }
        }

        then:
        caDef.verify()
        caDef.name == 'CADef-Sequence'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 3
        caDef.childrenGraphModel.startVertex.name == 'EA1'
        caDef.search('EA1')
        caDef.search('EA2_1')
        caDef.search('EA2_2')
        caDef.search('EA2') == null
    }

    def 'CompositeActivityDef can define ActSlotDef properties'() {
        when:
        def ea1 = new ActivityDef('EA1', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-AEProps', version: 0) {
            Layout {
                Act(ea1) {
                    Property(AGENT_ROLE, 'UserCode')
                    Property(stringVal: '1')
                    Property(intVal: 0, booleanVal: true)
                }
            }
        }

        then:
        caDef.verify()
        caDef.name == 'CADef-AEProps'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 1
        caDef.childrenGraphModel.startVertex.name == "EA1"
        //def ea1Slot = (GraphableVertex) caDef.childrenGraphModel.vertices.find { it.name == "EA1" }
        def ea1Slot = (GraphableVertex) caDef.search('EA1')
        ea1Slot.getBuiltInProperty(AGENT_ROLE) == 'UserCode'
        ea1Slot.getProperties().get('stringVal') == '1'
        ea1Slot.getProperties().get('booleanVal') == true
        ea1Slot.getProperties().get('intVal') == 0
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
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-Loop-RoutingScript', version: 0) {
            Layout {
                Loop(javascript: true) {
                    Property(toto: 123)
                }
            }
        }

        def loopDef = caDef.getChildren().find { it instanceof LoopDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-Loop-RoutingScript'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 3

        loopDef
        loopDef.properties.RoutingScriptName == 'javascript:"true";'
        loopDef.properties.toto == 123
    }

    def 'CompositeActivityDef can start and finish with AndSplit'() {
        when:
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-StartAndSplit', version: 0) {
            Layout {
                AndSplit {
                    Block { Act(left)  }
                    Block { Act(right) }
                }
            }
        }

        def andSplitDef = caDef.getChildren().find { it instanceof AndSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-StartAndSplit'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4
        caDef.childrenGraphModel.startVertex.class.simpleName == 'AndSplitDef'

        andSplitDef
        joinDef

        andSplitDef.getBuiltInProperty(PAIRING_ID)
        joinDef.getBuiltInProperty(PAIRING_ID)
        andSplitDef.getBuiltInProperty(PAIRING_ID) == joinDef.getBuiltInProperty(PAIRING_ID)

        andSplitDef.getInGraphables().size() == 0
        andSplitDef.getOutGraphables().collect {it.name} == ['left','right']
        joinDef.getInGraphables().collect {it.name} == ['left','right']
        joinDef.getOutGraphables().size() == 0
    }

    def 'CompositeActivityDef can build a sequence including AndSplit'() {
        when:
        def first = new ActivityDef('first', 0)
        def left1 = new ActivityDef('left1', 0)
        def left2 = new ActivityDef('left2', 0)
        def right = new ActivityDef('right', 0)
        def last  = new ActivityDef('last',  0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-IncludeAndSplit', version: 0) {
            Layout {
                Act(first)
                AndSplit {
                    Block {
                        Act(left1)
                        Act(left2)
                    }
                    Block {
                        Act(right)
                    }
                }
                Act(last)
            }
        }

        def andSplitDef = caDef.getChildren().find { it instanceof AndSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-IncludeAndSplit'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 7
        caDef.childrenGraphModel.startVertex.name == 'first'

        andSplitDef
        joinDef

        andSplitDef.getBuiltInProperty(PAIRING_ID)
        joinDef.getBuiltInProperty(PAIRING_ID)
        andSplitDef.getBuiltInProperty(PAIRING_ID) == joinDef.getBuiltInProperty(PAIRING_ID)

        andSplitDef.getInGraphables().collect {it.name} == ['first']
        andSplitDef.getOutGraphables().collect {it.name} == ['left1','right']
        joinDef.getInGraphables().collect {it.name} == ['left2','right']
        joinDef.getOutGraphables().collect {it.name} == ['last']
    }
}
