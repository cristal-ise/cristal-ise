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

import org.cristalise.test.CristalTestSetup
import org.junit.After
import org.junit.Before
import org.junit.Test


class LoopGenerationTests implements CristalTestSetup {

    WorkflowTestBuilder util

    @Before
    public void setup() {
        inMemorySetup()
        util = new WorkflowTestBuilder()
    }

    @After
    public void cleanup() {
        //println Gateway.getMarshaller().marshall(util.wf)
        cristalCleanup()
    }

    @Test
    public void 'Loop()'() {
        util.build {
            Loop {}
        }
        util.checkActPath('LoopSplit',      'workflow/domain/LoopSplit')
        util.checkActPath('LoopJoin_first', 'workflow/domain/LoopJoin_first')
        util.checkActPath('LoopJoin_last',  'workflow/domain/LoopJoin_last')
        
        util.checkJoin ('LoopJoin_first', ['LoopSplit'])
        util.checkSplit('LoopSplit',      ['LoopJoin_first', 'LoopJoin_last'])
        util.checkJoin ('LoopJoin_last',  ['LoopSplit'])

        util.checkSequence('LoopJoin_first', 'LoopSplit')
    }

    @Test
    public void 'Loop(inner)'() {
        util.build {
            Loop {
                ElemAct("inner")
            }
        }
        util.checkActPath('inner',     'workflow/domain/inner')
        util.checkActPath('LoopSplit', 'workflow/domain/LoopSplit')

        util.checkJoin ('LoopJoin_first', ['LoopSplit'])
        util.checkSplit('LoopSplit',      ['LoopJoin_first', 'LoopJoin_last'])
        util.checkJoin ('LoopJoin_last',  ['LoopSplit'])

        util.checkSequence('LoopJoin_first', 'inner', 'LoopSplit')
    }

    @Test
    public void 'first-Loop(innerA-innerB)-last'() {
        util.build {
            ElemAct("first")
            Loop {
                ElemAct("innerA")
                ElemAct("innerB")
            }
            ElemAct("last")
        }
        util.checkActPath('innerA',         'workflow/domain/innerA')
        util.checkActPath('innerB',         'workflow/domain/innerB')
        util.checkActPath('LoopSplit',      'workflow/domain/LoopSplit')

        util.checkJoin ('LoopJoin_first', ['first', 'LoopSplit'])
        util.checkSplit('LoopSplit',      ['LoopJoin_first', 'LoopJoin_last'])
        util.checkJoin ('LoopJoin_last',  ['LoopSplit'])

        util.checkSequence('LoopJoin_first', 'innerA', 'innerB', 'LoopSplit')
        util.checkSequence('LoopJoin_last', 'last')
    }

    @Test
    public void 'first-Loop(innerA-Loop(innerB))-last'() {
        util.build {
            ElemAct("first")
            Loop {
                ElemAct("innerA")
                Loop {
                    ElemAct("innerB")
                }
            }
            ElemAct("last")
        }
        util.checkActPath('innerA',     'workflow/domain/innerA')
        util.checkActPath('innerB',     'workflow/domain/innerB')
        util.checkActPath('LoopSplit',  'workflow/domain/LoopSplit')
        util.checkActPath('LoopSplit1', 'workflow/domain/LoopSplit1')
        
        util.checkJoin ('LoopJoin_first', ['first', 'LoopSplit'])
        util.checkSplit('LoopSplit',      ['LoopJoin_first', 'LoopJoin_last'])
        util.checkJoin ('LoopJoin_last',  ['LoopSplit'])

        util.checkJoin ('LoopJoin1_first', ['innerA', 'LoopSplit1'])
        util.checkSplit('LoopSplit1',      ['LoopJoin1_first', 'LoopJoin1_last'])
        util.checkJoin ('LoopJoin1_last',  ['LoopSplit1'])

        util.checkSequence('LoopJoin_first', 'innerA')
        util.checkSequence('LoopJoin1_first', 'innerB', 'LoopSplit1')
        util.checkSequence('LoopJoin_last', 'last')
    }
}
