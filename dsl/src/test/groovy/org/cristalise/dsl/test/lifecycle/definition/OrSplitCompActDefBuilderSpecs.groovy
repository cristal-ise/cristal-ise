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
import org.cristalise.kernel.lookup.ItemPath
import org.cristalise.kernel.scripting.Script
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
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitStart', version: 0) {
            Layout {
                OrSplit(groovy: 'left') {
                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
            }
        }

        def orSplitDef = caDef.getChildren().find { it instanceof OrSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-OrSplitStart'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 4
        caDef.childrenGraphModel.startVertex.class.simpleName == 'OrSplitDef'

        orSplitDef
        joinDef

        orSplitDef.getPairingId() == joinDef.getPairingId()

        orSplitDef.getInGraphables().size() == 0
        orSplitDef.getOutGraphables().collect {it.name} == ['left','right']
        orSplitDef.properties.RoutingScriptName == "groovy:left"
        orSplitDef.properties.RoutingScriptVersion == null;
        orSplitDef.getOutEdges().collect {((GraphableEdge)it).getBuiltInProperty(ALIAS)} == ['left','right']

        joinDef.getInGraphables().collect {it.name} == ['left','right']
        joinDef.getOutGraphables().size() == 0
    }

    def 'OrSplit can define properties'() {
        when:
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
    
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitProps', version: 0) {
            Layout {
                OrSplit(RoutingScriptName: 'CounterScript01', RoutingScriptVersion: 0) {
                    Property(counter: 'activity//./first:/TestData/counter')

                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
            }
        }

        def orSplitDef = caDef.getChildren().find { it instanceof OrSplitDef }

        then:
        caDef.verify()

        orSplitDef.properties.RoutingScriptName == 'CounterScript01'
        orSplitDef.properties.RoutingScriptVersion == 0
        orSplitDef.properties.counter == 'activity//./first:/TestData/counter'
    }

    def 'OrSplit can use Script object as RoutingScript'() {
        when:
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
        def script = new Script('RoutingScript42', 13, new ItemPath(), null)
        
        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitExternalRoutingScript', version: 0) {
            Layout {
                OrSplit(RoutingScript: script) {
                    Property(counter: 'activity//./first:/TestData/counter')

                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
            }
        }

        def orSplitDef = caDef.getChildren().find { it instanceof OrSplitDef }

        then:
        caDef.verify()

        orSplitDef.properties.RoutingScriptName == 'RoutingScript42'
        orSplitDef.properties.RoutingScriptVersion == 13
        orSplitDef.properties.counter == 'activity//./first:/TestData/counter'
    }

    def 'CompositeActivityDef can include OrSplit'() {
        when:
        def first = new ActivityDef('first',  0)
        def left  = new ActivityDef('left',  0)
        def right = new ActivityDef('right', 0)
        def last  = new ActivityDef('last',  0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitIncluded', version: 0) {
            Layout {
                Act(first)
                OrSplit(groovy: 'left') {
                    Block(Alias: 'left')  { Act(left)  }
                    Block(Alias: 'right') { Act(right) }
                }
                Act(last)
            }
        }

        def orSplitDef = caDef.getChildren().find { it instanceof OrSplitDef }
        def joinDef = caDef.getChildren().find { it instanceof JoinDef }

        then:
        caDef.verify()
        caDef.name == 'CADef-OrSplitIncluded'
        caDef.version == 0
        caDef.childrenGraphModel.vertices.length == 6
        caDef.childrenGraphModel.startVertex.class.simpleName == 'ActivitySlotDef'

        orSplitDef
        joinDef

        orSplitDef.pairingId == joinDef.pairingId

        orSplitDef.inGraphables.size() == 1
        joinDef.outGraphables.size() == 1
    }

    def 'OrSplit can contain OrSplits'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitWithOrSplit', version: 0) {
            Layout {
                OrSplit {
                    OrSplit { 
                        Block {Act('Middle1', middle)}
                        Block {Act('Middle2', middle)}
                    }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def outerOrSplit = (OrSplitDef) caDef.childrenGraphModel.startVertex
        def innerOrSplit = (OrSplitDef) caDef.getChildren().find { it instanceof OrSplitDef && it.getID() != outerOrSplit.getID() }

        def outerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == outerOrSplit.pairingId }
        def innerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == innerOrSplit.pairingId }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 7

        innerOrSplit.inGraphables[0].getID() == outerOrSplit.getID()
        outerJoin.inGraphables[0].getID() == innerJoin.getID()
    }

    def 'OrSplit can contain XOrSplits'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitWithXOrSplit', version: 0) {
            Layout {
                OrSplit {
                    XOrSplit {
                        Block {Act('Middle1', middle)}
                        Block {Act('Middle2', middle)}
                    }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def outerOrSplit  = (OrSplitDef)  caDef.childrenGraphModel.startVertex
        def innerXOrSplit = (XOrSplitDef) caDef.getChildren().find { it instanceof XOrSplitDef && it.getID() != outerOrSplit.getID() }

        def outerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == outerOrSplit.pairingId }
        def innerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == innerXOrSplit.pairingId }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 7

        innerXOrSplit.inGraphables[0].getID() == outerOrSplit.getID()
        outerJoin.inGraphables[0].getID() == innerJoin.getID()
    }

    def 'OrSplit can contain AndSplits'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitWithAndSplit', version: 0) {
            Layout {
                OrSplit {
                    AndSplit { 
                        Block {Act('Middle1', middle)}
                        Block {Act('Middle2', middle)}
                    }
                    Block { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        def outerOrSplit = (OrSplitDef) caDef.childrenGraphModel.startVertex
        def innerAndSplit = (AndSplitDef) caDef.getChildren().find { it instanceof AndSplitDef && it.getID() != outerOrSplit.getID() }

        def outerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == outerOrSplit.pairingId }
        def innerJoin = (JoinDef) caDef.getChildren().find { it instanceof JoinDef && it.pairingId == innerAndSplit.pairingId }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 7

        innerAndSplit.inGraphables[0].getID() == outerOrSplit.getID()
        outerJoin.inGraphables[0].getID() == innerJoin.getID()
    }

    def 'OrSplit can contain Loops'() {
        when:
        def middle = new ActivityDef('middle', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitWithLoop', version: 0) {
            Layout {
                OrSplit {
                    Loop { Act('Middle', middle)}
                    LoopInfinitive { CompActDef('ManageItemDesc', 0) }
                }
            }
        }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 10
    }

    def 'OrSplit can be used to skip a Loop'() {
        when:
        def right = new ActivityDef('right', 0)

        caDef = CompActDefBuilder.build(module: 'test', name: 'CADef-OrSplitWithLoopAndEmptyBlock', version: 0) {
            Layout {
                OrSplit(groovy: 'right') {
                    Loop(Alias: 'right') { Act(right) }
                    Block(Alias: 'left')  { /* EMPTY BLOCK*/ }
                }
            }
        }
        def orSplitDef = caDef.getChildren().find { it instanceof OrSplitDef }

        then:
        caDef.verify()
        caDef.childrenGraphModel.vertices.length == 6

        orSplitDef.getOutEdges().collect {((GraphableEdge)it).getBuiltInProperty(ALIAS)} == ['right', 'left']
    }
}
