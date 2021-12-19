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

import static org.cristalise.kernel.graph.model.BuiltInEdgeProperties.ALIAS
import org.cristalise.dsl.lifecycle.definition.CompActDefBuilder;
import org.cristalise.kernel.graph.layout.DefaultGraphLayoutGenerator
import org.cristalise.kernel.graph.model.GraphableEdge
import org.cristalise.kernel.lifecycle.ActivityDef
import org.cristalise.kernel.lifecycle.AndSplitDef
import org.cristalise.kernel.lifecycle.CompositeActivityDef
import org.cristalise.kernel.lifecycle.JoinDef
import org.cristalise.kernel.lifecycle.OrSplitDef
import org.cristalise.kernel.lifecycle.XOrSplitDef
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification


/**
 *
 */
class XOrSplitCompActDefBuilderSpecs extends Specification implements CristalTestSetup {
    
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

    def 'CompositeActivityDef can start with XOrSplit'() {
        when:
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitStart', version: 0) {
            Layout {
                XOrSplit(groovy: 'left') {
                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
            }
        }

        def xorSplitDef = caDef.getChildren().find { it instanceof XOrSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-XOrSplitStart'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4
        caDef.childrenGraphModel.startVertex.class.simpleName == 'XOrSplitDef'

        xorSplitDef
        joinDef

        xorSplitDef.getPairingId() == joinDef.getPairingId()

        xorSplitDef.getInGraphables().size() == 0
        xorSplitDef.getOutGraphables().collect {it.name} == ['left','right']
        xorSplitDef.properties.RoutingScriptName == "groovy:left"
        xorSplitDef.properties.RoutingScriptVersion == null;
        xorSplitDef.getOutEdges().collect {((GraphableEdge)it).getBuiltInProperty(ALIAS)} == ['left','right']

        joinDef.getInGraphables().collect {it.name} == ['left','right']
        joinDef.getOutGraphables().size() == 0
    }

    def 'XOrSplit can define properties'() {
        when:
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitProps', version: 0) {
            Layout {
                XOrSplit(RoutingScriptName: 'CounterScript01', RoutingScriptVersion: 0) {
                    Property(counter: 'activity//./first:/TestData/counter')

                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
            }
        }

        def xorSplitDef = caDef.getChildren().find { it instanceof XOrSplitDef }

        then:
        caDef.verify()

        xorSplitDef.properties.RoutingScriptName == 'CounterScript01'
        xorSplitDef.properties.RoutingScriptVersion == 0
        xorSplitDef.properties.counter == 'activity//./first:/TestData/counter'
    }

    def 'CompositeActivityDef can include XOrSplit'() {
        when:
        def first = new ActivityDef('first',  0)
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
        def last  = new ActivityDef('last',  0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitIncluded', version: 0) {
            Layout {
                Act(first)
                XOrSplit(groovy: 'left') {
                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
                Act(last)
            }
        }

        def xorSplitDef = caDef.getChildren().find { it instanceof XOrSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-XOrSplitIncluded'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 6
        caDef.childrenGraphModel.startVertex.class.simpleName == 'ActivitySlotDef'

        xorSplitDef
        joinDef

        xorSplitDef.pairingId == joinDef.pairingId

        xorSplitDef.inGraphables.size() == 1
        joinDef.outGraphables.size() == 1
    }

    def 'XOrSplit can contain XOrSplits'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitWithXOrSplit', version: 0) {
            Layout {
                XOrSplit {
                    XOrSplit { 
                        Block {Act('Middle1', middle)}
                        Block {Act('Middle2', middle)}
                    }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def outerXOrSplit = (XOrSplitDef) caDef.childrenGraphModel.startVertex
        def innerXOrSplit = (XOrSplitDef) caDef.getChildren().find { it instanceof XOrSplitDef && it.getID() != outerXOrSplit.getID() }

        def outerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == outerXOrSplit.pairingId }
        def innerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == innerXOrSplit.pairingId }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 7

        innerXOrSplit.inGraphables[0].getID() == outerXOrSplit.getID()
        outerJoin.inGraphables[0].getID() == innerJoin.getID()
    }

    def 'XOrSplit can contain OrSplits'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitWithOrSplit', version: 0) {
            Layout {
                XOrSplit {
                    OrSplit { 
                        Block {Act('Middle1', middle)}
                        Block {Act('Middle2', middle)}
                    }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def outerXOrSplit = (XOrSplitDef) caDef.childrenGraphModel.startVertex
        def innerOrSplit = (OrSplitDef) caDef.getChildren().find { it instanceof OrSplitDef && it.getID() != outerXOrSplit.getID() }

        def outerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == outerXOrSplit.pairingId }
        def innerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == innerOrSplit.pairingId }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 7

        innerOrSplit.inGraphables[0].getID() == outerXOrSplit.getID()
        outerJoin.inGraphables[0].getID() == innerJoin.getID()
    }

    def 'XOrSplit can contain AndSplits'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitWithAndSplit', version: 0) {
            Layout {
                XOrSplit {
                    AndSplit { 
                        Block {Act('Middle1', middle)}
                        Block {Act('Middle2', middle)}
                    }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def outerXOrSplit = (XOrSplitDef) caDef.childrenGraphModel.startVertex
        def innerAndSplit = (AndSplitDef) caDef.getChildren().find { it instanceof AndSplitDef && it.getID() != outerXOrSplit.getID() }

        def outerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == outerXOrSplit.pairingId }
        def innerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == innerAndSplit.pairingId }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 7

        innerAndSplit.inGraphables[0].getID() == outerXOrSplit.getID()
        outerJoin.inGraphables[0].getID() == innerJoin.getID()
    }

    def 'XOrSplit can contain Loops'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-XOrSplitWithLoop', version: 0) {
            Layout {
                XOrSplit {
                    Loop { Act('Middle', middle)}
                    LoopInfinitive { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 10
    }
}
