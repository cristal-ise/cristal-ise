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
package org.cristalise.kernel.test.lifecycle.instance;

//import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.dsl.test.builders.WorkflowTestBuilder;
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.utils.CristalTestSetup;

import spock.lang.Specification


class SplitExecutionSpecs extends Specification implements CristalTestSetup {

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

    def 'OrSplit enables branch(es) using RoutingScript'() {
        given: "Wf = first-OrSplit(script:1)((left)(right))-last"
        util.buildAndInitWf() {
            ElemAct("first")
            OrSplit(javascript: "1,3") {
                Block { ElemAct("left")  }
                Block { ElemAct("middle")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "EA(left) is enabled but EA(right) is disabled"
        util.checkActStatus("left",   [state: "Waiting", active: true])
        util.checkActStatus("middle", [state: "Waiting", active: false])
        util.checkActStatus("right",  [state: "Waiting", active: true])
        util.checkActStatus("last",   [state: "Waiting", active: false])

        when: "requesting EA(left) Done transition"
        util.requestAction("left", "Done")

        then: "EA(left) is Finished but EA(right) is disabled"
        util.checkActStatus("left",   [state: "Finished", active: false])
        util.checkActStatus("middle", [state: "Waiting",  active: false])
        util.checkActStatus("right",  [state: "Waiting",  active: true])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
        
        when: "requesting EA(right) Done transition"
        util.requestAction("right", "Done")

        then: "EA(left) is Finished but EA(right) is disabled"
        util.checkActStatus("left",   [state: "Finished", active: false])
        util.checkActStatus("middle", [state: "Waiting",  active: false])
        util.checkActStatus("right",  [state: "Finished", active: false])
        util.checkActStatus("last",   [state: "Waiting",  active: true])

        when: "requesting EA(last) Done transition"
        util.requestAction("last", "Done")

        then: "EA(last) is Finished and disabled"
        util.checkActStatus("left",   [state: "Finished", active: false])
        util.checkActStatus("middle", [state: "Waiting",  active: false])
        util.checkActStatus("right",  [state: "Finished", active: false])
        util.checkActStatus("last",   [state: "Finished", active: false])
    }


    def 'AndSplit enforces all branches to be executed'() {
        given: "Wf = first-AndSplit((left)(right))-last "
        util.buildAndInitWf() {
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: false])
        util.checkActStatus("right", [state: "Waiting", active: false])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished and EA(left) and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Waiting",  active: true])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: true])

        when: "requesting ElemAct(last) Done transition"
        util.requestAction("last", "Done")

        then: "ElemAct(last) state is Finished"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Finished", active: false])
    }

    def 'XOrSplit enforces one and only one branch to be executed'() {
        given: "Wf = first-AndSplit((left)(right))-last "
        util.buildAndInitWf() {
            ElemAct("first")
            XOrSplit(javascript: '1') {
                Block { ElemAct("left")  }
                Block { ElemAct("middle")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished and EA(left) and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
    }


    def 'first-AndSplit(AndSplit((left2)(right2))(right1))-last'() {
        given: "Workflow contaning AndSplit with two Blocks"
        util.buildAndInitWf {
            ElemAct("first") //This is only needed because of bug initializing Splits
            AndSplit {
                Block {
                    AndSplit('AndSplit1') {
                        Block { ElemAct("left2")  }
                        Block { ElemAct("right2")  }
                    }
                }
                Block { ElemAct("right1") }
            }
            ElemAct("last")
        }
        util.checkActStatus("first",  [state: "Waiting", active: true])
        util.checkActStatus("left2",  [state: "Waiting", active: false])
        util.checkActStatus("right2", [state: "Waiting", active: false])
        util.checkActStatus("right1", [state: "Waiting", active: false])
        util.checkActStatus("last",   [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished"
        util.checkActStatus("first",  [state: "Finished", active: false])
        util.checkActStatus("left2",  [state: "Waiting",  active: true])
        util.checkActStatus("right2", [state: "Waiting",  active: true])
        util.checkActStatus("right1", [state: "Waiting",  active: true])
        util.checkActStatus("last",   [state: "Waiting",  active: false])
    }

    def 'AndSplit followed by OrSplit shall not break split execution rules'() {
        given:
        util.buildAndInitWf() {
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            OrSplit(javascript: 1) {
                Block { ElemAct("left_or")  }
                Block { ElemAct("right_or") }
            }
            ElemAct("last")
        }

        util.checkActStatus("first",    [state: "Waiting", active: true])
        util.checkActStatus("left",     [state: "Waiting", active: false])
        util.checkActStatus("right",    [state: "Waiting", active: false])
        util.checkActStatus("left_or",  [state: "Waiting", active: false])
        util.checkActStatus("right_or", [state: "Waiting", active: false])
        util.checkActStatus("last",     [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished and EA(left) and EA(right) are enabled"
        util.checkActStatus("first",    [state: "Finished", active: false])
        util.checkActStatus("left",     [state: "Waiting",  active: true])
        util.checkActStatus("right",    [state: "Waiting",  active: true])
        util.checkActStatus("left_or",  [state: "Waiting",  active: false])
        util.checkActStatus("right_or", [state: "Waiting",  active: false])
        util.checkActStatus("last",     [state: "Waiting",  active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished"
        util.checkActStatus("first",    [state: "Finished", active: false])
        util.checkActStatus("left",     [state: "Finished", active: false])
        util.checkActStatus("right",    [state: "Waiting",  active: true])
        util.checkActStatus("left_or",  [state: "Waiting",  active: false])
        util.checkActStatus("right_or", [state: "Waiting",  active: false])
        util.checkActStatus("last",     [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished"
        util.checkActStatus("first",    [state: "Finished", active: false])
        util.checkActStatus("left",     [state: "Finished", active: false])
        util.checkActStatus("right",    [state: "Finished", active: false])
        util.checkActStatus("left_or",  [state: "Waiting",  active: true])
        util.checkActStatus("right_or", [state: "Waiting",  active: false])
        util.checkActStatus("last",     [state: "Waiting",  active: false])

        when: "requesting ElemAct(left_or) Done transition"
        util.requestAction("left_or", "Done")

        then: "ElemAct(left_or) state is Finished ) and EA(last) are enabled"
        util.checkActStatus("first",    [state: "Finished", active: false])
        util.checkActStatus("left",     [state: "Finished", active: false])
        util.checkActStatus("right",    [state: "Finished", active: false])
        util.checkActStatus("left_or",  [state: "Finished", active: false])
        util.checkActStatus("right_or", [state: "Waiting",  active: false])
        util.checkActStatus("last",     [state: "Waiting",  active: true])
    }

    def 'AndSplit followed by Loop shall not break split execution rules'() {
        given:
        util.buildAndInitWf() {
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            Loop(RoutingScriptName: 'javascript:\"false\";') { //loop shall finish automatically
                ElemAct('one')
            }
            ElemAct("last")
        }

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: false])
        util.checkActStatus("right", [state: "Waiting", active: false])
        util.checkActStatus("one",   [state: "Waiting", active: false])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished and EA(left) and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Waiting",  active: true])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("one",   [state: "Waiting",  active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("one",   [state: "Waiting",  active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("one",   [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(one) Done transition"
        util.requestAction("one", "Done")

        then: "ElemAct(one) state is Finished and EA(last) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("one",   [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: true])
    }

    def 'Loop containing an AndSplit shall not break split execution rules'() {
        given:
        util.buildAndInitWf() {
            ElemAct("first")
            Loop(RoutingScriptName: 'javascript:\"false\";') { //loop shall finish automatically
                AndSplit {
                    Block { ElemAct("left")  }
                    Block { ElemAct("right") }
                }
            }
            ElemAct("last")
        }

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: false])
        util.checkActStatus("right", [state: "Waiting", active: false])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished and EA(left) and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Waiting",  active: true])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished  and EA(last) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: true])
    }

    def 'AndSplit containing Loops shall not break split execution rules'() {
        given: 
        util.buildAndInitWf() {
            ElemAct("first")
            AndSplit {
                Block {
                    Loop(RoutingScriptName: 'javascript:\"false\";') {
                        //loop shall finish automatically
                        ElemAct("left")
                    }
                }
                Block {
                    Loop(RoutingScriptName: 'javascript:\"false\";') {
                        //loop shall finish automatically
                        ElemAct("right")
                    }
                }
            }
            ElemAct("last")
        }

        util.checkActStatus("first", [state: "Waiting", active: true])
        util.checkActStatus("left",  [state: "Waiting", active: false])
        util.checkActStatus("right", [state: "Waiting", active: false])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "ElemAct(first) state is Finished and EA(left) and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Waiting",  active: true])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished and EA(right) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished  and EA(last) are enabled"
        util.checkActStatus("first", [state: "Finished", active: false])
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: true])
    }
}
