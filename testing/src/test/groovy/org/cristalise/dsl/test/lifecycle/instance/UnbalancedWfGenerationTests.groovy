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


class UnbalancedWfGenerationTests implements CristalTestSetup {

    WorkflowTestBuilder wfBuilder

    @Before
    public void setup() {
        inMemorySetup()
        wfBuilder = new WorkflowTestBuilder()
    }

    @After
    public void cleanup() {
        cristalCleanup()
    }


    @Test
    public void 'Generate sequence of first-second-third-last'() {
        wfBuilder.build {
            connect ElemAct: 'first' to ElemAct: 'second' to ElemAct: 'third' to ElemAct: 'last'
            setFirst('first')
        }

        assert wfBuilder.verify()

        wfBuilder.checkSequence('first', 'second', 'third', 'last')
    }

    @Test
    public void 'Generate complex unbalanced workflow - check issue 4'() {
        wfBuilder.build {
            connect ElemAct: 'first' to OrSplit: 'DateSplit'
            connect OrSplit: 'DateSplit' to Join:    'JoinTop'
            connect OrSplit: 'DateSplit' to ElemAct: 'EA1'
            connect OrSplit: 'DateSplit' to ElemAct: 'EA2'

            connect ElemAct: 'EA1' to Join: 'DateJoin'
            connect ElemAct: 'EA2' to Join: 'DateJoin'
            connect Join: 'DateJoin' to ElemAct: 'EA3' to 'Join': 'Join1' to 'LoopSplit': 'CounterLoop'

            connect Join: 'JoinTop' to ElemAct: 'counter' to OrSplit: 'CounterSplit'
            connect OrSplit: 'CounterSplit' to 'Join': 'Join1' 
            connect OrSplit: 'CounterSplit' to 'Join': 'Join2'

            connect 'LoopSplit': 'CounterLoop' to Join: 'JoinTop'
            connect 'LoopSplit': 'CounterLoop' to Join: 'Join2'

            connect Join: 'Join2' to ElemAct: 'last'
            
            setFirst('first')
        }

        assert wfBuilder.verify()

        //wfBuilder.possiblePath('first','DateSplit','JoinTop','counter', 'CounterSplit', 'Join1','CounterLoop', 'Join2', 'last')
        
        wfBuilder.initialise()
    }
}
