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
package org.cristalise.dsl.test.lifecycle.instance;

import static org.junit.Assert.*

import org.cristalise.dsl.lifecycle.instance.SplitDelegate
import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.lifecycle.instance.WfVertex.Types
import org.cristalise.kernel.test.utils.CristalTestSetup;
import org.junit.After
import org.junit.Before
import org.junit.Test


class SplitGenerationTests  implements CristalTestSetup {

    static WorkflowTestBuilder util

    @Before
    public void setupSpec() {
        inMemorySetup()
        util = new WorkflowTestBuilder()
    }

    @After
    public void cleanupSpec() {
        //println Gateway.getMarshaller().marshall(util.wf)
        cristalCleanup()
    }

    def createAndCheck_Split(Types type) {
        util.build {
            "$type" {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
        }
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('left',  'workflow/domain/left')

        def splitName = SplitDelegate.getNamePrefix(type)
        def joinName = splitName.replace('Split', 'Join')
        
        util.checkSplit(splitName, ['left','right'])
        util.checkJoin (joinName,  ['left','right'])
    }

    def createAndCheck_Act_Split_Act(Types type) {
        util.build {
            ElemAct("first")
            "$type" {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }
        util.checkActPath('first', 'workflow/domain/first')
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('left',  'workflow/domain/left')
        util.checkActPath('last',  'workflow/domain/last')

        def splitName = SplitDelegate.getNamePrefix(type)
        def joinName = splitName.replace('Split', 'Join')

        util.checkSequence('first', splitName)
        util.checkSequence(joinName, 'last')

        util.checkSplit(splitName, ['left','right'])
        util.checkJoin (joinName,  ['left','right'])
    }

    @Test
    public void 'Split((left)(right))'() {
        createAndCheck_Split( Types.AndSplit )
        createAndCheck_Split( Types.OrSplit )
        createAndCheck_Split( Types.XOrSplit )
    }

    @Test
    public void 'first-Split((left)(right))-last'() {
        createAndCheck_Act_Split_Act( Types.AndSplit )
        createAndCheck_Act_Split_Act( Types.OrSplit )
        createAndCheck_Act_Split_Act( Types.XOrSplit )
    }

    @Test
    public void 'first-Block(And((inner)))-last'() {
        util.build {
            EA("first")
            Block {
                AndSplit {
                    B{ EA("left") }
                    B{ CA("right") {} }
                }
            }
            EA("last")
        }
        util.checkActPath('first', 'workflow/domain/first')
        util.checkActPath('left',  'workflow/domain/left')
        util.checkActPath('right', 'workflow/domain/right')
        util.checkActPath('last',  'workflow/domain/last')

        util.checkSplit('AndSplit', ['left','right'])
        util.checkJoin ('AndJoin',  ['left','right'])

        util.checkSequence('first','AndSplit')
        util.checkSequence('AndJoin','last')
    }

    @Test
    public void 'AndSplit(AndSplit((left1)(right1))(right))'() {
        util.build {
            AndSplit() {
                Block {
                    AndSplit {
                        Block { ElemAct("left1") }
                        Block { ElemAct("right1") }
                   }
                }
                Block { ElemAct("right") }
            }
        }

        util.checkActPath('right',  'workflow/domain/right')
        util.checkActPath('left1',  'workflow/domain/left1')
        util.checkActPath('right1', 'workflow/domain/right1')

        util.checkSplit('AndSplit', ['AndSplit1', 'right'])
        util.checkJoin ('AndJoin',  ['AndJoin1',  'right'])

        util.checkSplit('AndSplit1', ['left1','right1'])
        util.checkJoin ('AndJoin1',  ['left1','right1'])
    }

    @Test
    public void 'AndSplit((left)AndSplit((left1)(right1)))'() {
        util.build {
            AndSplit {
                Block { ElemAct("left") }
                Block {
                    AndSplit {
                        Block { ElemAct("left1") }
                        Block { ElemAct("right1") }
                    }
                }
            }
        }

        util.checkActPath('left',   'workflow/domain/left')
        util.checkActPath('left1',  'workflow/domain/left1')
        util.checkActPath('right1', 'workflow/domain/right1')

        util.checkSplit('AndSplit', ['AndSplit1', 'left'])
        util.checkJoin ('AndJoin',  ['AndJoin1',  'left'])

        util.checkSplit('AndSplit1', ['left1','right1'])
        util.checkJoin ('AndJoin1',  ['left1','right1'])
    }
}
