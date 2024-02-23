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
class AndSlitCompActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
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

    def 'CompositeActivityDef can define AndSplit with AndSplits, Loops, OrSplit and XOrSplts'() {
        when:
        def left   = new ActivityDef('left',  0)
        def middle = new ActivityDef('middle', 0)
        def right  = new ActivityDef('right', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-AndSplitWithSplits', version: 0) {
            Layout {
                AndSplit {
                    LoopInfinitive { Act('Left', left)  }
                    AndSplit { 
                        Block {Act('Middle0', middle)}
                        Block {Act('Middle1', middle)}
                    }
                    OrSplit { 
                        Block {Act('Middle2', middle)}
                        Block {Act('Middle3', middle)}
                    }
                    XOrSplit { 
                        Block {Act('Middle4', middle)}
                        Block {Act('Middle5', middle)}
                    }
                    Loop { Act('Right', right) }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def andSplitDef = (AndSplitDef) caDef.childrenGraphModel.startVertex

        then:
        caDef.verify()
        caDef.name == 'CADef-AndSplitWithSplits'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 23
        caDef.childrenGraphModel.startVertex.class.simpleName == 'AndSplitDef'

        andSplitDef.getInGraphables().size() == 0
        andSplitDef.getOutGraphables().collect { it.class.simpleName } == ['JoinDef', 'AndSplitDef', 'OrSplitDef', 'XOrSplitDef', 'JoinDef', 'ActivitySlotDef']
    }
}
