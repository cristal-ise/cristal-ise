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

import org.cristalise.dsl.test.lifecycle.instance.WorkflowTestBuilder
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway

import spock.lang.Specification

/**
 *
 */
class WfInitialiseSpecs extends Specification {

    WorkflowTestBuilder wfBuilder

    def setup() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()

        wfBuilder = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(wfBuilder.wf)
        Gateway.close()
    }

    def 'Empty Workflow is NOT fully initialised'() {
        given: "empty Workflow"
        wfBuilder.build {}
        wfBuilder.checkActStatus("rootCA", [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "The root CA (domain) is still 'Waiting' but became active"
        wfBuilder.checkActStatus("rootCA", [state: "Waiting", active: true])
    }

    def 'Workflow with single Act CAN be initialised'() {
        given: "the Workflow with single ElemAct"
        wfBuilder.build { 
            ElemAct('first')
        }
        wfBuilder.checkActStatus("rootCA", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("first",  [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "The root CA (domain) is 'Started' and ElemAct(frist) became active"
        wfBuilder.checkActStatus("rootCA", [state: "Started", active: true])
        wfBuilder.checkActStatus("first",  [state: "Waiting", active: true])
    }

    def 'Workflow with empty CompAct CAN be initialised'() {
        given: "the Workflow with empty CompAct"
        wfBuilder.build {
            CompAct('ca') {}
        }
        wfBuilder.checkActStatus("rootCA", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("ca",     [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "The root CA (domain) is 'Started' and CompAct(ca) became active"
        wfBuilder.checkActStatus("rootCA", [state: "Started", active: true])
        wfBuilder.checkActStatus("ca",     [state: "Waiting", active: true])
    }

    def 'Workflow with nested empty CompActs CAN be initialised'() {
        given: "the Workflow with empty CompAct"
        wfBuilder.build {
            CompAct {
                CompAct('ca1') {}
            }
        }
        wfBuilder.checkActStatus("rootCA", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("CA",     [state: "Waiting", active: false])
        wfBuilder.checkActStatus("ca1",    [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "The root CA (domain) is 'Started' and ElemAct(frist) became active"
        wfBuilder.checkActStatus("rootCA", [state: "Started", active: true])
        wfBuilder.checkActStatus("CA",     [state: "Started", active: true])
        wfBuilder.checkActStatus("ca1",    [state: "Waiting", active: true])
    }

    def 'Starting AndSplit initialise all Activities inside'() {
        given: "Workflow with AndSplit((left-left1)(right))"
        wfBuilder.build {
            AndSplit {
                Block { ElemAct("left"); ElemAct("left1") }
                Block { ElemAct("right") }
            }
        }
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "all Activities are Waiting and active"
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: true])
    }

    def 'Starting OrSplit initialise all Activities inside'() {
        given: "Workflow with OrSplit((left-left1)(right)"
        wfBuilder.build {
            OrSplit(javascript: '1,2') {
                Block { ElemAct("left"); ElemAct("left1") }
                Block { ElemAct("right") }
            }
        }
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "all Activities are Waiting and active"
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: true])
    }

    def 'Starting XOrSplit initialise all Activities inside'() {
        given: "Workflow with XOrSplit((leftl-left1)(right)"
        wfBuilder.build {
            XOrSplit {
                Block { ElemAct("left"); ElemAct("left1") }
                Block { ElemAct("right") }
            }
        }
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "all Activities are Waiting and active"
        wfBuilder.checkActStatus("left",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("left1", [state: "Waiting", active: false])
        wfBuilder.checkActStatus("right", [state: "Waiting", active: true])
    }

    def 'Starting Loop initialise the first Activity inside'() {
        given: "Workflow with Loop(first-second)"
        wfBuilder.build {
            Loop {
                B{ EA("first"); EA("second") }
            }
        }
        wfBuilder.checkActStatus("first",  [state: "Waiting", active: false])
        wfBuilder.checkActStatus("second", [state: "Waiting", active: false])

        when: "the Workflow is initialised"
        wfBuilder.initialise()

        then: "all Activities are Waiting and active"
        wfBuilder.checkActStatus("first",  [state: "Waiting", active: true])
        wfBuilder.checkActStatus("second", [state: "Waiting", active: false])
    }
}
