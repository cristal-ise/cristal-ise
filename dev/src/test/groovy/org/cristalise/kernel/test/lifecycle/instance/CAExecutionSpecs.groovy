/**
 * This file is part of the CRISTAL-iSE Development Module.
 * Copyright (c) 2001-2017 The CRISTAL Consortium. All rights reserved.
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
import org.cristalise.kernel.common.InvalidDataException
import org.cristalise.kernel.common.InvalidTransitionException
import org.cristalise.kernel.lifecycle.instance.Workflow
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup
import spock.lang.Specification


class CAExecutionSpecs extends Specification implements CristalTestSetup {

    static WorkflowTestBuilder util

    def setupSpec() {
        inMemoryServer(null, true)

        util = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(util.wf)
    }

    def cleanupSpec() {
        cristalCleanup()
    }

    def 'Execute ElemAct using Done transition'() {
        given: "Workflow containing single ElemAct"
        util.buildAndInitWf { ElemAct('first') }

        when: "requesting ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "ElemAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
    }

    def 'Execute ElemAct using Start/Complete transition'() {
        given: "Workflow containing single ElemAct"
        util.buildAndInitWf { ElemAct('first') }

        when: "requesting ElemAct Start transition"
        util.requestAction( 'first', "Start")

        then: "ElemAct state is Started"
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('first',   [state: "Started", active: true])

        when: "requesting ElemAct Complete transition"
        util.requestAction( 'first', "Complete" )

        then: "ElemAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
    }

    def 'Execute sequence of ElemActs using Done transition'() {
        given: "Workflow containing sequence of two ElemAct"
        util.buildAndInitWf { ElemAct('first'); ElemAct('second') }

        when: "requesting first ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "first ElemAct state is Finished and second is still Waiting"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction('second', "Done")

        then: "first ElemAct state is Finished and second is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when all Activities in sequence are finished'() {
        given: "Workflow containing CompAct containing 2 ElemAct in a sequence"
        util.buildAndInitWf {
            CompAct('ca') {
                ElemAct('first')
                ElemAct('second')
            }
        }

        when: "requesting first ElemAct Done transition"
        util.requestAction('first', "Done")

        then: "ElemAct 'first' and CompAct state is Started"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Waiting",  active: true])

        when: "requesting second ElemAct Done transition"
        util.requestAction('second', "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('first',  [state: "Finished", active: false])
        util.checkActStatus('second', [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when all Activities in AndSplit are finished'() {
        given: "Workflow containing CompAct containing 2 ElemAct in AndSplit"
        util.buildAndInitWf {
            CompAct('ca') {
                AndSplit {
                    Block { ElemAct('left')  }
                    Block { ElemAct('right') }
                }
            }
        }
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('left',   [state: "Waiting",  active: true])
        util.checkActStatus('right',  [state: "Waiting",  active: true])

        when: "requesting left ElemAct Done transition"
        util.requestAction('left', "Done")

        then: "ElemAct 'left' and CompAct state is Started"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Started",  active: true])
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Waiting",  active: true])

        when: "requesting right ElemAct Done transition"
        util.requestAction('right', "Done")

        then: "ElemAct and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when all Activities in AndSplit with Loops are finished'() {
        given: "Workflow containing CompAct containing 2 ElemAct in AndSplit"
        Workflow wf = util.buildAndInitWf {
            CompAct('ca') {
                Property('Abortable': true)
                AndSplit {
                    Block {
                        Loop(RoutingScriptName: 'javascript:\"false\";') {
                            ElemAct('left')
                        }
                    }
                    Block {
                        Loop(RoutingScriptName: 'javascript:\"false\";') {
                            ElemAct('right')
                        }
                   }
                }
            }
            ElemAct('last')
        }
        when: "requesting 'left' ElemAct Done transition"
        util.requestAction('left', "Done")

        then: "ElemAct 'left' should be finished and inactive, ElemAct 'right' should be waiting and active, ElemAct 'last' should be inactive"
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Waiting", active: true])
        util.checkActStatus('last',   [state: "Waiting", active: false])

        when: "requesting 'right' ElemAct Done transition"
        util.requestAction('right', "Done")

        then: "ElemAct 'left' and right should be Finished and inactive, ElemAct 'last' should be Waiting and active"
        util.checkActStatus('left',   [state: "Finished", active: false])
        util.checkActStatus('right',  [state: "Finished", active: false])
        util.checkActStatus('last',   [state: "Waiting", active: true])

        //Print images of workflow to debug easily
        util.saveWorkflowPngImage("workflow/domain", "target/workflowTest.png", true)
        util.saveWorkflowPngImage("workflow/domain/ca", "target/caTest.png", true)
    }

    def 'CompAct is automatically finished when all Activities in Loop are finished'() {
        given: "Workflow containing CompAct containing 1 ElemAct in Loop"
        util.buildAndInitWf {
            CompAct('ca') {
                Loop(RoutingScriptName: 'javascript:\"false\";') { //loop shall finish automatically
                    ElemAct('one')
                }
            }
        }

        when: "requesting one ElemAct Done transition"
        util.requestAction('one', "Done")

        then: "ElemAct 'one' and CompAct state is Finished"
        util.checkActStatus('rootCA', [state: "Started",  active: true])
        util.checkActStatus('one',    [state: "Finished", active: false])
        util.checkActStatus('ca',     [state: "Finished", active: false])
    }

    def 'CompAct is automatically finished when OrSplit triggers empty block'() {
        given:
        util.buildAndInitWf() {
            CompAct('ca') {
                ElemAct("first") // ElemAct is within CompAct
                OrSplit(javascript: "2") {
                    Block { ElemAct("left") }
                    Block { /*EMPTY*/ }
                }
            }
        }

        when:
        util.requestAction("first", "Done")

        then:
        util.checkActStatus('ca', [state: "Finished", active: false])
    }

    def 'CompAct is automatically started and finished when OrSplit triggers empty block'() {
        given:
        util.buildAndInitWf() {
            ElemAct("first") // ElemAct is before CompAct
            CompAct('ca') {
                OrSplit(javascript: "2") {
                    Block { ElemAct("left") }
                    Block { /*EMPTY*/ }
                }
            }
        }

        when:
        util.requestAction("first", "Done")
        //util.saveWorkflowPngImage("workflow/domain/ca", "target/CAStartFinsishOrSplitEmptyBlock.png", true)

        then:
        util.checkActStatus('ca', [state: "Finished", active: false])
    }

    def 'CompAct with infinitive Loop never finishes'() {
        given: "Workflow containing CompAct containing 1 ElemAct in Loop"
        util.buildAndInitWf {
            CompAct('ca') {
                Loop { //by default the DSL creates infinitive Loop
                    ElemAct('one')
                }
            }
        }

        when: "requesting one ElemAct Done transition"
        util.requestAction('one', "Done")

        then: "ElemAct 'one' and CompAct are still active"
        util.checkActStatus('rootCA', [state: "Started", active: true])
        util.checkActStatus('one',    [state: "Waiting", active: true])
        util.checkActStatus('ca',     [state: "Started", active: true])
    }

    def 'Empty Wf cannot be Completed'() {
        given: "empty Workflow"
        util.buildAndInitWf {
            //checks before Wf.init()
            util.checkActStatus('rootCA', [state: "Waiting", active: false])
        }

        when: "requesting Root CompAct Complete transition"
        util.requestAction('rootCA', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'Cannot complete Compact without finishing all ElemActs'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                ElemAct('first')
            }
        }

        when: "requesting CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "InvalidTransitionException is thrown"
        thrown InvalidTransitionException
    }

    def 'CompAct can be finished with active children if Abortable'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                Property('Abortable': true)
                ElemAct('first')
            }
        }
        util.checkActStatus('ca',   [state: "Started", active: true])

        when: "requesting Root CompAct Complete transition"
        util.requestAction('ca', "Complete")

        then: "CompAct is finished, child is inactive but Waiting"
        util.checkActStatus('ca',    [state: "Finished", active: false])
        util.checkActStatus('first', [state: "Waiting",  active: false])
    }

    def 'Abortable CompAct calls Abort transition for its children CompActs'() {
        given: "Workflow containing single CompAct with a single ElemAct"
        util.buildAndInitWf {
            CompAct('ca') {
                Property('Abortable': true)
                ElemAct('first')
                CompAct('ca1') {
                    Property('Abortable': true)
                    ElemAct('first1')
                }
            }
        }
        util.checkActStatus('ca',   [state: "Started", active: true])

        when: "requesting Root CompAct Complete transition"
        util.requestAction('first', "Done")
        util.requestAction('ca', "Complete")

        then: "CompAct is finished, child is inactive but Waiting"
        util.checkActStatus('ca1',    [state: "Aborted", active: false])
        util.checkActStatus('first1', [state: "Waiting",  active: false])
        util.checkActStatus('ca',     [state: "Finished", active: false])
        util.checkActStatus('first',  [state: "Finished",  active: false])
    }
}
