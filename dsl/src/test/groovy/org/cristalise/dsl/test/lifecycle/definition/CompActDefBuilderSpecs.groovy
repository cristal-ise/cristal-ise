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
import org.cristalise.kernel.graph.model.GraphableVertex
import org.cristalise.kernel.lifecycle.ActivityDef
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
    
    def setup()   {}
    def cleanup() {}

    def 'Empty CompositeActivityDef can be built '() {
        when:
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {}

        then:
        caDef.name == 'CADef'
        caDef.version == 0

        caDef.properties.getAbstract().size() == 0
    }

    def 'CompositeActivityDef can define its Properties, StateMachine, Schema and Script'() {
        when:
        def schema = new Schema('schema', 0, '<xs:schema/>')
        def script = new Script('script', 0, new ItemPath(), null)
        def sm = new StateMachine('sm', 0)

        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {
            Property(concreteProp: 'dummy')
            AbstractProperty(abstractProp: 'dummy')

            Schema(schema)
            Script(script)
            StateMachine(sm)
        }

        then:
        caDef.name == 'CADef'
        caDef.version == 0
        caDef.getScript()
        caDef.getSchema()
        caDef.getStateMachine()

        caDef.properties.size() == 12 //there are 10 default properties
        caDef.properties.getAbstract() == ['abstractProp']
        caDef.properties.concreteProp == 'dummy'
        caDef.properties.abstractProp == 'dummy'
    }


    def 'CompositeActivityDef can build a sequence of ElementaryActivityDefs'() {
        when:
        def ea1 = new ActivityDef('EA1', 0)
        def ea2 = new ActivityDef('EA2', 0)
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {
            Layout {
                Act(ea1)
                Act('EA2_1', ea2)
                Act('EA2_2', ea2)
            }
        }

        then:
        caDef.name == 'CADef'
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
    
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {
            Layout {
                Act(ea1) {
                    Property(stringVal: '1')
                    Property(intVal: 0, booleanVal: true)
                }
            }
        }

        then:
        caDef.name == 'CADef'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 1
        caDef.childrenGraphModel.startVertex.name == "EA1"
        //def ea1Slot = (GraphableVertex) caDef.childrenGraphModel.vertices.find { it.name == "EA1" }
        def ea1Slot = (GraphableVertex) caDef.search('EA1')
        ea1Slot.getProperties().get('stringVal') == '1'
        ea1Slot.getProperties().get('booleanVal') == true
        ea1Slot.getProperties().get('intVal') == 0
    }

    def 'CompositeActivityDef can build a sequence including LoopDef'() {
        when:
        def ea1 = new ActivityDef('EA1', 0)
        def ea2 = new ActivityDef('EA2', 0)
        def ea3 = new ActivityDef('EA3', 0)
    
        def caDef = CompActDefBuilder.build(module: 'test', name: 'CADef', version: 0) {
            Layout {
                Act(ea1)
                Loop {
                    Act(ea2)
                }
                Act(ea3)
            }
        }

        def loopDef = caDef.getChildren().find { it instanceof LoopDef }

        then:
        caDef.name == 'CADef'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 6
        caDef.childrenGraphModel.startVertex.name == "EA1"

        loopDef
        loopDef.getOutGraphables().size() == 2
        loopDef.getOutGraphables().findAll {
            it.getBuiltInProperty(PAIRING_ID) == loopDef.getBuiltInProperty(PAIRING_ID) }.size() == 1
        loopDef.getOutEdges().size() == 2

        loopDef.getInGraphables().size() == 1
        loopDef.getInEdges().size() == 1
    }
}
