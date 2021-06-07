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
package org.cristalise.kernel.test.lifecycle.instance

import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


/**
 *
 */
class WorkflowVerifySpecs extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        inMemorySetup('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc')
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanupSpec() {
        cristalCleanup()
    }

    def 'Invalid Workflow - Single LoopSplit'() {
        when: "Workflow created with single LoopSplit"
        wfBuilder.build {
            create LoopSplit: 'DummySplit'
        }

        then: "Workflow verify fails with error 'bad number of pointing back nexts'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'bad number of pointing back nexts'
    }

    def 'Invalid Workflow - Single AndSplit'() {
        when: "Workflow created with single AndSplit"
        wfBuilder.build {
            create AndSplit: 'DummySplit'
        }

        then: "Workflow verify fails with error 'not enough next'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'not enough next'
    }

    def 'Invalid Workflow - Single OrSplit'() {
        when: "Workflow created with single OrSplit"
        wfBuilder.build {
            create OrSplit: 'DummySplit'
        }

        then: "Workflow verify fails with error 'not enough next'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'not enough next'
    }

    def 'Invalid Workflow - Single XOrSplit'() {
        when: "Workflow created with single XOrSplit"
        wfBuilder.build {
            create XOrSplit: 'DummySplit'
        }

        then: "Workflow verify fails with error 'not enough next'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'not enough next'
    }

    def 'Invalid Workflow - Single Join'() {
        when: "Workflow created with single Join"
        wfBuilder.build {
            create Join: 'DummyJoin'
        }

        then: "Workflow verify fails with error 'not enough previous'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummyJoin'].errors == 'not enough previous'
    }

    def 'Invalid Workflow - Sequence of two EA with AndSplit ending'() {
        when: "Workflow containind seqence of ElemActs connected to AndSplit"
        wfBuilder.build {
            connect ElemAct: 'first'  to ElemAct:  'second'
            connect ElemAct: 'second' to AndSplit: 'DummySplit'
            setFirst('first')
        }

        then: "Workflow verify fails with error 'not enough next'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'not enough next'
    }

    def 'Invalid Workflow - Sequence of two EA with AndSplit in the middle'() {
        when: "Workflow containind seqence of ElemActs connected to AndSplit"
        wfBuilder.build {
            connect ElemAct:  'first'      to AndSplit: 'DummySplit' 
            connect AndSplit: 'DummySplit' to ElemAct:  'second'
            setFirst('first')
        }

        then: "Workflow verify fails with error 'not enough next'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'not enough next'
    }

    def 'Invalid Workflow - Sequence of two EA with LoopSplit in the middle'() {
        when: "Workflow containind seqence of ElemActs connected to LoopSplit"
        wfBuilder.build {
            connect ElemAct:   'first'      to LoopSplit: 'DummySplit'
            connect LoopSplit: 'DummySplit' to ElemAct:   'second'
            setFirst('first')
        }

        then: "Workflow verify fails with error 'bad number of pointing back nexts'"
        wfBuilder.verify() == false
        wfBuilder.vertexCache['DummySplit'].errors == 'bad number of pointing back nexts'
    }

    def 'Sequence of two EA with Join in the middle'() {
        when: "Workflow containing seqence of ElemActs connected to Join"
        wfBuilder.build {
            connect ElemAct: 'first'     to Join:     'DummyJoin'
            connect Join:    'DummyJoin' to ElemAct: 'second'
            setFirst('first')
        }

        then: "Workflow is valid"
        wfBuilder.verify()
    }
}
