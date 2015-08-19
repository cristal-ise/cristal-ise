package org.cristalise.kernel.test.lifecycle.instance;

//import static org.cristalise.kernel.lifecycle.instance.WfVertex.Types.*

import org.cristalise.dsl.test.lifecycle.instance.WorkflowTestBuilder
import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway

import spock.lang.Specification


class SplitExecutionSpecs extends Specification {

    WorkflowTestBuilder util

    def setup() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()

        util = new WorkflowTestBuilder()
    }

    def cleanup() {
        println Gateway.getMarshaller().marshall(util.wf)
        Gateway.close()
    }

    def 'OrSplit enables branch(es) using RoutingScript'() {
        given: "Wf = first-OrSplit(script:1)((enabled)(disabled))-last"
        util.buildAndInitWf() {
            ElemAct("first")
            OrSplit(javascript: 1) {
                Block { ElemAct("enabled")  }
                Block { ElemAct("disabled") }
            }
            ElemAct("last")
        }

        when: "requesting ElemAct(first) Done transition"
        util.requestAction("first", "Done")

        then: "EA(enaled) is enabled but EA(disabled) is disabled"
        util.checkActStatus("enabled",  [state: "Waiting", active: true])
        util.checkActStatus("disabled", [state: "Waiting", active: false])
        util.checkActStatus("last",     [state: "Waiting", active: false])

        when: "requesting EA(enabled) Done transition"
        util.requestAction("enabled", "Done")

        then: "EA(enabled) is Finished but EA(disabled) is disabled"
        util.checkActStatus("enabled",  [state: "Finished", active: false])
        util.checkActStatus("disabled", [state: "Waiting",  active: false])
        util.checkActStatus("last",     [state: "Waiting",  active: true])
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
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Waiting",  active: true])

        when: "requesting ElemAct(last) Done transition"
        util.requestAction("last", "Done")

        then: "ElemAct(last) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: false])
        util.checkActStatus("right", [state: "Finished", active: false])
        util.checkActStatus("last",  [state: "Finished", active: false])
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
}
