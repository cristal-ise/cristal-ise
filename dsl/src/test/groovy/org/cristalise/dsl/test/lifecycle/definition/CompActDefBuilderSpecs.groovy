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
                Act('ea1', ea1) {
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
        caDef.childrenGraphModel.startVertex.name == "ea1"
        def ea1Slot = (GraphableVertex) caDef.search('ea1')
        ea1Slot.getBuiltInProperty(NAME) == 'ea1'
        ea1Slot.getBuiltInProperty(AGENT_ROLE) == 'UserCode'
        ea1Slot.getProperties().get('stringVal') == '1'
        ea1Slot.getProperties().get('booleanVal') == true
        ea1Slot.getProperties().get('intVal') == 0
    }
}
