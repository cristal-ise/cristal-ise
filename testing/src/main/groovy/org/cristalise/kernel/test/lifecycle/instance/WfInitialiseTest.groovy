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
import org.junit.After
import org.junit.Before
import org.junit.Test

import spock.lang.Specification

/**
 *
 */
class WfInitialiseTest extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder wfBuilder

    def setupSpec() {
        inMemoryServer('src/main/bin/inMemoryServer.conf', 'src/main/bin/inMemory.clc', null, true)
        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanupSpec() {
        cristalCleanup()
    }

    def 'Starting AndSplit-in-AndSplit initialise all Activities inside'() {
        given:
        wfBuilder.build {
            AndSplit {
                B{ AndSplit {
                    Block { ElemAct("left"); ElemAct("left1") }
                    Block { ElemAct("right") }
                } }
            }
        }
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])

        when:
        wfBuilder.initialise()

        then:
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: true])
    }

    def 'Starting OrSplit-in-OrSplit initialise all Activities inside'() {
        given:
        wfBuilder.build {
            OrSplit(javascript: '1') {
                B { OrSplit(javascript: '1,2') {
                    Block { ElemAct("left"); ElemAct("left1") }
                    Block { ElemAct("right") }
                } }
            }
        }
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])

        when:
        wfBuilder.initialise()

        then:
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: true])
    }

    def 'Starting XOrSplit-in-XOrSplit initialise one branch inside'() {
        given:
        wfBuilder.build {
            XOrSplit(javascript: '1') {
                B { XOrSplit(javascript: '1') {
                    Block { ElemAct("left"); ElemAct("left1") }
                    Block { ElemAct("right") }
                } }
            }
        }
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])

        when:
        wfBuilder.initialise()

        then:
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
    }

    def 'Starting Loop-in-Loop initialise the first Activity inside'() {
        given:
        wfBuilder.build {
            Loop {
                Loop {
                    EA("first"); EA("second")
                }
            }
        }
        wfBuilder.checkActStatus("first",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("second", [state: "Waiting", active: false])

        when:
        wfBuilder.initialise()

        then:
        wfBuilder.checkActStatus("first",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("second", [state: "Waiting", active: false])
    }
}
