package org.cristalise.kernel.test.unit.workflow;

import static org.junit.Assert.*

import org.cristalise.kernel.process.AbstractMain
import org.cristalise.kernel.process.Gateway
import org.cristalise.kernel.test.lifecycle.WfBuilder

import spock.lang.Specification


class SplitExecutionSpecs extends Specification {

    WfBuilder util

    def setup() {
        String[] args = ['-logLevel', '8', '-config', 'src/test/conf/testServer.conf', '-connect', 'src/test/conf/testInMemory.clc']
        Gateway.init(AbstractMain.readC2KArgs(args))
        Gateway.connect()

        util = new WfBuilder()
    }

    def cleanup() {
        Gateway.close()
    }

    def 'first-AndSplit((left)(right))-last'() {
        given: "Workflow contaning AndSplit with two Blocks"
        util.buildWf(false) { 
            ElemAct("first")
            AndSplit {
                Block { ElemAct("left")  }
                Block { ElemAct("right") }
            }
            ElemAct("last")
        }
        util.checkActStatus("left",  [state: "Waiting", active: true])
        util.checkActStatus("right", [state: "Waiting", active: true])
        util.checkActStatus("last",  [state: "Waiting", active: false])

        when: "requesting ElemAct(left) Done transition"
        util.requestAction("left", "Done")

        then: "ElemAct(left) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: true])
        util.checkActStatus("right", [state: "Waiting",  active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: false])

        when: "requesting ElemAct(right) Done transition"
        util.requestAction("right", "Done")

        then: "ElemAct(right) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: true])
        util.checkActStatus("right", [state: "Finished", active: true])
        util.checkActStatus("last",  [state: "Waiting",  active: true])

        when: "requesting ElemAct(last) Done transition"
        util.requestAction("last", "Done")

        then: "ElemAct(last) state is Finished"
        util.checkActStatus("left",  [state: "Finished", active: true])
        util.checkActStatus("right", [state: "Finished", active: true])
        util.checkActStatus("last",  [state: "Finished", active: true])
    }
}
